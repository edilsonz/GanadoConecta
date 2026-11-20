package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.util.List;
import java.util.UUID;
import pe.ganadoconecta.modulos.sanidad.dominio.EventoSanitario;
import pe.ganadoconecta.modulos.sanidad.dominio.TipoEventoSanitario;

public interface RepositorioEventosSanitarios {
    ResultadoEventoSanitario registrar(EventoSanitario evento, UUID operationId,
            byte[] huella, int schemaVersion);
    List<EventoSanitario> listar(UUID ganaderiaId, UUID animalId,
            TipoEventoSanitario filtro);
}
