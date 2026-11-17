package pe.ganadoconecta.modulos.identidad.dominio;

/** Error de entrada seguro para AU05. */
public final class DatoGanaderiaInvalidoException extends RuntimeException {
    private final String campo;

    public DatoGanaderiaInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
