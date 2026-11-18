package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.GanaderiaNoDisponibleException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaGanaderiaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioGanaderias;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

/** Persistencia AU05–AU07 con autorización revalidada en PostgreSQL. */
public final class RepositorioGanaderiasPostgres implements RepositorioGanaderias {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final AutorizarAccesoGanaderiaTransaccional autorizador;

    public RepositorioGanaderiasPostgres(JdbcTemplate jdbc,
            TransactionTemplate transacciones) {
        this(jdbc, transacciones, new AutorizarAccesoGanaderiaPostgres(jdbc));
    }

    public RepositorioGanaderiasPostgres(JdbcTemplate jdbc,
            TransactionTemplate transacciones,
            AutorizarAccesoGanaderiaTransaccional autorizador) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    @Override
    public ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId) {
        Objects.requireNonNull(ganaderia);
        Objects.requireNonNull(membresiaId);
        try {
            return Objects.requireNonNull(transacciones.execute(estado -> {
                int insertadas = jdbc.update("""
                        INSERT INTO identidad.ganaderia
                            (id, nombre, propietario_usuario_id, estado)
                        VALUES (?, ?, ?, 'ACTIVA')
                        ON CONFLICT (id) DO NOTHING
                        """, ganaderia.id(), ganaderia.nombre().valor(),
                        ganaderia.propietarioUsuarioId());

                if (insertadas == 0) {
                    if (esMismoContenido(ganaderia)) {
                        return ResultadoCreacion.YA_EXISTIA;
                    }
                    throw new GanaderiaNoDisponibleException();
                }

                jdbc.update("""
                        INSERT INTO identidad.membresia
                            (id, ganaderia_id, usuario_id, rol, estado)
                        VALUES (?, ?, ?, 'PROPIETARIO', 'ACTIVA')
                        """, membresiaId, ganaderia.id(),
                        ganaderia.propietarioUsuarioId());
                return ResultadoCreacion.CREADA;
            }));
        } catch (GanaderiaNoDisponibleException exception) {
            throw exception;
        } catch (DataAccessException | TransactionException exception) {
            throw new PersistenciaGanaderiaException(exception);
        }
    }

    @Override
    public List<GanaderiaAccesible> listarActivas(UUID usuarioId) {
        Objects.requireNonNull(usuarioId);
        try {
            return jdbc.query("""
                    SELECT g.id, g.nombre, g.estado, m.rol
                    FROM identidad.membresia m
                    JOIN identidad.ganaderia g ON g.id = m.ganaderia_id
                    WHERE m.usuario_id = ?
                      AND m.estado = 'ACTIVA'
                      AND g.estado = 'ACTIVA'
                    ORDER BY LOWER(g.nombre), g.id
                    """, (resultado, fila) -> new GanaderiaAccesible(
                            resultado.getObject("id", UUID.class),
                            resultado.getString("nombre"),
                            resultado.getString("estado"),
                            resultado.getString("rol")),
                    usuarioId);
        } catch (DataAccessException exception) {
            throw new PersistenciaGanaderiaException(exception);
        }
    }

    @Override
    public Optional<GanaderiaAccesible> buscarActiva(UUID usuarioId, UUID ganaderiaId) {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(ganaderiaId);
        try {
            return jdbc.query("""
                    SELECT g.id, g.nombre, g.estado, m.rol
                    FROM identidad.membresia m
                    JOIN identidad.ganaderia g ON g.id = m.ganaderia_id
                    WHERE m.usuario_id = ?
                      AND m.ganaderia_id = ?
                      AND m.estado = 'ACTIVA'
                      AND g.estado = 'ACTIVA'
                    """, (resultado, fila) -> new GanaderiaAccesible(
                            resultado.getObject("id", UUID.class),
                            resultado.getString("nombre"),
                            resultado.getString("estado"),
                            resultado.getString("rol")),
                    usuarioId, ganaderiaId).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new PersistenciaGanaderiaException(exception);
        }
    }

    @Override
    public ResultadoRevocacion revocarMembresia(UUID actorUsuarioId, UUID actorSesionId,
            UUID ganaderiaId, UUID objetivoUsuarioId, Instant revocadoEn) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(actorSesionId);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(objetivoUsuarioId);
        Objects.requireNonNull(revocadoEn);
        try {
            return Objects.requireNonNull(transacciones.execute(estado -> {
                try {
                    autorizador.autorizar(actorUsuarioId, actorSesionId, ganaderiaId,
                            PermisoGanaderia.ADMINISTRAR, revocadoEn);
                } catch (AccesoGanaderiaDenegadoException exception) {
                    return ResultadoRevocacion.ACTOR_NO_AUTORIZADO;
                }

                Optional<MembresiaBloqueada> objetivo = buscarMembresiaBloqueada(
                        ganaderiaId, objetivoUsuarioId);
                if (objetivo.isEmpty()) {
                    return ResultadoRevocacion.OBJETIVO_NO_ENCONTRADO;
                }
                if ("PROPIETARIO".equals(objetivo.get().rol())) {
                    return ResultadoRevocacion.OBJETIVO_PROPIETARIO;
                }
                if ("REVOCADA".equals(objetivo.get().estado())) {
                    return ResultadoRevocacion.YA_REVOCADA;
                }

                jdbc.update("""
                        UPDATE identidad.membresia
                        SET estado = 'REVOCADA', revocado_en = ?, actualizado_en = ?
                        WHERE ganaderia_id = ? AND usuario_id = ?
                        """, fecha(revocadoEn), fecha(revocadoEn),
                        ganaderiaId, objetivoUsuarioId);
                return ResultadoRevocacion.REVOCADA;
            }));
        } catch (DataAccessException | TransactionException
                | PersistenciaIdentidadException exception) {
            throw new PersistenciaGanaderiaException(exception);
        }
    }

    private Optional<MembresiaBloqueada> buscarMembresiaBloqueada(
            UUID ganaderiaId, UUID usuarioId) {
        return jdbc.query("""
                SELECT rol, estado
                FROM identidad.membresia
                WHERE ganaderia_id = ? AND usuario_id = ?
                FOR UPDATE
                """, (resultado, fila) -> new MembresiaBloqueada(
                        resultado.getString("rol"), resultado.getString("estado")),
                ganaderiaId, usuarioId).stream().findFirst();
    }

    private static OffsetDateTime fecha(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private boolean esMismoContenido(GanaderiaNueva ganaderia) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM identidad.ganaderia g
                    JOIN identidad.membresia m
                      ON m.ganaderia_id = g.id
                     AND m.usuario_id = g.propietario_usuario_id
                     AND m.rol = 'PROPIETARIO'
                     AND m.estado = 'ACTIVA'
                    WHERE g.id = ?
                      AND g.nombre = ?
                      AND g.propietario_usuario_id = ?
                      AND g.estado = 'ACTIVA'
                )
                """, Boolean.class, ganaderia.id(), ganaderia.nombre().valor(),
                ganaderia.propietarioUsuarioId()));
    }

    private record MembresiaBloqueada(String rol, String estado) {
    }

}
