package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class SesionNoValidaException extends RuntimeException {
    public SesionNoValidaException() {
        super("La sesión no es válida");
    }
}
