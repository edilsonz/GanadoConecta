package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

public record ResultadoEventoSanitario(UUID operationId, UUID eventId,
        Estado estado, Instant confirmadoEn) {
    public enum Estado { APLICADO, REPROCESADO }
}
