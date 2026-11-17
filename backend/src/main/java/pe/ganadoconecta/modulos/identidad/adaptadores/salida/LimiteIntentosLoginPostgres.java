package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.DemasiadosIntentosAutenticacionException;
import pe.ganadoconecta.modulos.identidad.aplicacion.LimiteIntentosLogin;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;

/** Contadores persistentes por huella de correo y origen; nunca almacena esos valores. */
public final class LimiteIntentosLoginPostgres implements LimiteIntentosLogin {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final int maxFallos;
    private final Duration ventana;

    public LimiteIntentosLoginPostgres(JdbcTemplate jdbc, TransactionTemplate transacciones,
            int maxFallos, Duration ventana) {
        if (maxFallos < 1 || maxFallos > 10) {
            throw new IllegalArgumentException("maxFallos debe estar entre 1 y 10");
        }
        if (ventana == null || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.maxFallos = maxFallos;
        this.ventana = ventana;
    }

    @Override
    public void verificarPermitido(String claveCorreo, String origen, Instant ahora) {
        try {
            if (bloqueado("CORREO", claveCorreo, ahora) || bloqueado("ORIGEN", origen, ahora)) {
                throw new DemasiadosIntentosAutenticacionException();
            }
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    @Override
    public void registrarFallo(String claveCorreo, String origen, Instant ahora) {
        try {
            boolean bloquear = Boolean.TRUE.equals(transacciones.execute(status -> {
                boolean correoBloqueado = incrementar("CORREO", claveCorreo, ahora);
                boolean origenBloqueado = incrementar("ORIGEN", origen, ahora);
                return correoBloqueado || origenBloqueado;
            }));
            if (bloquear) {
                throw new DemasiadosIntentosAutenticacionException();
            }
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    private boolean bloqueado(String dimension, String clave, Instant ahora) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM identidad.intento_login
                    WHERE dimension = ? AND clave_sha256 = ?
                      AND bloqueado_hasta > ?
                )
                """, Boolean.class, dimension, huella(clave), fecha(ahora)));
    }

    private boolean incrementar(String dimension, String clave, Instant ahora) {
        OffsetDateTime ahoraUtc = fecha(ahora);
        OffsetDateTime limiteVentana = fecha(ahora.minus(ventana));
        OffsetDateTime bloqueadoHasta = jdbc.queryForObject("""
                INSERT INTO identidad.intento_login AS actual
                    (dimension, clave_sha256, ventana_inicia_en, fallos,
                     bloqueado_hasta, actualizado_en)
                VALUES (?, ?, ?, 1, CASE WHEN 1 >= ? THEN ? ELSE NULL END, ?)
                ON CONFLICT (dimension, clave_sha256) DO UPDATE SET
                    ventana_inicia_en = CASE
                        WHEN actual.ventana_inicia_en <= ? THEN EXCLUDED.ventana_inicia_en
                        ELSE actual.ventana_inicia_en END,
                    fallos = CASE
                        WHEN actual.ventana_inicia_en <= ? THEN 1
                        ELSE LEAST(actual.fallos + 1, ?) END,
                    bloqueado_hasta = CASE
                        WHEN actual.ventana_inicia_en <= ?
                            THEN CASE WHEN 1 >= ? THEN ? ELSE NULL END
                        WHEN actual.fallos + 1 >= ?
                            THEN actual.ventana_inicia_en + (? * INTERVAL '1 millisecond')
                        ELSE actual.bloqueado_hasta END,
                    actualizado_en = EXCLUDED.actualizado_en
                RETURNING bloqueado_hasta
                """, OffsetDateTime.class,
                dimension, huella(clave), ahoraUtc, maxFallos,
                fecha(ahora.plus(ventana)), ahoraUtc,
                limiteVentana, limiteVentana, maxFallos,
                limiteVentana, maxFallos, fecha(ahora.plus(ventana)),
                maxFallos, ventana.toMillis());
        return bloqueadoHasta != null && bloqueadoHasta.toInstant().isAfter(ahora);
    }

    private byte[] huella(String clave) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(
                    Objects.requireNonNullElse(clave, "").getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static OffsetDateTime fecha(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
