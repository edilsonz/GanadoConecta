package pe.ganadoconecta.modulos.identidad.dominio;

public final class DatoRegistroInvalidoException extends RuntimeException {
    private final String campo;

    public DatoRegistroInvalidoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
