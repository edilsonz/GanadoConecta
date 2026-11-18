package pe.ganadoconecta.modulos.ganado.adaptadores.salida;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;

public final class UnidadDeTrabajoPostgres implements UnidadDeTrabajo {
    private final TransactionTemplate transacciones;

    public UnidadDeTrabajoPostgres(TransactionTemplate transacciones) {
        this.transacciones = Objects.requireNonNull(transacciones);
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return Objects.requireNonNull(transacciones.execute(estado -> trabajo.get()));
    }
}
