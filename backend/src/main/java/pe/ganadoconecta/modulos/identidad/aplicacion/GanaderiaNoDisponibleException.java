package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Conflicto seguro: el identificador no puede usarse con el contenido solicitado. */
public final class GanaderiaNoDisponibleException extends RuntimeException {
    public GanaderiaNoDisponibleException() {
        super("No fue posible crear la ganadería solicitada.");
    }
}
