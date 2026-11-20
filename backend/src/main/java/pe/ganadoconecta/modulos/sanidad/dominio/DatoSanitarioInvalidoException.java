package pe.ganadoconecta.modulos.sanidad.dominio;

public final class DatoSanitarioInvalidoException extends RuntimeException {
    private final String campo;

    public DatoSanitarioInvalidoException(String campo) {
        super("Dato sanitario inválido: " + campo);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
