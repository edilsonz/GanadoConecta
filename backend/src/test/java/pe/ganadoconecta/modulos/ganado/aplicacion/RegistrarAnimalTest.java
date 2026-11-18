package pe.ganadoconecta.modulos.ganado.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;

class RegistrarAnimalTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GANADERIA = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-18T15:00:00Z");

    @Test
    void validaAutorizaYPersisteDentroDeUnidadDeTrabajo() {
        AtomicBoolean transaccion = new AtomicBoolean();
        AtomicBoolean autorizado = new AtomicBoolean();
        AtomicBoolean persistido = new AtomicBoolean();
        SolicitudRegistroAnimal solicitud = solicitud(LocalDate.of(2026, 11, 18));

        RegistrarAnimal caso = new RegistrarAnimal(unidadDeTrabajo(transaccion),
                (usuario, sesion, ganaderia, permiso, ahora) -> {
            assertThat(transaccion).isTrue();
            autorizado.set(true);
            return new GanaderiaAccesible(ganaderia, "Los Pinos", "ACTIVA", "EDITOR");
        }, (animal, operacion, actor, huella) -> {
            assertThat(transaccion).isTrue();
            assertThat(autorizado).isTrue();
            assertThat(huella).hasSize(32);
            persistido.set(true);
            return new ResultadoRegistroAnimal(animal.id(), operacion, 1, AHORA,
                    ResultadoRegistroAnimal.EstadoResultado.APLICADO, false);
        }, new CanonizadorAltaAnimal(), Clock.fixed(AHORA, ZoneOffset.UTC));

        ResultadoRegistroAnimal resultado = caso.ejecutar(solicitud, ACTOR, SESION);

        assertThat(resultado.estado()).isEqualTo(
                ResultadoRegistroAnimal.EstadoResultado.APLICADO);
        assertThat(persistido).isTrue();
    }

    @Test
    void noPersisteCuandoLaAutorizacionEsDenegada() {
        RegistrarAnimal caso = new RegistrarAnimal(unidadDeTrabajo(new AtomicBoolean()),
                (usuario, sesion, ganaderia, permiso, ahora) -> {
                    throw new AccesoGanaderiaDenegadoException();
                }, (animal, operacion, actor, huella) -> {
                    throw new AssertionError("No debe persistir");
                }, new CanonizadorAltaAnimal(), Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> caso.ejecutar(solicitud(null), ACTOR, SESION))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
    }

    @Test
    void fechaFuturaFallaAntesDeAbrirLaTransaccion() {
        AtomicBoolean invocada = new AtomicBoolean();
        RegistrarAnimal caso = new RegistrarAnimal(new UnidadDeTrabajo() {
            @Override
            public <T> T ejecutar(Supplier<T> trabajo) {
                invocada.set(true);
                return trabajo.get();
            }
        }, (usuario, sesion, ganaderia, permiso, ahora) -> null,
                (animal, operacion, actor, huella) -> null,
                new CanonizadorAltaAnimal(), Clock.fixed(AHORA, ZoneOffset.UTC));

        assertThatThrownBy(() -> caso.ejecutar(solicitud(LocalDate.of(2026, 11, 19)),
                ACTOR, SESION)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fechaNacimiento");
        assertThat(invocada).isFalse();
    }

    @Test
    void exigeContratoVersionadoEIdentificadoresDiferentes() {
        AnimalId animal = new AnimalId(UUID.randomUUID());
        DatosAnimal datos = new DatosAnimal(Sexo.MACHO, null, null, null, null,
                null, null, null, null);
        assertThatThrownBy(() -> new SolicitudRegistroAnimal(
                new GanaderiaId(GANADERIA), animal, new OperacionId(UUID.randomUUID()),
                "OTRA", 1, 0, datos))
                .isInstanceOf(ContratoOperacionInvalidoException.class)
                .hasMessageContaining("tipo");
        assertThatThrownBy(() -> new SolicitudRegistroAnimal(
                new GanaderiaId(GANADERIA), animal, new OperacionId(UUID.randomUUID()),
                "CREAR_ANIMAL", 2, 0, datos))
                .isInstanceOf(ContratoOperacionInvalidoException.class)
                .hasMessageContaining("schemaVersion");
        assertThatThrownBy(() -> new SolicitudRegistroAnimal(
                new GanaderiaId(GANADERIA), animal, new OperacionId(animal.valor()),
                "CREAR_ANIMAL", 1, 0, datos))
                .isInstanceOf(ContratoOperacionInvalidoException.class)
                .hasMessageContaining("operationId");
    }

    private SolicitudRegistroAnimal solicitud(LocalDate fecha) {
        return new SolicitudRegistroAnimal(new GanaderiaId(GANADERIA),
                new AnimalId(UUID.randomUUID()), new OperacionId(UUID.randomUUID()),
                "CREAR_ANIMAL", 1, 0,
                new DatosAnimal(Sexo.HEMBRA, "Luna", null, null, null,
                        fecha, fecha == null ? null : false, null, null));
    }

    private UnidadDeTrabajo unidadDeTrabajo(AtomicBoolean transaccion) {
        return new UnidadDeTrabajo() {
            @Override
            public <T> T ejecutar(Supplier<T> trabajo) {
                transaccion.set(true);
                try {
                    return trabajo.get();
                } finally {
                    transaccion.set(false);
                }
            }
        };
    }
}
