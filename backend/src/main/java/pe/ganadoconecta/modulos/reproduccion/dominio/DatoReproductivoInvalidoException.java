package pe.ganadoconecta.modulos.reproduccion.dominio;
public final class DatoReproductivoInvalidoException extends RuntimeException {
    private final String campo;
    public DatoReproductivoInvalidoException(String campo){super("Dato reproductivo inválido");this.campo=campo;}
    public String campo(){return campo;}
}
