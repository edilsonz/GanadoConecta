package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

public interface RepositorioGanaderias {
    ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId);

    List<GanaderiaAccesible> listarActivas(UUID usuarioId);

    Optional<GanaderiaAccesible> buscarActiva(UUID usuarioId, UUID ganaderiaId);

    ResultadoRevocacion revocarMembresia(UUID actorUsuarioId, UUID actorSesionId,
            UUID ganaderiaId, UUID objetivoUsuarioId, Instant revocadoEn);

    enum ResultadoCreacion {
        CREADA,
        YA_EXISTIA
    }

    enum ResultadoRevocacion {
        REVOCADA,
        YA_REVOCADA,
        ACTOR_NO_AUTORIZADO,
        OBJETIVO_NO_ENCONTRADO,
        OBJETIVO_PROPIETARIO
    }
}
