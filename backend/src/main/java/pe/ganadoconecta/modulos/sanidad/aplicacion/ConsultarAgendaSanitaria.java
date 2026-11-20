package pe.ganadoconecta.modulos.sanidad.aplicacion;
import java.util.*;import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;import pe.ganadoconecta.modulos.sanidad.dominio.*;
public final class ConsultarAgendaSanitaria{
 private final AutorizarLecturaGanaderia auth;private final RepositorioAgendaSanitaria repo;
 public ConsultarAgendaSanitaria(AutorizarLecturaGanaderia a,RepositorioAgendaSanitaria r){auth=a;repo=r;}
 public List<ActividadSanitaria> ejecutar(UUID actor,UUID g,UUID animal,TipoActividadSanitaria tipo,EstadoActividadSanitaria estado){auth.autorizar(actor,g);return repo.listar(g,animal,tipo,estado);}
}
