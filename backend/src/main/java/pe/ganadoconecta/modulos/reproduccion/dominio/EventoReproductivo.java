package pe.ganadoconecta.modulos.reproduccion.dominio;
import java.text.Normalizer; import java.time.*; import java.util.*;
public record EventoReproductivo(UUID id,UUID ganaderiaId,UUID animalId,
 TipoEventoReproductivo tipo,LocalDate fecha,EstadoGestacion estadoGestacion,
 LocalDate fechaProbableParto,UUID reproductorAnimalId,String responsable,
 String observaciones,UUID creadoPor,Instant creadoEn){
 public EventoReproductivo{
  if(id==null||id.version()!=4)throw new DatoReproductivoInvalidoException("eventId");
  Objects.requireNonNull(ganaderiaId);Objects.requireNonNull(animalId);Objects.requireNonNull(tipo);
  if(fecha==null)throw new DatoReproductivoInvalidoException("fecha");
  if((tipo==TipoEventoReproductivo.EVALUACION_GESTACION)!=(estadoGestacion!=null))throw new DatoReproductivoInvalidoException("estadoGestacion");
  if(fechaProbableParto!=null&&(tipo!=TipoEventoReproductivo.EVALUACION_GESTACION||estadoGestacion!=EstadoGestacion.CONFIRMADA||fechaProbableParto.isBefore(fecha)))throw new DatoReproductivoInvalidoException("fechaProbableParto");
  responsable=texto(responsable,150,"responsable");observaciones=texto(observaciones,2000,"observaciones");
  Objects.requireNonNull(creadoPor);Objects.requireNonNull(creadoEn);
 }
 public void validarNoFuturo(LocalDate hoy){if(fecha.isAfter(hoy))throw new DatoReproductivoInvalidoException("fecha");}
 private static String texto(String v,int max,String c){if(v==null)return null;String n=Normalizer.normalize(v,Normalizer.Form.NFC).strip();if(n.isEmpty())return null;if(n.codePointCount(0,n.length())>max||n.codePoints().anyMatch(x->Character.isISOControl(x)&&x!='\n'&&x!='\r'))throw new DatoReproductivoInvalidoException(c);return n;}
}
