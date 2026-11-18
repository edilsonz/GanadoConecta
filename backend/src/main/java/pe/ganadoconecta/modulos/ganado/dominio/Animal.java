package pe.ganadoconecta.modulos.ganado.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Animal(
        AnimalId id,
        GanaderiaId ganaderiaId,
        DatosAnimal datos,
        EstadoAnimal estado,
        long version,
        UUID creadoPor,
        Instant creadoEn) {

    public Animal {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(datos);
        Objects.requireNonNull(estado);
        Objects.requireNonNull(creadoPor);
        Objects.requireNonNull(creadoEn);
        if (version != 1) {
            throw new IllegalArgumentException("La versión inicial debe ser 1");
        }
    }
}
