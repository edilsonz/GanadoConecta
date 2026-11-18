package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.function.Supplier;

public interface UnidadDeTrabajo {
    <T> T ejecutar(Supplier<T> trabajo);
}
