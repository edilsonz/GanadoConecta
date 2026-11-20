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
import pe.ganadoconecta.modulos.sanidad.aplicacion.*;
import pe.ganadoconecta.modulos.sanidad.dominio.*;

public final class RepositorioEventosSanitariosPostgres
        implements RepositorioEventosSanitarios {
    private final JdbcTemplate jdbc;
    public RepositorioEventosSanitariosPostgres(JdbcTemplate jdbc) {
        this.jdbc=Objects.requireNonNull(jdbc);
    }
    @Override public ResultadoEventoSanitario registrar(EventoSanitario e,
            UUID operacion, byte[] huella, int schema) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("El registro sanitario requiere una transacción");
        try {
            exigirAnimal(e.ganaderiaId(),e.animalId());
            int insertadas=jdbc.update("""
                    INSERT INTO sanidad.operacion_confirmada (
                      operacion_id,ganaderia_id,animal_id,evento_id,actor_usuario_id,
                      tipo,schema_version,huella_sha256,confirmado_en)
                    VALUES (?,?,?,? ,?,'REGISTRAR_EVENTO_SANITARIO',?,?,?)
                    ON CONFLICT (operacion_id) DO NOTHING
                    """,operacion,e.ganaderiaId(),e.animalId(),e.id(),e.creadoPor(),
                    schema,huella,OffsetDateTime.ofInstant(e.creadoEn(),ZoneOffset.UTC));
            if(insertadas==0)return reprocesar(e,operacion,huella);
            jdbc.update("""
                    INSERT INTO sanidad.evento_sanitario (
                      id,ganaderia_id,animal_id,tipo,fecha_evento,descripcion,
                      responsable,producto,indicaciones,observaciones,creado_por,creado_en)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """,e.id(),e.ganaderiaId(),e.animalId(),e.tipo().name(),
                    Date.valueOf(e.fecha()),e.descripcion(),e.responsable(),e.producto(),
                    e.indicaciones(),e.observaciones(),e.creadoPor(),
                    OffsetDateTime.ofInstant(e.creadoEn(),ZoneOffset.UTC));
            return new ResultadoEventoSanitario(operacion,e.id(),
                    ResultadoEventoSanitario.Estado.APLICADO,e.creadoEn());
        }catch(AnimalNoEncontradoException|ConflictoSanitarioException ex){throw ex;}
        catch(DuplicateKeyException ex){throw new ConflictoSanitarioException();}
        catch(DataAccessException ex){throw new PersistenciaSanitariaException(ex);}
    }
    private ResultadoEventoSanitario reprocesar(EventoSanitario e,UUID operacion,byte[] huella){
        List<Operacion> guardadas=jdbc.query("""
                SELECT ganaderia_id,animal_id,evento_id,actor_usuario_id,tipo,
                       huella_sha256,confirmado_en FROM sanidad.operacion_confirmada
                WHERE operacion_id=? FOR UPDATE
                """,(rs,n)->new Operacion(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),
                    rs.getObject(3,UUID.class),rs.getObject(4,UUID.class),rs.getString(5),
                    rs.getBytes(6),rs.getObject(7,OffsetDateTime.class)),operacion);
        if(guardadas.size()!=1||!guardadas.getFirst().coincide(e,huella))
            throw new ConflictoSanitarioException();
        return new ResultadoEventoSanitario(operacion,e.id(),
                ResultadoEventoSanitario.Estado.REPROCESADO,
                guardadas.getFirst().confirmado().toInstant());
    }
    @Override public List<EventoSanitario> listar(UUID ganaderia,UUID animal,
            TipoEventoSanitario filtro){
        try{
            exigirAnimal(ganaderia,animal);
            String tipo=filtro==null?null:filtro.name();
            return jdbc.query("""
                    SELECT id,ganaderia_id,animal_id,tipo,fecha_evento,nombre,
                      descripcion,responsable,lote,producto,indicaciones,observaciones,
                      proxima_fecha,creado_por,creado_en
                    FROM sanidad.evento_sanitario
                    WHERE ganaderia_id=? AND animal_id=?
                      AND (CAST(? AS VARCHAR) IS NULL OR tipo=?)
                    ORDER BY fecha_evento DESC,creado_en DESC,id
                    """,(rs,n)->new EventoSanitario(rs.getObject("id",UUID.class),
                      rs.getObject("ganaderia_id",UUID.class),rs.getObject("animal_id",UUID.class),
                      TipoEventoSanitario.valueOf(rs.getString("tipo")),rs.getDate("fecha_evento").toLocalDate(),
                      rs.getString("nombre"),rs.getString("descripcion"),rs.getString("responsable"),
                      rs.getString("lote"),rs.getString("producto"),rs.getString("indicaciones"),
                      rs.getString("observaciones"),rs.getDate("proxima_fecha")==null?null:rs.getDate("proxima_fecha").toLocalDate(),
                      rs.getObject("creado_por",UUID.class),rs.getObject("creado_en",OffsetDateTime.class).toInstant()),
                    ganaderia,animal,tipo,tipo);
        }catch(AnimalNoEncontradoException ex){throw ex;}
        catch(DataAccessException ex){throw new PersistenciaSanitariaException(ex);}
    }
    private void exigirAnimal(UUID ganaderia,UUID animal){
        Boolean existe=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM ganado.animal WHERE ganaderia_id=? AND id=?)",Boolean.class,ganaderia,animal);
        if(!Boolean.TRUE.equals(existe))throw new AnimalNoEncontradoException();
    }
    private record Operacion(UUID ganaderia,UUID animal,UUID evento,UUID actor,
            String tipo,byte[] huella,OffsetDateTime confirmado){
        boolean coincide(EventoSanitario e,byte[] otra){return ganaderia.equals(e.ganaderiaId())
            &&animal.equals(e.animalId())&&evento.equals(e.id())&&actor.equals(e.creadoPor())
            &&"REGISTRAR_EVENTO_SANITARIO".equals(tipo)&&MessageDigest.isEqual(huella,otra);}
    }
}
