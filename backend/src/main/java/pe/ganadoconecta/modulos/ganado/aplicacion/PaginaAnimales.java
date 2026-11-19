package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;

public record PaginaAnimales(
        List<Animal> items,
        CursorListadoAnimales siguienteCursor,
        Instant consultadoEn) {
    public PaginaAnimales {
        items = List.copyOf(items);
        Objects.requireNonNull(consultadoEn);
    }
}
