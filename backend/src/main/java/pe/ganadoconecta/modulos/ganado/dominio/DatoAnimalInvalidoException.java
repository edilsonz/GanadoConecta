package pe.ganadoconecta.modulos.ganado.dominio;

public final class DatoAnimalInvalidoException extends RuntimeException {
    private final String campo;

    public DatoAnimalInvalidoException(String campo) {
        super("Dato de animal inválido: " + campo);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
