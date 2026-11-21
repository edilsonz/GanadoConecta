package pe.ganadoconecta.modulos.sanidad.aplicacion;
import java.time.Instant;import java.util.*;import pe.ganadoconecta.modulos.sanidad.dominio.*;
public interface RepositorioAgendaSanitaria{
 ActividadSanitaria crear(ActividadSanitaria actividad);
 List<ActividadSanitaria> listar(UUID ganaderia,UUID animal,TipoActividadSanitaria tipo,EstadoActividadSanitaria estado);
 ActividadSanitaria completar(UUID ganaderia,UUID actividad,UUID actor,Instant ahora,ConfirmacionActividad confirmacion);
}
