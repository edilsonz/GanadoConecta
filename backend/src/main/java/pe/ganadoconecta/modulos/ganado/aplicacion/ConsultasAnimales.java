package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.List;
import java.util.Optional;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;

public interface ConsultasAnimales {
    List<Animal> listar(GanaderiaId ganaderiaId, int cantidad,
            CursorListadoAnimales cursor);

    Optional<Animal> buscar(GanaderiaId ganaderiaId, AnimalId animalId);
}
