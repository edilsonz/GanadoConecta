package pe.ganadoconecta.modulos.ganado.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.ganado.dominio.UbicacionIdentificacionTextil;

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

    @Test
    void conservaLaHuellaHistoricaV1YVersionaLosCamposTextiles() {
        UUID actor = UUID.fromString("00000000-0000-4000-8000-000000000001");
        GanaderiaId ganaderia = new GanaderiaId(
                UUID.fromString("00000000-0000-4000-8000-000000000002"));
        AnimalId animal = new AnimalId(
                UUID.fromString("00000000-0000-4000-8000-000000000003"));
        DatosAnimal sinTextil = new DatosAnimal(Sexo.HEMBRA, "Luna", null, null,
                null, null, null, null, null, null, null);
        SolicitudRegistroAnimal v1 = new SolicitudRegistroAnimal(ganaderia, animal,
                new OperacionId(UUID.fromString(
                        "00000000-0000-4000-8000-000000000004")),
                "CREAR_ANIMAL", 1, 0, sinTextil);
        DatosAnimal conTextil = new DatosAnimal(Sexo.HEMBRA, "Luna", null, null,
                "rojo", UbicacionIdentificacionTextil.DERECHA,
                null, null, null, null, null);
        SolicitudRegistroAnimal v2 = new SolicitudRegistroAnimal(ganaderia, animal,
                new OperacionId(UUID.fromString(
                        "00000000-0000-4000-8000-000000000005")),
                "CREAR_ANIMAL", 2, 0, conTextil);

        CanonizadorAltaAnimal canonizador = new CanonizadorAltaAnimal();
        assertThat(HexFormat.of().formatHex(canonizador.calcular(v1, actor)))
                .isEqualTo("c91e83054f68f5127c674e3617193a22418dd5323cafb4d1d6b0fe52d40c60b9");
        assertThat(canonizador.calcular(v2, actor))
                .isNotEqualTo(canonizador.calcular(v1, actor));
    }

    private SolicitudRegistroAnimal solicitud(String nombre) {
        return new SolicitudRegistroAnimal(new GanaderiaId(GANADERIA),
                new AnimalId(ANIMAL), new OperacionId(UUID.randomUUID()),
                "CREAR_ANIMAL", 1, 0,
                new DatosAnimal(Sexo.HEMBRA, nombre, null, null, null,
                        null, null, null, null, null, null));
    }
}
