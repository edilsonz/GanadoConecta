package pe.ganadoconecta.modulos.ganado.aplicacion;

/** Conflicto genérico: no revela si colisionó el animal o la operación. */
public final class ConflictoRegistroAnimalException extends RuntimeException {
    public ConflictoRegistroAnimalException() {
        super("La operación entra en conflicto con un registro existente");
    }
}
