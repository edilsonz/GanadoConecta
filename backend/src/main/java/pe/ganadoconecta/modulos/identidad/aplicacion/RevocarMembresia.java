package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** AU07: revoca una membresía existente sin eliminar su historial. */
public final class RevocarMembresia {
    private final RepositorioGanaderias repositorio;
    private final Clock reloj;

    public RevocarMembresia(RepositorioGanaderias repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public void ejecutar(UUID actorUsuarioId, UUID actorSesionId,
            UUID ganaderiaId, UUID objetivoUsuarioId) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(actorSesionId);
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(objetivoUsuarioId);
        switch (repositorio.revocarMembresia(actorUsuarioId, actorSesionId, ganaderiaId,
                objetivoUsuarioId, reloj.instant())) {
            case REVOCADA, YA_REVOCADA -> {
                return;
            }
            case ACTOR_NO_AUTORIZADO -> throw new AccesoGanaderiaDenegadoException();
            case OBJETIVO_NO_ENCONTRADO -> throw new MembresiaNoEncontradaException();
            case OBJETIVO_PROPIETARIO -> throw new PropietarioNoRevocableException();
        }
    }
}
