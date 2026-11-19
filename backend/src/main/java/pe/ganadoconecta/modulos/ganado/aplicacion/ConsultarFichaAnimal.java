package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;

public final class ConsultarFichaAnimal {
    private final AutorizarLecturaGanaderia autorizador;
    private final ConsultasAnimales consultas;

    public ConsultarFichaAnimal(AutorizarLecturaGanaderia autorizador,
            ConsultasAnimales consultas) {
        this.autorizador = Objects.requireNonNull(autorizador);
        this.consultas = Objects.requireNonNull(consultas);
    }

    public Animal ejecutar(UUID actorUsuarioId, GanaderiaId ganaderiaId,
            AnimalId animalId) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(animalId);
        autorizador.autorizar(actorUsuarioId, ganaderiaId.valor());
        return consultas.buscar(ganaderiaId, animalId)
                .orElseThrow(AnimalNoEncontradoException::new);
    }
}
