package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioSesiones;

/** Adaptador transaccional para sesión y familia de renovación. */
public final class RepositorioSesionesPostgres implements RepositorioSesiones {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    public RepositorioSesionesPostgres(JdbcTemplate jdbc, TransactionTemplate transacciones) {
        this.jdbc = jdbc;
        this.transacciones = transacciones;
    }

    @Override
    public void crear(NuevaSesion sesion) {
        try {
            transacciones.executeWithoutResult(status -> {
                jdbc.update("""
                        INSERT INTO identidad.sesion_dispositivo
                            (id, usuario_id, creado_en, vence_en)
                        VALUES (?, ?, ?, ?)
                        """, sesion.sesionId(), sesion.usuarioId(), fecha(sesion.creadoEn()),
                        fecha(sesion.venceEn()));
                jdbc.update("""
                        INSERT INTO identidad.verificador_renovacion
                            (id, sesion_id, huella_sha256, creado_en)
                        VALUES (?, ?, ?, ?)
                        """, sesion.verificadorId(), sesion.sesionId(),
                        bytes(sesion.huellaRenovacion()), fecha(sesion.creadoEn()));
            });
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    @Override
    public Rotacion rotar(String huellaActual, UUID sucesorId,
            String huellaSucesor, Instant ahora) {
        try {
            return transacciones.execute(status -> rotarEnTransaccion(
                    huellaActual, sucesorId, huellaSucesor, ahora));
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    private Rotacion rotarEnTransaccion(String huellaActual, UUID sucesorId,
            String huellaSucesor, Instant ahora) {
        List<FilaRenovacion> filas = jdbc.query("""
                SELECT v.id AS verificador_id, v.estado AS verificador_estado,
                       s.id AS sesion_id, s.usuario_id, s.vence_en, s.revocado_en,
                       u.estado AS usuario_estado
                FROM identidad.verificador_renovacion v
                JOIN identidad.sesion_dispositivo s ON s.id = v.sesion_id
                JOIN identidad.usuario u ON u.id = s.usuario_id
                WHERE v.huella_sha256 = ?
                FOR UPDATE OF v, s, u
                """, (rs, row) -> new FilaRenovacion(
                        rs.getObject("verificador_id", UUID.class),
                        rs.getString("verificador_estado"),
                        rs.getObject("sesion_id", UUID.class),
                        rs.getObject("usuario_id", UUID.class),
                        rs.getObject("vence_en", OffsetDateTime.class).toInstant(),
                        rs.getObject("revocado_en") != null,
                        rs.getString("usuario_estado")), bytes(huellaActual));
        if (filas.isEmpty()) {
            return Rotacion.invalida();
        }

        FilaRenovacion fila = filas.getFirst();
        if ("CONSUMIDO".equals(fila.estadoVerificador())) {
            revocarFamilia(fila.sesionId(), ahora);
            return Rotacion.reutilizada();
        }
        if (!"ACTIVO".equals(fila.estadoVerificador()) || fila.sesionRevocada()
                || !"ACTIVO".equals(fila.estadoUsuario())
                || !fila.venceEn().isAfter(ahora)) {
            revocarFamilia(fila.sesionId(), ahora);
            return Rotacion.invalida();
        }

        jdbc.execute("SET CONSTRAINTS identidad.fk_verificador_sucesor DEFERRED");
        jdbc.update("""
                UPDATE identidad.verificador_renovacion
                SET estado = 'CONSUMIDO', consumido_en = ?, sucesor_id = ?
                WHERE id = ? AND estado = 'ACTIVO'
                """, fecha(ahora), sucesorId, fila.verificadorId());
        jdbc.update("""
                INSERT INTO identidad.verificador_renovacion
                    (id, sesion_id, huella_sha256, creado_en)
                VALUES (?, ?, ?, ?)
                """, sucesorId, fila.sesionId(), bytes(huellaSucesor), fecha(ahora));
        return Rotacion.valida(fila.sesionId(), fila.usuarioId(), fila.venceEn());
    }

    private void revocarFamilia(UUID sesionId, Instant ahora) {
        jdbc.update("""
                UPDATE identidad.sesion_dispositivo
                SET revocado_en = COALESCE(revocado_en, ?)
                WHERE id = ?
                """, fecha(ahora), sesionId);
        jdbc.update("""
                UPDATE identidad.verificador_renovacion
                SET estado = 'REVOCADO', revocado_en = COALESCE(revocado_en, ?)
                WHERE sesion_id = ? AND estado <> 'REVOCADO'
                """, fecha(ahora), sesionId);
    }

    @Override
    public boolean estaVigente(UUID sesionId, UUID usuarioId, Instant ahora) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM identidad.sesion_dispositivo s
                        JOIN identidad.usuario u ON u.id = s.usuario_id
                        WHERE s.id = ? AND s.usuario_id = ?
                          AND s.revocado_en IS NULL AND s.vence_en > ?
                          AND u.estado = 'ACTIVO'
                    )
                    """, Boolean.class, sesionId, usuarioId, fecha(ahora)));
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    private static byte[] bytes(String hexadecimal) {
        try {
            byte[] resultado = HexFormat.of().parseHex(hexadecimal);
            if (resultado.length != 32) {
                throw new IllegalArgumentException("La huella debe tener 32 bytes");
            }
            return resultado;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Huella SHA-256 inválida", exception);
        }
    }

    private static OffsetDateTime fecha(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record FilaRenovacion(UUID verificadorId, String estadoVerificador,
            UUID sesionId, UUID usuarioId, Instant venceEn, boolean sesionRevocada,
            String estadoUsuario) {
    }
}
