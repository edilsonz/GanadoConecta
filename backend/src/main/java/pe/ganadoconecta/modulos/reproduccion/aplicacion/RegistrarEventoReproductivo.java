package pe.ganadoconecta.modulos.reproduccion.aplicacion;
import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;import pe.ganadoconecta.modulos.identidad.aplicacion.*;import pe.ganadoconecta.modulos.reproduccion.dominio.*;
public final class RegistrarEventoReproductivo{
 private final UnidadDeTrabajo unidad;private final AutorizarAccesoGanaderiaTransaccional autorizador;private final RepositorioReproduccion repo;private final Clock reloj;
 public RegistrarEventoReproductivo(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioReproduccion r,Clock c){unidad=u;autorizador=a;repo=r;reloj=c;}
 public ResultadoEventoReproductivo ejecutar(SolicitudEventoReproductivo s,UUID actor,UUID sesion){
  if(s==null||s.operationId()==null||s.operationId().version()!=4)throw new DatoReproductivoInvalidoException("operationId");if(s.schemaVersion()!=1)throw new DatoReproductivoInvalidoException("schemaVersion");
  Instant ahora=reloj.instant().truncatedTo(ChronoUnit.MICROS);var e=new EventoReproductivo(s.eventId(),s.ganaderiaId(),s.animalId(),s.tipo(),s.fecha(),s.estadoGestacion(),s.fechaProbableParto(),s.reproductorAnimalId(),s.responsable(),s.observaciones(),actor,ahora);e.validarNoFuturo(ahora.atZone(ZoneId.of("America/Lima")).toLocalDate());byte[] h=huella(e,s.operationId());
  return unidad.ejecutar(()->{autorizador.autorizar(actor,sesion,s.ganaderiaId(),PermisoGanaderia.ESCRIBIR,ahora);return repo.registrar(e,s.operationId(),h,s.schemaVersion());});
 }
 private static byte[] huella(EventoReproductivo e,UUID op){try{return MessageDigest.getInstance("SHA-256").digest(String.join("\u001f",op.toString(),e.id().toString(),e.ganaderiaId().toString(),e.animalId().toString(),e.tipo().name(),e.fecha().toString(),Objects.toString(e.estadoGestacion(),""),Objects.toString(e.fechaProbableParto(),""),Objects.toString(e.reproductorAnimalId(),""),Objects.toString(e.responsable(),""),Objects.toString(e.observaciones(),""),e.creadoPor().toString()).getBytes(StandardCharsets.UTF_8));}catch(NoSuchAlgorithmException x){throw new IllegalStateException(x);}}
}
