package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Respuesta indistinguible para una ganadería inexistente, ajena o no vigente. */
public final class AccesoGanaderiaDenegadoException extends RuntimeException {
    public AccesoGanaderiaDenegadoException() {
        super("No tiene acceso a la ganadería solicitada");
    }
}
