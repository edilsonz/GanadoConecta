package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.time.LocalDate;
import java.util.UUID;
import pe.ganadoconecta.modulos.sanidad.dominio.TipoEventoSanitario;

public record SolicitudEventoSanitario(UUID ganaderiaId, UUID animalId,
        UUID eventId, UUID operationId, int schemaVersion,
        TipoEventoSanitario tipo, LocalDate fecha, String descripcion,
        String responsable, String producto, String indicaciones,
        String observaciones) { }
