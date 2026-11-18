package pe.ganadoconecta.modulos.ganado.adaptadores.salida;

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
import pe.ganadoconecta.modulos.ganado.aplicacion.ConflictoRegistroAnimalException;
import pe.ganadoconecta.modulos.ganado.aplicacion.PersistenciaAnimalException;
import pe.ganadoconecta.modulos.ganado.aplicacion.RepositorioAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.ResultadoRegistroAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;

public final class RepositorioAnimalesPostgres implements RepositorioAnimales {
    private final JdbcTemplate jdbc;
    private final Runnable despuesDeReservar;

    public RepositorioAnimalesPostgres(JdbcTemplate jdbc) {
        this(jdbc, () -> { });
    }

    RepositorioAnimalesPostgres(JdbcTemplate jdbc, Runnable despuesDeReservar) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.despuesDeReservar = Objects.requireNonNull(despuesDeReservar);
    }

    @Override
    public ResultadoRegistroAnimal registrar(Animal animal, OperacionId operacionId,
            UUID actorUsuarioId, byte[] huellaSha256) {
        exigirTransaccion();
        try {
            boolean areteCoincidente = existeAreteCoincidente(animal);
            int reservadas = reservarOperacion(animal, operacionId, actorUsuarioId,
                    huellaSha256, areteCoincidente);
            if (reservadas == 0) {
                return reprocesarSiCoincide(animal, operacionId, actorUsuarioId,
                        huellaSha256);
            }
            despuesDeReservar.run();
            insertarAnimal(animal);
            return new ResultadoRegistroAnimal(animal.id(), operacionId, animal.version(),
                    animal.creadoEn(), ResultadoRegistroAnimal.EstadoResultado.APLICADO,
                    areteCoincidente);
        } catch (ConflictoRegistroAnimalException exception) {
            throw exception;
        } catch (DuplicateKeyException exception) {
            throw new ConflictoRegistroAnimalException();
        } catch (DataAccessException exception) {
            throw new PersistenciaAnimalException(exception);
        }
    }

    private int reservarOperacion(Animal animal, OperacionId operacionId,
            UUID actorUsuarioId, byte[] huella, boolean areteCoincidente) {
        return jdbc.update("""
                INSERT INTO ganado.operacion_confirmada (
                    operacion_id, ganaderia_id, actor_usuario_id, animal_id,
                    tipo, schema_version, base_version, huella_sha256,
                    version_creada, confirmado_en, arete_coincidente
                ) VALUES (?, ?, ?, ?, 'CREAR_ANIMAL', 1, 0, ?, 1, ?, ?)
                ON CONFLICT (operacion_id) DO NOTHING
                """, operacionId.valor(), animal.ganaderiaId().valor(), actorUsuarioId,
                animal.id().valor(), huella,
                OffsetDateTime.ofInstant(animal.creadoEn(), ZoneOffset.UTC),
                areteCoincidente);
    }

    private ResultadoRegistroAnimal reprocesarSiCoincide(Animal animal,
            OperacionId operacionId, UUID actorUsuarioId, byte[] huella) {
        List<OperacionGuardada> guardadas = jdbc.query("""
                SELECT ganaderia_id, actor_usuario_id, animal_id, huella_sha256,
                       version_creada, confirmado_en, arete_coincidente
                FROM ganado.operacion_confirmada
                WHERE operacion_id = ?
                FOR UPDATE
                """, (resultado, fila) -> new OperacionGuardada(
                        resultado.getObject("ganaderia_id", UUID.class),
                        resultado.getObject("actor_usuario_id", UUID.class),
                        resultado.getObject("animal_id", UUID.class),
                        resultado.getBytes("huella_sha256"),
                        resultado.getLong("version_creada"),
                        resultado.getObject("confirmado_en", OffsetDateTime.class),
                        resultado.getBoolean("arete_coincidente")),
                operacionId.valor());
        if (guardadas.size() != 1 || !guardadas.getFirst().coincide(
                animal, actorUsuarioId, huella)) {
            throw new ConflictoRegistroAnimalException();
        }
        OperacionGuardada guardada = guardadas.getFirst();
        return new ResultadoRegistroAnimal(animal.id(), operacionId,
                guardada.versionCreada(), guardada.confirmadoEn().toInstant(),
                ResultadoRegistroAnimal.EstadoResultado.REPROCESADO,
                guardada.areteCoincidente());
    }

    private void insertarAnimal(Animal animal) {
        DatosAnimal datos = animal.datos();
        jdbc.update("""
                INSERT INTO ganado.animal (
                    id, ganaderia_id, sexo, nombre, arete_frente, arete_reverso,
                    raza, fecha_nacimiento, nacimiento_estimado, categoria,
                    observaciones, estado, version, creado_por, creado_en
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, animal.id().valor(), animal.ganaderiaId().valor(),
                datos.sexo().name(), datos.nombre(), datos.areteFrente(),
                datos.areteReverso(), datos.raza(), fechaSql(datos),
                datos.nacimientoEstimado(),
                datos.categoria() == null ? null : datos.categoria().name(),
                datos.observaciones(), animal.estado().name(), animal.version(),
                animal.creadoPor(),
                OffsetDateTime.ofInstant(animal.creadoEn(), ZoneOffset.UTC));
    }

    private boolean existeAreteCoincidente(Animal animal) {
        String frente = animal.datos().areteFrente();
        if (frente == null) {
            return false;
        }
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM ganado.animal
                WHERE ganaderia_id = ? AND id <> ?
                  AND LOWER(arete_frente) = LOWER(?)
                """, Integer.class, animal.ganaderiaId().valor(), animal.id().valor(),
                frente);
        return cantidad != null && cantidad > 0;
    }

    private static Date fechaSql(DatosAnimal datos) {
        return datos.fechaNacimiento() == null ? null : Date.valueOf(datos.fechaNacimiento());
    }

    private static void exigirTransaccion() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("El registro de animales requiere una transacción");
        }
    }

    private record OperacionGuardada(UUID ganaderiaId, UUID actorUsuarioId,
            UUID animalId, byte[] huella, long versionCreada,
            OffsetDateTime confirmadoEn, boolean areteCoincidente) {
        private boolean coincide(Animal animal, UUID actor, byte[] otraHuella) {
            return ganaderiaId.equals(animal.ganaderiaId().valor())
                    && actorUsuarioId.equals(actor)
                    && animalId.equals(animal.id().valor())
                    && MessageDigest.isEqual(huella, otraHuella);
        }
    }
}
