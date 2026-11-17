package pe.ganadoconecta.modulos.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/** Proyección privada para autenticar; nunca debe salir por HTTP ni logging. */
public record UsuarioAutenticable(
        UUID id, CorreoElectronico correo, String passwordHash, Estado estado) {
    public UsuarioAutenticable {
        Objects.requireNonNull(id);
        Objects.requireNonNull(correo);
        Objects.requireNonNull(passwordHash);
        Objects.requireNonNull(estado);
    }

    public boolean activo() {
        return estado == Estado.ACTIVO;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticable[id=" + id + ", datos=PROTEGIDOS]";
    }

    public enum Estado {
        ACTIVO,
        INACTIVO
    }
}
