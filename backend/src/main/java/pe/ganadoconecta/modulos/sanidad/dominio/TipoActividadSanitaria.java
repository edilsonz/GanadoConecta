package pe.ganadoconecta.modulos.sanidad.dominio;
public enum TipoActividadSanitaria {
    VACUNACION, TRATAMIENTO_SANITARIO, REVISION_VETERINARIA, REPRODUCTIVA, PRODUCTIVA,
    CAMPANA_VACUNACION, CAMPANA_DESPARASITACION, CAMPANA_BANO;

    public TipoEventoSanitario eventoDeCampana() {
        return switch (this) {
            case CAMPANA_VACUNACION -> TipoEventoSanitario.VACUNACION;
            case CAMPANA_DESPARASITACION -> TipoEventoSanitario.DESPARASITACION;
            case CAMPANA_BANO -> TipoEventoSanitario.BANO_ANTIPARASITARIO;
            default -> null;
        };
    }
}
