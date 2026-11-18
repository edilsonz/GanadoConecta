package pe.ganadoconecta.modulos.identidad.aplicacion;

public final class MembresiaNoEncontradaException extends RuntimeException {
    public MembresiaNoEncontradaException() {
        super("No se encontró la membresía solicitada");
    }
}
