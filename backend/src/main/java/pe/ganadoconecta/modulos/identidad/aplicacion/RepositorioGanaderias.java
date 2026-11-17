package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

public interface RepositorioGanaderias {
    ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId);

    enum ResultadoCreacion {
        CREADA,
        YA_EXISTIA
    }
}
