package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class TokenAccesoInvalidoException extends RuntimeException {
    public TokenAccesoInvalidoException() {
        super("El token de acceso no es válido");
    }

    public TokenAccesoInvalidoException(Throwable cause) {
        super("El token de acceso no es válido", cause);
    }
}
