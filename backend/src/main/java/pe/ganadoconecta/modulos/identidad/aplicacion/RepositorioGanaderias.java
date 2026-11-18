package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

public interface RepositorioGanaderias {
    ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId);

    List<GanaderiaAccesible> listarActivas(UUID usuarioId);

    Optional<GanaderiaAccesible> buscarActiva(UUID usuarioId, UUID ganaderiaId);

    enum ResultadoCreacion {
        CREADA,
        YA_EXISTIA
    }
}
