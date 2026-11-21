package pe.ganadoconecta.modulos.sanidad.aplicacion;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;import pe.ganadoconecta.modulos.identidad.aplicacion.*;import pe.ganadoconecta.modulos.sanidad.dominio.*;
public final class ProgramarActividadSanitaria{
 private final UnidadDeTrabajo unidad;private final AutorizarAccesoGanaderiaTransaccional auth;private final RepositorioAgendaSanitaria repo;private final Clock reloj;
 public ProgramarActividadSanitaria(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioAgendaSanitaria r,Clock c){unidad=u;auth=a;repo=r;reloj=c;}
 public ActividadSanitaria ejecutar(UUID actor,UUID sesion,UUID g,UUID animal,UUID id,UUID evento,TipoActividadSanitaria tipo,LocalDate fecha,String descripcion,String obs,List<UUID> participantes){
  var ahora=reloj.instant().truncatedTo(ChronoUnit.MICROS);var a=new ActividadSanitaria(id,g,animal,evento,tipo,fecha,descripcion,obs,EstadoActividadSanitaria.PENDIENTE,1,actor,ahora,null,null,participantes,List.of(),null,null);
  return unidad.ejecutar(()->{auth.autorizar(actor,sesion,g,PermisoGanaderia.ESCRIBIR,ahora);return repo.crear(a);});}
}
