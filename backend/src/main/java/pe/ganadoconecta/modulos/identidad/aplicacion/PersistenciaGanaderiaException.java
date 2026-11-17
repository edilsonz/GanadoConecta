package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Fallo técnico genérico sin detalles de PostgreSQL ni de otra ganadería. */
public final class PersistenciaGanaderiaException extends RuntimeException {
    public PersistenciaGanaderiaException(Throwable cause) {
        super("No fue posible guardar la ganadería.", cause);
    }
}
