package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.util.List;
import java.util.UUID;
import pe.ganadoconecta.modulos.sanidad.dominio.Vacunacion;

public interface RepositorioVacunaciones {
    ResultadoVacunacion registrar(Vacunacion vacunacion, UUID operationId,
            byte[] huella, int schemaVersion);
    List<Vacunacion> listar(UUID ganaderiaId, UUID animalId);
}
