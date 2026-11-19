package pe.ganadoconecta.modulos.ganado.aplicacion;

public final class ConsultaAnimalesInvalidaException extends RuntimeException {
    public ConsultaAnimalesInvalidaException() {
        super("Los parámetros de consulta no son válidos");
    }
}
