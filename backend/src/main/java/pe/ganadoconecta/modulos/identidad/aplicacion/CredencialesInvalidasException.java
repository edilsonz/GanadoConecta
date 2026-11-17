package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Error deliberadamente indistinguible para cuenta ausente, inactiva o clave incorrecta. */
public final class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Las credenciales no son válidas");
    }
}
