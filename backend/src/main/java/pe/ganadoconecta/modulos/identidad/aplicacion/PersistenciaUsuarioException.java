package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class PersistenciaUsuarioException extends RuntimeException {
    public PersistenciaUsuarioException(Throwable cause) {
        super("No fue posible guardar la cuenta.", cause);
    }
}
