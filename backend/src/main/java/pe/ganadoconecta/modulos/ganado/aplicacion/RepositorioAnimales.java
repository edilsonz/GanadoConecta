package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;

public interface RepositorioAnimales {
    ResultadoRegistroAnimal registrar(Animal animal, OperacionId operacionId,
            UUID actorUsuarioId, byte[] huellaSha256, int schemaVersion);
}
