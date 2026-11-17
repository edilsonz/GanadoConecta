package pe.ganadoconecta.modulos.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/** Datos de la ganadería y propietario obtenidos por el caso de uso. */
public record GanaderiaNueva(UUID id, NombreGanaderia nombre, UUID propietarioUsuarioId) {
    public GanaderiaNueva {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(propietarioUsuarioId);
    }
}
