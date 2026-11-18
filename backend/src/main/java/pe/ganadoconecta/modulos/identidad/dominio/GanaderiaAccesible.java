package pe.ganadoconecta.modulos.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/** Vista mínima de una ganadería autorizada por membresía activa. */
public record GanaderiaAccesible(UUID id, String nombre, String estado, String rol) {
    public GanaderiaAccesible {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(estado);
        Objects.requireNonNull(rol);
    }
}
