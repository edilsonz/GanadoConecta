package pe.ganadoconecta.modulos.reproduccion.adaptadores.salida;

import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.reproduccion.aplicacion.*;
import pe.ganadoconecta.modulos.reproduccion.dominio.*;

public final class RepositorioGenealogiaPostgres implements RepositorioGenealogia {
    private final JdbcTemplate jdbc;
    public RepositorioGenealogiaPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public ResultadoParentesco corregir(SolicitudParentesco s, UUID actor,
            byte[] huella, Instant ahora) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Se requiere transacción");
        try {
            validar(s);
            int nueva = jdbc.update("INSERT INTO reproduccion.operacion_parentesco(operacion_id,ganaderia_id,animal_id,tipo,progenitor_animal_id,actor_usuario_id,schema_version,huella_sha256,confirmado_en) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(operacion_id) DO NOTHING",
                    s.operationId(), s.ganaderiaId(), s.animalId(), s.tipo().name(),
                    s.progenitorAnimalId(), actor, s.schemaVersion(), huella,
                    OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
            if (nueva == 0) return reprocesar(s, actor, huella);
            List<UUID> anteriores = jdbc.query("SELECT progenitor_animal_id FROM reproduccion.parentesco WHERE ganaderia_id=? AND animal_id=? AND tipo=? FOR UPDATE",
                    (r,n)->r.getObject(1,UUID.class), s.ganaderiaId(), s.animalId(), s.tipo().name());
            UUID anterior = anteriores.isEmpty() ? null : anteriores.getFirst();
            if (s.progenitorAnimalId() == null) {
                jdbc.update("DELETE FROM reproduccion.parentesco WHERE ganaderia_id=? AND animal_id=? AND tipo=?",
                        s.ganaderiaId(), s.animalId(), s.tipo().name());
            } else {
                jdbc.update("INSERT INTO reproduccion.parentesco(ganaderia_id,animal_id,tipo,progenitor_animal_id,origen,parto_id,actualizado_por,actualizado_en) VALUES(?,?,?,?,'CORRECCION',NULL,?,?) ON CONFLICT(ganaderia_id,animal_id,tipo) DO UPDATE SET progenitor_animal_id=EXCLUDED.progenitor_animal_id,origen='CORRECCION',parto_id=NULL,actualizado_por=EXCLUDED.actualizado_por,actualizado_en=EXCLUDED.actualizado_en,version=reproduccion.parentesco.version+1",
                        s.ganaderiaId(), s.animalId(), s.tipo().name(),
                        s.progenitorAnimalId(), actor,
                        OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
            }
            jdbc.update("INSERT INTO reproduccion.parentesco_historial(id,ganaderia_id,animal_id,tipo,progenitor_anterior_id,progenitor_nuevo_id,motivo,actor_usuario_id,operacion_id,registrado_en) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(), s.ganaderiaId(), s.animalId(), s.tipo().name(),
                    anterior, s.progenitorAnimalId(),
                    s.progenitorAnimalId()==null ? "RETIRO" : "CORRECCION",
                    actor, s.operationId(), OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
            return new ResultadoParentesco(s.operationId(), s.animalId(), "APLICADO", ahora);
        } catch (AnimalNoEncontradoException | DatoReproductivoInvalidoException
                | ConflictoReproductivoException e) { throw e; }
        catch (DuplicateKeyException e) { throw new ConflictoReproductivoException(); }
        catch (DataAccessException e) { throw new PersistenciaReproductivaException(e); }
    }

    private void validar(SolicitudParentesco s) {
        if (sexo(s.ganaderiaId(), s.animalId()) == null) throw new AnimalNoEncontradoException();
        if (s.progenitorAnimalId() == null) return;
        if (s.animalId().equals(s.progenitorAnimalId()))
            throw new DatoReproductivoInvalidoException("progenitorAnimalId");
        String sexo = sexo(s.ganaderiaId(), s.progenitorAnimalId());
        if (sexo == null || (s.tipo()==TipoParentesco.MADRE
                ? !"HEMBRA".equals(sexo) : !"MACHO".equals(sexo)))
            throw new DatoReproductivoInvalidoException("progenitorAnimalId");
        Boolean ciclo = jdbc.queryForObject("WITH RECURSIVE anc(id) AS (SELECT CAST(? AS uuid) UNION SELECT p.progenitor_animal_id FROM reproduccion.parentesco p JOIN anc a ON p.animal_id=a.id WHERE p.ganaderia_id=?) SELECT EXISTS(SELECT 1 FROM anc WHERE id=?)",
                Boolean.class, s.progenitorAnimalId(), s.ganaderiaId(), s.animalId());
        if (Boolean.TRUE.equals(ciclo))
            throw new DatoReproductivoInvalidoException("progenitorAnimalId");
    }

    private String sexo(UUID g, UUID a) {
        List<String> x=jdbc.query("SELECT sexo FROM ganado.animal WHERE ganaderia_id=? AND id=?",
                (r,n)->r.getString(1),g,a);
        return x.isEmpty()?null:x.getFirst();
    }

    private ResultadoParentesco reprocesar(SolicitudParentesco s, UUID actor, byte[] h) {
        List<Guardada>x=jdbc.query("SELECT ganaderia_id,animal_id,tipo,progenitor_animal_id,actor_usuario_id,huella_sha256,confirmado_en FROM reproduccion.operacion_parentesco WHERE operacion_id=? FOR UPDATE",
                (r,n)->new Guardada(r.getObject(1,UUID.class),r.getObject(2,UUID.class),
                        r.getString(3),r.getObject(4,UUID.class),r.getObject(5,UUID.class),
                        r.getBytes(6),r.getObject(7,OffsetDateTime.class)),s.operationId());
        if(x.size()!=1||!x.getFirst().coincide(s,actor,h))throw new ConflictoReproductivoException();
        return new ResultadoParentesco(s.operationId(),s.animalId(),"REPROCESADO",x.getFirst().fecha.toInstant());
    }

    @Override public GenealogiaAnimal consultar(UUID g, UUID animal) {
        if(sexo(g,animal)==null)throw new AnimalNoEncontradoException();
        try {
            var asc=jdbc.query("WITH RECURSIVE anc(id,relacion,nivel) AS (SELECT p.progenitor_animal_id,p.tipo,1 FROM reproduccion.parentesco p WHERE p.ganaderia_id=? AND p.animal_id=? UNION ALL SELECT p.progenitor_animal_id,p.tipo,a.nivel+1 FROM reproduccion.parentesco p JOIN anc a ON p.animal_id=a.id WHERE p.ganaderia_id=? AND a.nivel<2) SELECT a.id,x.nombre,x.sexo,a.relacion,a.nivel FROM anc a JOIN ganado.animal x ON x.id=a.id AND x.ganaderia_id=? ORDER BY a.nivel,a.relacion,a.id",
                    (r,n)->new GenealogiaAnimal.Ascendiente(r.getObject(1,UUID.class),
                            r.getString(2),r.getString(3),TipoParentesco.valueOf(r.getString(4)),r.getInt(5)),
                    g,animal,g,g);
            var desc=jdbc.query("SELECT x.id,x.nombre,x.sexo,p.tipo FROM reproduccion.parentesco p JOIN ganado.animal x ON x.id=p.animal_id AND x.ganaderia_id=p.ganaderia_id WHERE p.ganaderia_id=? AND p.progenitor_animal_id=? ORDER BY x.nombre NULLS LAST,x.id",
                    (r,n)->new GenealogiaAnimal.Descendiente(r.getObject(1,UUID.class),
                            r.getString(2),r.getString(3),TipoParentesco.valueOf(r.getString(4))),g,animal);
            return new GenealogiaAnimal(animal,asc,desc);
        } catch(DataAccessException e){throw new PersistenciaReproductivaException(e);}
    }

    private record Guardada(UUID g,UUID a,String tipo,UUID progenitor,UUID actor,
            byte[] huella,OffsetDateTime fecha){
        boolean coincide(SolicitudParentesco s,UUID u,byte[]h){return g.equals(s.ganaderiaId())
                &&a.equals(s.animalId())&&tipo.equals(s.tipo().name())
                &&Objects.equals(progenitor,s.progenitorAnimalId())&&actor.equals(u)
                &&MessageDigest.isEqual(huella,h);}
    }
}
