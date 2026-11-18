package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;

/** Revalida membresía y rol en la fuente de verdad para cada operación privada. */
public final class AutorizarAccesoGanaderia {
    private final RepositorioGanaderias repositorio;

    public AutorizarAccesoGanaderia(RepositorioGanaderias repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio);
    }

    public GanaderiaAccesible autorizar(UUID actorUsuarioId, UUID ganaderiaId,
            PermisoGanaderia permiso) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(permiso);
        GanaderiaAccesible acceso = repositorio.buscarActiva(actorUsuarioId, ganaderiaId)
                .orElseThrow(AccesoGanaderiaDenegadoException::new);
        if (!permiso.permite(acceso.rol())) {
            throw new AccesoGanaderiaDenegadoException();
        }
        return acceso;
    }
}
