package pe.ganadoconecta.modulos.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/** Datos ya validados y codificados que pueden cruzar hacia persistencia. */
public record UsuarioNuevo(UUID id, CorreoElectronico correo, String passwordHash) {
    public UsuarioNuevo {
        Objects.requireNonNull(id);
        Objects.requireNonNull(correo);
        Objects.requireNonNull(passwordHash);
    }
}
