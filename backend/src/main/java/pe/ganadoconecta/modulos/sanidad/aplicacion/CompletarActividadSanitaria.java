package pe.ganadoconecta.modulos.sanidad.aplicacion;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;import pe.ganadoconecta.modulos.identidad.aplicacion.*;import pe.ganadoconecta.modulos.sanidad.dominio.*;
public final class CompletarActividadSanitaria{
 private final UnidadDeTrabajo unidad;private final AutorizarAccesoGanaderiaTransaccional auth;private final RepositorioAgendaSanitaria repo;private final Clock reloj;
 public CompletarActividadSanitaria(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioAgendaSanitaria r,Clock c){unidad=u;auth=a;repo=r;reloj=c;}
 public ActividadSanitaria ejecutar(UUID actor,UUID sesion,UUID g,UUID id,ConfirmacionActividad confirmacion){return unidad.ejecutar(()->{var ahora=reloj.instant().truncatedTo(ChronoUnit.MICROS);auth.autorizar(actor,sesion,g,PermisoGanaderia.ESCRIBIR,ahora);return repo.completar(g,id,actor,ahora,confirmacion);});}
}
