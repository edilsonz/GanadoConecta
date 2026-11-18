package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.DatoGanaderiaInvalidoException;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;

class CrearGanaderiaTest {
    private static final UUID GANADERIA_ID = UUID.fromString(
            "123e4567-e89b-42d3-a456-426614174000");
    private static final UUID USUARIO_ID = UUID.fromString(
            "123e4567-e89b-42d3-a456-426614174001");
    private static final UUID MEMBRESIA_ID = UUID.fromString(
            "123e4567-e89b-42d3-a456-426614174002");

    @Test
    void normalizaNombreYUsaAlActorComoPropietario() {
        AtomicReference<GanaderiaNueva> guardada = new AtomicReference<>();
        RepositorioGanaderias repositorio = repositorio((ganaderia, membresiaId) -> {
            guardada.set(ganaderia);
            assertThat(membresiaId).isEqualTo(MEMBRESIA_ID);
            return RepositorioGanaderias.ResultadoCreacion.CREADA;
        });
        CrearGanaderia caso = new CrearGanaderia(repositorio, () -> MEMBRESIA_ID);

        CrearGanaderia.Resultado resultado = caso.ejecutar(new CrearGanaderia.Comando(
                GANADERIA_ID.toString(), "  Ganaderi\u0301a El Prado  ", USUARIO_ID));

        assertThat(resultado.creada()).isTrue();
        assertThat(resultado.nombre()).isEqualTo("Ganadería El Prado");
        assertThat(resultado.estado()).isEqualTo("ACTIVA");
        assertThat(resultado.rol()).isEqualTo("PROPIETARIO");
        assertThat(guardada.get().propietarioUsuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    void informaReplaySinCrearOtraAsociacion() {
        CrearGanaderia caso = new CrearGanaderia(repositorio(
                (ganaderia, membresiaId) -> RepositorioGanaderias.ResultadoCreacion.YA_EXISTIA),
                () -> MEMBRESIA_ID);

        CrearGanaderia.Resultado resultado = caso.ejecutar(new CrearGanaderia.Comando(
                GANADERIA_ID.toString(), "El Prado", USUARIO_ID));

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.id()).isEqualTo(GANADERIA_ID);
    }

    @Test
    void rechazaNombreVacioOLargoAntesDePersistir() {
        RepositorioGanaderias repositorio = repositorio((ganaderia, membresiaId) -> {
            throw new AssertionError("No debe invocar persistencia");
        });
        CrearGanaderia caso = new CrearGanaderia(repositorio, () -> MEMBRESIA_ID);

        assertThatThrownBy(() -> caso.ejecutar(new CrearGanaderia.Comando(
                GANADERIA_ID.toString(), "   ", USUARIO_ID)))
                .isInstanceOf(DatoGanaderiaInvalidoException.class)
                .extracting("campo").isEqualTo("nombre");
        assertThatThrownBy(() -> caso.ejecutar(new CrearGanaderia.Comando(
                GANADERIA_ID.toString(), "a".repeat(101), USUARIO_ID)))
                .isInstanceOf(DatoGanaderiaInvalidoException.class)
                .extracting("campo").isEqualTo("nombre");
    }

    @Test
    void rechazaIdentificadorQueNoEsUuidV4() {
        CrearGanaderia caso = new CrearGanaderia(repositorio((ganaderia, membresiaId) ->
                RepositorioGanaderias.ResultadoCreacion.CREADA), () -> MEMBRESIA_ID);

        assertThatThrownBy(() -> caso.ejecutar(new CrearGanaderia.Comando(
                "123e4567-e89b-12d3-a456-426614174000", "El Prado", USUARIO_ID)))
                .isInstanceOf(DatoGanaderiaInvalidoException.class)
                .extracting("campo").isEqualTo("id");
    }

    private RepositorioGanaderias repositorio(
            BiFunction<GanaderiaNueva, UUID, RepositorioGanaderias.ResultadoCreacion> crear) {
        return new RepositorioGanaderias() {
            @Override
            public ResultadoCreacion crear(GanaderiaNueva ganaderia, UUID membresiaId) {
                return crear.apply(ganaderia, membresiaId);
            }

            @Override
            public List<pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible>
                    listarActivas(UUID usuarioId) {
                return List.of();
            }

            @Override
            public Optional<pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible>
                    buscarActiva(UUID usuarioId, UUID ganaderiaId) {
                return Optional.empty();
            }
        };
    }
}
