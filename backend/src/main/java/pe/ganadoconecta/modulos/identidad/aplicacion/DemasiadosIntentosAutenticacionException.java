package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class DemasiadosIntentosAutenticacionException extends RuntimeException {
    public DemasiadosIntentosAutenticacionException() {
        super("Límite temporal de autenticación alcanzado");
    }
}
