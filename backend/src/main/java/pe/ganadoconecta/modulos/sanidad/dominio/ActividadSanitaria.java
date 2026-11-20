package pe.ganadoconecta.modulos.sanidad.dominio;
import java.time.*;
import java.util.*;
public record ActividadSanitaria(UUID id,UUID ganaderiaId,UUID animalId,UUID eventoOrigenId,
 TipoActividadSanitaria tipo,LocalDate fechaPrevista,String descripcion,String observaciones,
 EstadoActividadSanitaria estado,long version,UUID creadoPor,Instant creadoEn,
 UUID completadoPor,Instant completadoEn){
 public ActividadSanitaria{
  if(id==null||id.version()!=4)throw new DatoSanitarioInvalidoException("actividadId");
  Objects.requireNonNull(ganaderiaId);Objects.requireNonNull(animalId);
  if(tipo==null)throw new DatoSanitarioInvalidoException("tipo");
  if(fechaPrevista==null)throw new DatoSanitarioInvalidoException("fechaPrevista");
  descripcion=texto(descripcion,"descripcion",1000,true);observaciones=texto(observaciones,"observaciones",2000,false);
  Objects.requireNonNull(estado);Objects.requireNonNull(creadoPor);Objects.requireNonNull(creadoEn);
 }
 public void validarFechaProgramable(LocalDate hoy){if(fechaPrevista.isBefore(hoy))throw new DatoSanitarioInvalidoException("fechaPrevista");}
 public String estadoVisible(LocalDate hoy){return estado==EstadoActividadSanitaria.COMPLETADA?"COMPLETADA":fechaPrevista.isBefore(hoy)?"VENCIDA":"PROXIMA";}
 private static String texto(String v,String c,int m,boolean req){if(v==null||v.strip().isEmpty()){if(req)throw new DatoSanitarioInvalidoException(c);return null;}String x=v.strip();if(x.codePointCount(0,x.length())>m||x.chars().anyMatch(n->(n<32&&n!='\n'&&n!='\r'&&n!='\t')||n==127))throw new DatoSanitarioInvalidoException(c);return x;}
}
