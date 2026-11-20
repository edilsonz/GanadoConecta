package pe.ganadoconecta.modulos.reproduccion.aplicacion;
import java.time.LocalDate; import java.util.UUID; import pe.ganadoconecta.modulos.reproduccion.dominio.*;
public record SolicitudEventoReproductivo(UUID ganaderiaId,UUID animalId,UUID eventId,
 UUID operationId,int schemaVersion,TipoEventoReproductivo tipo,LocalDate fecha,
 EstadoGestacion estadoGestacion,LocalDate fechaProbableParto,UUID reproductorAnimalId,
 String responsable,String observaciones){}
