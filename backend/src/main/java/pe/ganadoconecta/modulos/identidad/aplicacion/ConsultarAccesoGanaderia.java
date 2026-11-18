package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** AU06: revalida el contexto de una ganadería en la fuente de verdad. */
public final class ConsultarAccesoGanaderia {
    private final RepositorioGanaderias repositorio;
    private final Clock reloj;

    public ConsultarAccesoGanaderia(RepositorioGanaderias repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Resultado ejecutar(UUID actorUsuarioId, UUID ganaderiaId) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(ganaderiaId);
        var ganaderia = repositorio.buscarActiva(actorUsuarioId, ganaderiaId)
                .orElseThrow(AccesoGanaderiaDenegadoException::new);
        return new Resultado(ganaderia.id(), ganaderia.rol(), ganaderia.estado(),
                reloj.instant());
    }

    public record Resultado(UUID ganaderiaId, String rol, String estado,
            Instant validadoEn) {
    }
}
