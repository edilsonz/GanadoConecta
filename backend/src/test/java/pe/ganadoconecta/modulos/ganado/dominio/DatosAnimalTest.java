package pe.ganadoconecta.modulos.ganado.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DatosAnimalTest {
    @Test
    void normalizaOpcionalesYConservaDatosValidos() {
        DatosAnimal datos = new DatosAnimal(Sexo.HEMBRA, "  Rosita  ", " A-1 ",
                " ", null, null, " Holstein ", LocalDate.of(2024, 1, 10), false,
                Categoria.VAQUILLA, "  Primera línea\nSegunda línea  ");

        assertThat(datos.nombre()).isEqualTo("Rosita");
        assertThat(datos.areteReverso()).isNull();
        assertThat(datos.observaciones()).isEqualTo("Primera línea\nSegunda línea");
    }

    @Test
    void exigeSexo() {
        assertThatThrownBy(() -> datos(null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("sexo");
    }

    @Test
    void exigeIndicadorCuandoHayFechaYRechazaFechaFutura() {
        assertThatThrownBy(() -> new DatosAnimal(Sexo.MACHO, null, null, null,
                null, null, null, LocalDate.of(2024, 1, 1), null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("nacimientoEstimado");

        DatosAnimal datos = datos(Sexo.MACHO, Categoria.TERNERO);
        assertThatThrownBy(() -> datos.validarFechaNoFutura(LocalDate.of(2023, 12, 31)))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("fechaNacimiento");
    }

    @Test
    void rechazaCategoriaIncompatibleLimitesYControlesInvisibles() {
        assertThatThrownBy(() -> datos(Sexo.MACHO, Categoria.VACA))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("categoria");
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, "x".repeat(101),
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("nombre");
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, "visible\u0000oculto",
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("nombre");
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, "visible\u200Boculto",
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void admiteAreteTextilAmbosONinguno() {
        DatosAnimal soloArete = new DatosAnimal(Sexo.HEMBRA, null, "P-20", null,
                null, null, null, null, null, null, null);
        DatosAnimal soloTextil = new DatosAnimal(Sexo.HEMBRA, null, null, null,
                "  rojo y blanco  ", UbicacionIdentificacionTextil.AMBAS,
                null, null, null, null, null);
        DatosAnimal ambos = new DatosAnimal(Sexo.MACHO, null, "M-8", "familia",
                "azul", UbicacionIdentificacionTextil.IZQUIERDA,
                null, null, null, null, null);
        DatosAnimal ninguno = new DatosAnimal(Sexo.MACHO, null, null, null,
                null, null, null, null, null, null, null);

        assertThat(soloArete.areteFrente()).isEqualTo("P-20");
        assertThat(soloArete.coloresIdentificacionTextil()).isNull();
        assertThat(soloTextil.areteFrente()).isNull();
        assertThat(soloTextil.coloresIdentificacionTextil()).isEqualTo("rojo y blanco");
        assertThat(soloTextil.ubicacionIdentificacionTextil())
                .isEqualTo(UbicacionIdentificacionTextil.AMBAS);
        assertThat(ambos.areteReverso()).isEqualTo("familia");
        assertThat(ambos.ubicacionIdentificacionTextil())
                .isEqualTo(UbicacionIdentificacionTextil.IZQUIERDA);
        assertThat(ninguno.areteFrente()).isNull();
        assertThat(ninguno.coloresIdentificacionTextil()).isNull();
    }

    @Test
    void exigeElParCompletoYValidaColoresTextiles() {
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, null, null, null,
                "rojo", null, null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("ubicacionIdentificacionTextil");
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, null, null, null,
                null, UbicacionIdentificacionTextil.DERECHA,
                null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("coloresIdentificacionTextil");
        assertThatThrownBy(() -> new DatosAnimal(Sexo.HEMBRA, null, null, null,
                "x".repeat(101), UbicacionIdentificacionTextil.DERECHA,
                null, null, null, null, null))
                .isInstanceOf(DatoAnimalInvalidoException.class)
                .hasMessageContaining("coloresIdentificacionTextil");
    }

    private DatosAnimal datos(Sexo sexo, Categoria categoria) {
        return new DatosAnimal(sexo, null, null, null, null,
                null, null, LocalDate.of(2024, 1, 1), false, categoria, null);
    }
}
