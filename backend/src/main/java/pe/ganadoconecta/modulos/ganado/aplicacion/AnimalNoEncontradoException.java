package pe.ganadoconecta.modulos.ganado.aplicacion;

public final class AnimalNoEncontradoException extends RuntimeException {
    public AnimalNoEncontradoException() {
        super("No se encontró el animal solicitado");
    }
}
