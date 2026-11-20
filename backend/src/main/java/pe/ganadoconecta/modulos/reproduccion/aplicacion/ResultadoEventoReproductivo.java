package pe.ganadoconecta.modulos.reproduccion.aplicacion;
import java.time.Instant; import java.util.UUID;
public record ResultadoEventoReproductivo(UUID operationId,UUID eventId,Estado estado,Instant confirmadoEn){public enum Estado{APLICADO,REPROCESADO}}
