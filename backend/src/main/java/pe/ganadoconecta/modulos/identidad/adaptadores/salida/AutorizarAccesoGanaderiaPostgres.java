package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;

/** Autorización bloqueante compartida por AU07 y las escrituras de negocio. */
public final class AutorizarAccesoGanaderiaPostgres
        implements AutorizarAccesoGanaderiaTransaccional {
    private final JdbcTemplate jdbc;

    public AutorizarAccesoGanaderiaPostgres(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public GanaderiaAccesible autorizar(UUID actorUsuarioId, UUID actorSesionId,
            UUID ganaderiaId, PermisoGanaderia permiso, Instant ahora) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(actorSesionId);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(permiso);
        Objects.requireNonNull(ahora);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "La autorización de escritura requiere una transacción activa");
        }
        try {
            List<SesionBloqueada> sesiones = jdbc.query("""
                    SELECT s.revocado_en, s.vence_en, u.estado AS usuario_estado
                    FROM identidad.sesion_dispositivo s
                    JOIN identidad.usuario u ON u.id = s.usuario_id
                    WHERE s.id = ? AND s.usuario_id = ?
                    FOR UPDATE OF s, u
                    """, (resultado, fila) -> new SesionBloqueada(
                            resultado.getObject("revocado_en", OffsetDateTime.class),
                            resultado.getObject("vence_en", OffsetDateTime.class),
                            resultado.getString("usuario_estado")),
                    actorSesionId, actorUsuarioId);
            if (sesiones.isEmpty() || !sesiones.getFirst().estaVigente(ahora)) {
                throw new AccesoGanaderiaDenegadoException();
            }

            List<GanaderiaBloqueada> ganaderias = jdbc.query("""
                    SELECT id, nombre, estado
                    FROM identidad.ganaderia
                    WHERE id = ?
                    FOR UPDATE
                    """, (resultado, fila) -> new GanaderiaBloqueada(
                            resultado.getObject("id", UUID.class),
                            resultado.getString("nombre"),
                            resultado.getString("estado")), ganaderiaId);
            if (ganaderias.isEmpty() || !"ACTIVA".equals(ganaderias.getFirst().estado())) {
                throw new AccesoGanaderiaDenegadoException();
            }

            List<MembresiaBloqueada> membresias = jdbc.query("""
                    SELECT rol, estado
                    FROM identidad.membresia
                    WHERE ganaderia_id = ? AND usuario_id = ?
                    FOR UPDATE
                    """, (resultado, fila) -> new MembresiaBloqueada(
                            resultado.getString("rol"), resultado.getString("estado")),
                    ganaderiaId, actorUsuarioId);
            if (membresias.isEmpty()
                    || !"ACTIVA".equals(membresias.getFirst().estado())
                    || !permiso.permite(membresias.getFirst().rol())) {
                throw new AccesoGanaderiaDenegadoException();
            }
            GanaderiaBloqueada ganaderia = ganaderias.getFirst();
            return new GanaderiaAccesible(ganaderia.id(), ganaderia.nombre(),
                    ganaderia.estado(), membresias.getFirst().rol());
        } catch (AccesoGanaderiaDenegadoException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }

    private record SesionBloqueada(OffsetDateTime revocadoEn,
            OffsetDateTime venceEn, String usuarioEstado) {
        private boolean estaVigente(Instant ahora) {
            return revocadoEn == null
                    && venceEn.toInstant().isAfter(ahora)
                    && "ACTIVO".equals(usuarioEstado);
        }
    }

    private record GanaderiaBloqueada(UUID id, String nombre, String estado) {
    }

    private record MembresiaBloqueada(String rol, String estado) {
    }
}
