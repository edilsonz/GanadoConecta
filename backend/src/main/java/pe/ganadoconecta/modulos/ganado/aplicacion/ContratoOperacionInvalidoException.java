package pe.ganadoconecta.modulos.ganado.aplicacion;

public final class ContratoOperacionInvalidoException extends RuntimeException {
    private final String campo;

    public ContratoOperacionInvalidoException(String campo) {
        super("Contrato de operación inválido: " + campo);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
