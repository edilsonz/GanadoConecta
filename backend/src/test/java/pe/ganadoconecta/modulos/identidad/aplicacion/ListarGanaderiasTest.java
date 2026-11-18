package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

class ListarGanaderiasTest {
    @Test
    void consultaSoloConLaIdentidadAutenticada() {
        UUID actor = UUID.randomUUID();
        GanaderiaAccesible accesible = new GanaderiaAccesible(
                UUID.randomUUID(), "Los Pinos", "ACTIVA", "LECTOR");
        RepositorioGanaderias repositorio = new RepositorioGanaderias() {
            @Override
            public ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId) {
                throw new AssertionError("No debe crear");
            }

            @Override
            public List<GanaderiaAccesible> listarActivas(UUID usuarioId) {
                assertThat(usuarioId).isEqualTo(actor);
                return List.of(accesible);
            }
        };

        assertThat(new ListarGanaderias(repositorio).ejecutar(actor))
                .containsExactly(accesible);
    }
}
