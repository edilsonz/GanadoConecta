package pe.ganadoconecta.modulos.ganado.dominio;

public enum Categoria {
    TERNERA(Sexo.HEMBRA),
    VAQUILLA(Sexo.HEMBRA),
    VACA(Sexo.HEMBRA),
    TERNERO(Sexo.MACHO),
    TORETE(Sexo.MACHO),
    TORO(Sexo.MACHO);

    private final Sexo sexo;

    Categoria(Sexo sexo) {
        this.sexo = sexo;
    }

    public boolean correspondeA(Sexo valor) {
        return sexo == valor;
    }
}
