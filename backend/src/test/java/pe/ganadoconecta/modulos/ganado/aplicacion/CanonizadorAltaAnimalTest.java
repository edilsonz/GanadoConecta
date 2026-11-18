package pe.ganadoconecta.modulos.ganado.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;

class CanonizadorAltaAnimalTest {
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GANADERIA = UUID.randomUUID();
    private static final UUID ANIMAL = UUID.randomUUID();

    @Test
    void produceHuellaEstableParaDatosNormalizados() {
        CanonizadorAltaAnimal canonizador = new CanonizadorAltaAnimal();
        byte[] primera = canonizador.calcular(solicitud("  Luna  "), ACTOR);
        byte[] segunda = canonizador.calcular(solicitud("Luna"), ACTOR);

        assertThat(primera).hasSize(32).isEqualTo(segunda);
    }

    @Test
    void vinculaLaHuellaAlActorGanaderiaAnimalYContenido() {
        CanonizadorAltaAnimal canonizador = new CanonizadorAltaAnimal();
        byte[] original = canonizador.calcular(solicitud("Luna"), ACTOR);

        assertThat(canonizador.calcular(solicitud("Sol"), ACTOR))
                .isNotEqualTo(original);
        assertThat(canonizador.calcular(solicitud("Luna"), UUID.randomUUID()))
                .isNotEqualTo(original);
    }

    private SolicitudRegistroAnimal solicitud(String nombre) {
        return new SolicitudRegistroAnimal(new GanaderiaId(GANADERIA),
                new AnimalId(ANIMAL), new OperacionId(UUID.randomUUID()),
                "CREAR_ANIMAL", 1, 0,
                new DatosAnimal(Sexo.HEMBRA, nombre, null, null, null,
                        null, null, null, null));
    }
}
