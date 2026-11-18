package pe.ganadoconecta.modulos.ganado.aplicacion;

public final class PersistenciaAnimalException extends RuntimeException {
    public PersistenciaAnimalException(Throwable causa) {
        super("No fue posible persistir el animal", causa);
    }
}
