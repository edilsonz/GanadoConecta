package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Conflicto seguro: no transporta datos de la cuenta existente. */
public final class CorreoNoDisponibleException extends RuntimeException {
    public CorreoNoDisponibleException() {
        super("El registro no está disponible.");
    }
}
