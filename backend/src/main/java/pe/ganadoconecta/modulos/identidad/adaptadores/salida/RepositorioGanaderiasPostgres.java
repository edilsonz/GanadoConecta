package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.GanaderiaNoDisponibleException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaGanaderiaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioGanaderias;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

/** Persistencia AU05: ganadería y propietario se confirman en una transacción. */
public final class RepositorioGanaderiasPostgres implements RepositorioGanaderias {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    public RepositorioGanaderiasPostgres(JdbcTemplate jdbc,
            TransactionTemplate transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transacciones = Objects.requireNonNull(transacciones);
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
}
