package pe.ganadoconecta.modulos.ganado.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.EstadoAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;

class ConsultarAnimalesTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final GanaderiaId GANADERIA = new GanaderiaId(UUID.randomUUID());
    private static final Instant AHORA = Instant.parse("2026-11-18T20:00:00Z");

    @Test
    void paginaConOrdenDelRepositorioYCursorDelUltimoItemVisible() {
        Animal primero = animal(AHORA.minusSeconds(1));
        Animal segundo = animal(AHORA.minusSeconds(2));
        Animal extra = animal(AHORA.minusSeconds(3));
        AtomicInteger autorizaciones = new AtomicInteger();
        ConsultasFalsas consultas = new ConsultasFalsas(List.of(primero, segundo, extra));
        ListarAnimales caso = new ListarAnimales(
                (actor, ganaderia) -> autorizaciones.incrementAndGet(),
                consultas, Clock.fixed(AHORA, ZoneOffset.UTC));

        PaginaAnimales pagina = caso.ejecutar(ACTOR, GANADERIA, 2, null);

        assertThat(pagina.items()).containsExactly(primero, segundo);
        assertThat(pagina.siguienteCursor()).isEqualTo(
                new CursorListadoAnimales(segundo.creadoEn(), segundo.id()));
        assertThat(pagina.consultadoEn()).isEqualTo(AHORA);
        assertThat(consultas.cantidadSolicitada).isEqualTo(3);
        assertThat(autorizaciones).hasValue(1);
    }

    @Test
    void ultimaPaginaYListaVaciaNoCreanCursor() {
        Animal unico = animal(AHORA.minusSeconds(1));
        ListarAnimales conDato = caso(new ConsultasFalsas(List.of(unico)));
        ListarAnimales vacio = caso(new ConsultasFalsas(List.of()));

        assertThat(conDato.ejecutar(ACTOR, GANADERIA, 20, null).siguienteCursor())
                .isNull();
        assertThat(vacio.ejecutar(ACTOR, GANADERIA, 20, null).items()).isEmpty();
    }

    @Test
    void rechazaLimiteFueraDeContratoAntesDeConsultar() {
        ConsultasFalsas consultas = new ConsultasFalsas(List.of());
        ListarAnimales caso = caso(consultas);

        assertThatThrownBy(() -> caso.ejecutar(ACTOR, GANADERIA, 0, null))
                .isInstanceOf(ConsultaAnimalesInvalidaException.class);
        assertThatThrownBy(() -> caso.ejecutar(ACTOR, GANADERIA, 101, null))
                .isInstanceOf(ConsultaAnimalesInvalidaException.class);
        assertThat(consultas.invocaciones).hasValue(0);
    }

    @Test
    void fichaAutorizaAntesDeBuscarYNoDistingueAusencia() {
        Animal animal = animal(AHORA.minusSeconds(1));
        ConsultasFalsas consultas = new ConsultasFalsas(List.of(animal));
        ConsultarFichaAnimal caso = new ConsultarFichaAnimal(
                (actor, ganaderia) -> { }, consultas);

        assertThat(caso.ejecutar(ACTOR, GANADERIA, animal.id())).isEqualTo(animal);
        assertThatThrownBy(() -> caso.ejecutar(
                ACTOR, GANADERIA, new AnimalId(UUID.randomUUID())))
                .isInstanceOf(AnimalNoEncontradoException.class);

        ConsultarFichaAnimal denegado = new ConsultarFichaAnimal(
                (actor, ganaderia) -> {
                    throw new AccesoGanaderiaDenegadoException();
                }, consultas);
        int invocacionesAntes = consultas.invocaciones.get();
        assertThatThrownBy(() -> denegado.ejecutar(ACTOR, GANADERIA, animal.id()))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThat(consultas.invocaciones).hasValue(invocacionesAntes);
    }

    private ListarAnimales caso(ConsultasAnimales consultas) {
        return new ListarAnimales((actor, ganaderia) -> { }, consultas,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Animal animal(Instant creadoEn) {
        return new Animal(new AnimalId(UUID.randomUUID()), GANADERIA,
                new DatosAnimal(Sexo.HEMBRA, null, null, null,
                        null, null, null, null, null, null, null),
                EstadoAnimal.ACTIVO, 1, ACTOR, creadoEn);
    }

    private static final class ConsultasFalsas implements ConsultasAnimales {
        private final List<Animal> animales;
        private final AtomicInteger invocaciones = new AtomicInteger();
        private int cantidadSolicitada;

        private ConsultasFalsas(List<Animal> animales) {
            this.animales = animales;
        }

        @Override
        public List<Animal> listar(GanaderiaId ganaderiaId, int cantidad,
                CursorListadoAnimales cursor) {
            invocaciones.incrementAndGet();
            cantidadSolicitada = cantidad;
            return animales;
        }

        @Override
        public Optional<Animal> buscar(GanaderiaId ganaderiaId, AnimalId animalId) {
            invocaciones.incrementAndGet();
            return animales.stream().filter(animal -> animal.id().equals(animalId))
                    .findFirst();
        }
    }
}
