package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Fallo técnico genérico; no transporta SQL, credenciales ni datos de cuenta. */
public final class PersistenciaIdentidadException extends RuntimeException {
    public PersistenciaIdentidadException(Throwable cause) {
        super("No fue posible completar la operación de identidad.", cause);
    }
}
