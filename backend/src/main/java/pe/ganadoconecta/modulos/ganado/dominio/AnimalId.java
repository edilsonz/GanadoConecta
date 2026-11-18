package pe.ganadoconecta.modulos.ganado.dominio;

import java.util.Objects;
import java.util.UUID;

public record AnimalId(UUID valor) {
    public AnimalId {
        Objects.requireNonNull(valor);
        if (valor.version() != 4 || valor.variant() != 2) {
            throw new DatoAnimalInvalidoException("animalId");
        }
    }
}
