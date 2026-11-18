package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Respuesta indistinguible para una ganadería inexistente, ajena o no vigente. */
public final class AccesoGanaderiaDenegadoException extends RuntimeException {
    private final Motivo motivo;

    public AccesoGanaderiaDenegadoException() {
        this(Motivo.ACCESO_DENEGADO);
    }

    public AccesoGanaderiaDenegadoException(Motivo motivo) {
        super("No tiene acceso a la ganadería solicitada");
        this.motivo = java.util.Objects.requireNonNull(motivo);
    }

    public Motivo motivo() {
        return motivo;
    }

    public enum Motivo {
        ACCESO_DENEGADO,
        MEMBRESIA_REVOCADA,
        ROL_INSUFICIENTE
    }
}
