package pe.ganadoconecta.modulos.ganado.dominio;

import java.util.Objects;
import java.util.UUID;

public record OperacionId(UUID valor) {
    public OperacionId {
        Objects.requireNonNull(valor);
        if (valor.version() != 4 || valor.variant() != 2) {
            throw new DatoAnimalInvalidoException("operationId");
        }
    }
}
