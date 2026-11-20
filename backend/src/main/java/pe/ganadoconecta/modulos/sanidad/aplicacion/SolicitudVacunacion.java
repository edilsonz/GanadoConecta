package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

public record SolicitudVacunacion(UUID ganaderiaId, UUID animalId, UUID eventId,
        UUID operationId, int schemaVersion, String nombreVacuna,
        LocalDate fechaAplicacion, String responsable, String lote,
        String observaciones, LocalDate proximaFecha) {
}
