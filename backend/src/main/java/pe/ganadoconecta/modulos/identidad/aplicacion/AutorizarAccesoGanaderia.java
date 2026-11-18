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
        if (!permite(acceso.rol(), permiso)) {
            throw new AccesoGanaderiaDenegadoException();
        }
        return acceso;
    }

    private boolean permite(String rol, PermisoGanaderia permiso) {
        return switch (permiso) {
            case LEER -> "PROPIETARIO".equals(rol)
                    || "EDITOR".equals(rol)
                    || "LECTOR".equals(rol);
            case ESCRIBIR -> "PROPIETARIO".equals(rol) || "EDITOR".equals(rol);
        };
    }
}
