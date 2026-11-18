package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

class ConsultarAccesoGanaderiaTest {
    private static final Instant AHORA = Instant.parse("2026-11-18T02:30:00Z");
    private static final UUID ACTOR = UUID.fromString(
            "123e4567-e89b-42d3-a456-426614174001");
    private static final UUID GANADERIA = UUID.fromString(
            "123e4567-e89b-42d3-a456-426614174002");

    @Test
    void devuelveContextoVigenteConHoraUtcDelServidor() {
        GanaderiaAccesible acceso = new GanaderiaAccesible(
                GANADERIA, "Los Pinos", "ACTIVA", "EDITOR");
        ConsultarAccesoGanaderia caso = new ConsultarAccesoGanaderia(
                new AutorizarAccesoGanaderia(repositorio(Optional.of(acceso))),
                Clock.fixed(AHORA, ZoneOffset.UTC));

        var resultado = caso.ejecutar(ACTOR, GANADERIA);

        assertThat(resultado.ganaderiaId()).isEqualTo(GANADERIA);
        assertThat(resultado.rol()).isEqualTo("EDITOR");
        assertThat(resultado.estado()).isEqualTo("ACTIVA");
        assertThat(resultado.validadoEn()).isEqualTo(AHORA);
    }

    @Test
    void respondeIgualCuandoNoExisteAccesoActivo() {
        ConsultarAccesoGanaderia caso = new ConsultarAccesoGanaderia(
                new AutorizarAccesoGanaderia(repositorio(Optional.empty())),
                Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> caso.ejecutar(ACTOR, GANADERIA))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class)
                .hasMessage("No tiene acceso a la ganadería solicitada");
    }

    private RepositorioGanaderias repositorio(Optional<GanaderiaAccesible> acceso) {
        return new RepositorioGanaderias() {
            @Override
            public ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId) {
                throw new AssertionError("No debe crear");
            }

            @Override
            public List<GanaderiaAccesible> listarActivas(UUID usuarioId) {
                throw new AssertionError("No debe listar");
            }

            @Override
            public Optional<GanaderiaAccesible> buscarActiva(
                    UUID usuarioId, UUID ganaderiaId) {
                assertThat(usuarioId).isEqualTo(ACTOR);
                assertThat(ganaderiaId).isEqualTo(GANADERIA);
                return acceso;
            }

            @Override
            public ResultadoRevocacion revocarMembresia(UUID actorUsuarioId,
                    UUID actorSesionId, UUID ganaderiaId,
                    UUID objetivoUsuarioId, Instant revocadoEn) {
                throw new AssertionError("No debe revocar");
            }
        };
    }
}
