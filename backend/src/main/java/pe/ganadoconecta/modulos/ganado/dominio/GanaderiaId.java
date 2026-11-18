package pe.ganadoconecta.modulos.ganado.dominio;

import java.util.Objects;
import java.util.UUID;

public record GanaderiaId(UUID valor) {
    public GanaderiaId {
        Objects.requireNonNull(valor);
    }
}
