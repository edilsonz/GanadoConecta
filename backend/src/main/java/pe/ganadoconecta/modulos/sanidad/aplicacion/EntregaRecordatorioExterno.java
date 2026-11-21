package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto EXT01. La selección de canal, consentimiento y política de entrega se resuelven antes de conectar un adaptador. */
@FunctionalInterface
public interface EntregaRecordatorioExterno {
    void solicitar(UUID entregaId, UUID destinatarioId, UUID ganaderiaId,
            UUID actividadId, LocalDate fechaPrevista);
}
