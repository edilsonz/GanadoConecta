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

class RevocarMembresiaTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GANADERIA = UUID.randomUUID();
    private static final UUID OBJETIVO = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-17T18:00:00Z");

    @Test
    void aceptaRevocacionNuevaOIdempotente() {
        for (RepositorioGanaderias.ResultadoRevocacion resultado : List.of(
                RepositorioGanaderias.ResultadoRevocacion.REVOCADA,
                RepositorioGanaderias.ResultadoRevocacion.YA_REVOCADA)) {
            new RevocarMembresia(repositorio(resultado), reloj())
                    .ejecutar(ACTOR, SESION, GANADERIA, OBJETIVO);
        }
    }

    @Test
    void traduceResultadosSinFiltrarDatos() {
        assertThatThrownBy(() -> ejecutar(
                RepositorioGanaderias.ResultadoRevocacion.ACTOR_NO_AUTORIZADO))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThatThrownBy(() -> ejecutar(
                RepositorioGanaderias.ResultadoRevocacion.OBJETIVO_NO_ENCONTRADO))
                .isInstanceOf(MembresiaNoEncontradaException.class);
        assertThatThrownBy(() -> ejecutar(
                RepositorioGanaderias.ResultadoRevocacion.OBJETIVO_PROPIETARIO))
                .isInstanceOf(PropietarioNoRevocableException.class);
    }

    private void ejecutar(RepositorioGanaderias.ResultadoRevocacion resultado) {
        new RevocarMembresia(repositorio(resultado), reloj())
                .ejecutar(ACTOR, SESION, GANADERIA, OBJETIVO);
    }

    private Clock reloj() {
        return Clock.fixed(AHORA, ZoneOffset.UTC);
    }

    private RepositorioGanaderias repositorio(
            RepositorioGanaderias.ResultadoRevocacion resultado) {
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
                throw new AssertionError("No debe consultar");
            }

            @Override
            public ResultadoRevocacion revocarMembresia(UUID actorUsuarioId,
                    UUID actorSesionId, UUID ganaderiaId,
                    UUID objetivoUsuarioId, Instant revocadoEn) {
                assertThat(actorUsuarioId).isEqualTo(ACTOR);
                assertThat(actorSesionId).isEqualTo(SESION);
                assertThat(ganaderiaId).isEqualTo(GANADERIA);
                assertThat(objetivoUsuarioId).isEqualTo(OBJETIVO);
                assertThat(revocadoEn).isEqualTo(AHORA);
                return resultado;
            }
        };
    }
}
