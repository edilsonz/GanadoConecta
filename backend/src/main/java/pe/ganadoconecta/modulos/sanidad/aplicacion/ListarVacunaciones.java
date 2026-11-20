package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.sanidad.dominio.Vacunacion;

public final class ListarVacunaciones {
    private final AutorizarLecturaGanaderia autorizador;
    private final RepositorioVacunaciones repositorio;

    public ListarVacunaciones(AutorizarLecturaGanaderia autorizador,
            RepositorioVacunaciones repositorio) {
        this.autorizador = Objects.requireNonNull(autorizador);
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    public List<Vacunacion> ejecutar(UUID actor, UUID ganaderiaId, UUID animalId) {
        autorizador.autorizar(actor, ganaderiaId);
        return repositorio.listar(ganaderiaId, animalId);
    }
}
