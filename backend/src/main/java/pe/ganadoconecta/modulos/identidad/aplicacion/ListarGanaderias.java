package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;

/** AU06 parcial: lista únicamente membresías activas del usuario autenticado. */
public final class ListarGanaderias {
    private final RepositorioGanaderias repositorio;

    public ListarGanaderias(RepositorioGanaderias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    public List<GanaderiaAccesible> ejecutar(UUID actorUsuarioId) {
        return List.copyOf(repositorio.listarActivas(
                Objects.requireNonNull(actorUsuarioId)));
    }
}
