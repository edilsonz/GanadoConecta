package pe.ganadoconecta.modulos.sanidad.dominio;
import java.time.*;
import java.util.*;
public record ActividadSanitaria(UUID id,UUID ganaderiaId,UUID animalId,UUID eventoOrigenId,
 TipoActividadSanitaria tipo,LocalDate fechaPrevista,String descripcion,String observaciones,
 EstadoActividadSanitaria estado,long version,UUID creadoPor,Instant creadoEn,
 UUID completadoPor,Instant completadoEn,List<UUID> participantes,List<UUID> atendidos,
 LocalDate fechaAtencion,String nombreVacuna){
 public ActividadSanitaria(UUID id,UUID ganaderiaId,UUID animalId,UUID eventoOrigenId,
   TipoActividadSanitaria tipo,LocalDate fechaPrevista,String descripcion,String observaciones,
   EstadoActividadSanitaria estado,long version,UUID creadoPor,Instant creadoEn,
   UUID completadoPor,Instant completadoEn) {
  this(id,ganaderiaId,animalId,eventoOrigenId,tipo,fechaPrevista,descripcion,observaciones,
    estado,version,creadoPor,creadoEn,completadoPor,completadoEn,List.of(),List.of(),null,null);
 }
 public ActividadSanitaria{
  if(id==null||id.version()!=4)throw new DatoSanitarioInvalidoException("actividadId");
  Objects.requireNonNull(ganaderiaId);
  participantes = seleccionar(participantes, "participantes");
  atendidos = seleccionar(atendidos, "atendidos");
  if ((animalId == null) == participantes.isEmpty()
      || (animalId == null && eventoOrigenId != null)
      || !participantes.containsAll(atendidos)) throw new DatoSanitarioInvalidoException("participantes");
  if(tipo==null)throw new DatoSanitarioInvalidoException("tipo");
  if (tipo.eventoDeCampana() != null && animalId != null) throw new DatoSanitarioInvalidoException("participantes");
  if(fechaPrevista==null)throw new DatoSanitarioInvalidoException("fechaPrevista");
  descripcion=texto(descripcion,"descripcion",1000,true);observaciones=texto(observaciones,"observaciones",2000,false);
  Objects.requireNonNull(estado);Objects.requireNonNull(creadoPor);Objects.requireNonNull(creadoEn);
 }
 public void validarFechaProgramable(LocalDate hoy){if(fechaPrevista.isBefore(hoy))throw new DatoSanitarioInvalidoException("fechaPrevista");}
 public String estadoVisible(LocalDate hoy){return estado==EstadoActividadSanitaria.COMPLETADA?"COMPLETADA":fechaPrevista.isBefore(hoy)?"VENCIDA":fechaPrevista.equals(hoy)?"PENDIENTE":"PROXIMA";}
 public static List<UUID> seleccionar(List<UUID> ids, String campo) {
  if (ids == null) return List.of();
  if (ids.size() > 100 || ids.stream().anyMatch(Objects::isNull)
      || new HashSet<>(ids).size() != ids.size()) throw new DatoSanitarioInvalidoException(campo);
  return ids.stream().sorted().toList();
 }
 private static String texto(String v,String c,int m,boolean req){if(v==null||v.strip().isEmpty()){if(req)throw new DatoSanitarioInvalidoException(c);return null;}String x=v.strip();if(x.codePointCount(0,x.length())>m||x.chars().anyMatch(n->(n<32&&n!='\n'&&n!='\r'&&n!='\t')||n==127))throw new DatoSanitarioInvalidoException(c);return x;}
}
