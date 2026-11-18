package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

class AutorizarAccesoGanaderiaTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GANADERIA = UUID.randomUUID();

    @Test
    void propietarioYEditorPuedenEscribir() {
        for (String rol : List.of("PROPIETARIO", "EDITOR")) {
            var autorizador = new AutorizarAccesoGanaderia(repositorio(rol));
            assertThat(autorizador.autorizar(
                    ACTOR, GANADERIA, PermisoGanaderia.ESCRIBIR).rol()).isEqualTo(rol);
        }
    }

    @Test
    void lectorSoloPuedeLeer() {
        var autorizador = new AutorizarAccesoGanaderia(repositorio("LECTOR"));

        assertThat(autorizador.autorizar(
                ACTOR, GANADERIA, PermisoGanaderia.LEER).rol()).isEqualTo("LECTOR");
        assertThatThrownBy(() -> autorizador.autorizar(
                ACTOR, GANADERIA, PermisoGanaderia.ESCRIBIR))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
    }

    @Test
    void soloPropietarioPuedeAdministrar() {
        assertThat(new AutorizarAccesoGanaderia(repositorio("PROPIETARIO"))
                .autorizar(ACTOR, GANADERIA, PermisoGanaderia.ADMINISTRAR).rol())
                .isEqualTo("PROPIETARIO");
        for (String rol : List.of("EDITOR", "LECTOR")) {
            assertThatThrownBy(() -> new AutorizarAccesoGanaderia(repositorio(rol))
                    .autorizar(ACTOR, GANADERIA, PermisoGanaderia.ADMINISTRAR))
                    .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        }
    }

    @Test
    void rechazaMembresiaAusenteORolNoReconocido() {
        assertThatThrownBy(() -> new AutorizarAccesoGanaderia(repositorio(null))
                .autorizar(ACTOR, GANADERIA, PermisoGanaderia.LEER))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThatThrownBy(() -> new AutorizarAccesoGanaderia(repositorio("DESCONOCIDO"))
                .autorizar(ACTOR, GANADERIA, PermisoGanaderia.LEER))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
    }

    private RepositorioGanaderias repositorio(String rol) {
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
                return rol == null ? Optional.empty() : Optional.of(
                        new GanaderiaAccesible(GANADERIA, "Los Pinos", "ACTIVA", rol));
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
