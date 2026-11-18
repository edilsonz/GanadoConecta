package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class PropietarioNoRevocableException extends RuntimeException {
    public PropietarioNoRevocableException() {
        super("El propietario no puede revocarse mediante esta operación");
    }
}
