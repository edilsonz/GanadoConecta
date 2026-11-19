package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Instant;
import java.util.Objects;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;

public record CursorListadoAnimales(Instant creadoEn, AnimalId animalId) {
    public CursorListadoAnimales {
        Objects.requireNonNull(creadoEn);
        Objects.requireNonNull(animalId);
    }
}
