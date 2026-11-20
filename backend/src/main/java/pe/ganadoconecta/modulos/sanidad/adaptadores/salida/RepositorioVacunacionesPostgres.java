package pe.ganadoconecta.modulos.sanidad.adaptadores.salida;

import java.security.MessageDigest;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConflictoSanitarioException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.PersistenciaSanitariaException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RepositorioVacunaciones;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ResultadoVacunacion;
import pe.ganadoconecta.modulos.sanidad.dominio.Vacunacion;

public final class RepositorioVacunacionesPostgres implements RepositorioVacunaciones {
    private final JdbcTemplate jdbc;

    public RepositorioVacunacionesPostgres(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public ResultadoVacunacion registrar(Vacunacion v, UUID operationId,
            byte[] huella, int schemaVersion) {
        exigirTransaccion();
        try {
            if (!existeAnimal(v.ganaderiaId(), v.animalId())) {
                throw new AnimalNoEncontradoException();
            }
            int reservadas = jdbc.update("""
                    INSERT INTO sanidad.operacion_confirmada (
                        operacion_id, ganaderia_id, animal_id, evento_id,
                        actor_usuario_id, tipo, schema_version, huella_sha256, confirmado_en
                    ) VALUES (?, ?, ?, ?, ?, 'REGISTRAR_VACUNACION', ?, ?, ?)
                    ON CONFLICT (operacion_id) DO NOTHING
                    """, operationId, v.ganaderiaId(), v.animalId(), v.id(),
                    v.creadoPor(), schemaVersion, huella,
                    OffsetDateTime.ofInstant(v.creadoEn(), ZoneOffset.UTC));
            if (reservadas == 0) return reprocesar(v, operationId, huella);
            jdbc.update("""
                    INSERT INTO sanidad.evento_sanitario (
                        id, ganaderia_id, animal_id, tipo, fecha_evento, nombre,
                        responsable, lote, observaciones, proxima_fecha, creado_por, creado_en
                    ) VALUES (?, ?, ?, 'VACUNACION', ?, ?, ?, ?, ?, ?, ?, ?)
                    """, v.id(), v.ganaderiaId(), v.animalId(), Date.valueOf(v.fechaAplicacion()),
                    v.nombreVacuna(), v.responsable(), v.lote(), v.observaciones(),
                    v.proximaFecha() == null ? null : Date.valueOf(v.proximaFecha()),
                    v.creadoPor(), OffsetDateTime.ofInstant(v.creadoEn(), ZoneOffset.UTC));
            return new ResultadoVacunacion(operationId, v.id(),
                    ResultadoVacunacion.Estado.APLICADO, v.creadoEn());
        } catch (AnimalNoEncontradoException | ConflictoSanitarioException e) {
            throw e;
        } catch (DuplicateKeyException e) {
            throw new ConflictoSanitarioException();
        } catch (DataAccessException e) {
            throw new PersistenciaSanitariaException(e);
        }
    }

    private ResultadoVacunacion reprocesar(Vacunacion v, UUID operationId, byte[] huella) {
        List<Operacion> operaciones = jdbc.query("""
                SELECT ganaderia_id, animal_id, evento_id, actor_usuario_id,
                       tipo, huella_sha256, confirmado_en
                FROM sanidad.operacion_confirmada
                WHERE operacion_id = ? FOR UPDATE
                """, (rs, row) -> new Operacion(
                        rs.getObject("ganaderia_id", UUID.class),
                        rs.getObject("animal_id", UUID.class),
                        rs.getObject("evento_id", UUID.class),
                        rs.getObject("actor_usuario_id", UUID.class),
                        rs.getString("tipo"), rs.getBytes("huella_sha256"),
                        rs.getObject("confirmado_en", OffsetDateTime.class)), operationId);
        if (operaciones.size() != 1 || !operaciones.getFirst().coincide(v, huella)) {
            throw new ConflictoSanitarioException();
        }
        return new ResultadoVacunacion(operationId, v.id(),
                ResultadoVacunacion.Estado.REPROCESADO,
                operaciones.getFirst().confirmadoEn().toInstant());
    }

    @Override
    public List<Vacunacion> listar(UUID ganaderiaId, UUID animalId) {
        try {
            if (!existeAnimal(ganaderiaId, animalId)) throw new AnimalNoEncontradoException();
            return jdbc.query("""
                    SELECT id, ganaderia_id, animal_id, nombre, fecha_evento,
                           responsable, lote, observaciones, proxima_fecha,
                           creado_por, creado_en
                    FROM sanidad.evento_sanitario
                    WHERE ganaderia_id = ? AND animal_id = ? AND tipo = 'VACUNACION'
                    ORDER BY fecha_evento DESC, creado_en DESC, id
                    """, (rs, row) -> new Vacunacion(
                            rs.getObject("id", UUID.class),
                            rs.getObject("ganaderia_id", UUID.class),
                            rs.getObject("animal_id", UUID.class), rs.getString("nombre"),
                            rs.getDate("fecha_evento").toLocalDate(),
                            rs.getString("responsable"), rs.getString("lote"),
                            rs.getString("observaciones"),
                            rs.getDate("proxima_fecha") == null ? null
                                    : rs.getDate("proxima_fecha").toLocalDate(),
                            rs.getObject("creado_por", UUID.class),
                            rs.getObject("creado_en", OffsetDateTime.class).toInstant()),
                    ganaderiaId, animalId);
        } catch (AnimalNoEncontradoException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new PersistenciaSanitariaException(e);
        }
    }

    private boolean existeAnimal(UUID ganaderiaId, UUID animalId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM ganado.animal WHERE ganaderia_id = ? AND id = ?)
                """, Boolean.class, ganaderiaId, animalId);
        return Boolean.TRUE.equals(existe);
    }

    private static void exigirTransaccion() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("El registro sanitario requiere una transacción");
        }
    }

    private record Operacion(UUID ganaderiaId, UUID animalId, UUID eventoId,
            UUID actor, String tipo, byte[] huella, OffsetDateTime confirmadoEn) {
        boolean coincide(Vacunacion v, byte[] otraHuella) {
            return ganaderiaId.equals(v.ganaderiaId()) && animalId.equals(v.animalId())
                    && eventoId.equals(v.id()) && actor.equals(v.creadoPor())
                    && "REGISTRAR_VACUNACION".equals(tipo)
                    && MessageDigest.isEqual(huella, otraHuella);
        }
    }
}
