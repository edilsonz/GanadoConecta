package pe.ganadoconecta.modulos.ganado.dominio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentificadoresAnimalTest {
    @Test
    void aceptaUuidV4ParaAnimalYOperacion() {
        new AnimalId(UUID.randomUUID());
        new OperacionId(UUID.randomUUID());
    }

    @Test
    void rechazaUuidQueNoSeaV4() {
        UUID versionUno = UUID.fromString("d9428888-122b-11e1-b85c-61cd3cbb3210");
        assertThatThrownBy(() -> new AnimalId(versionUno))
                .isInstanceOf(DatoAnimalInvalidoException.class);
        assertThatThrownBy(() -> new OperacionId(versionUno))
                .isInstanceOf(DatoAnimalInvalidoException.class);
    }

    @Test
    void rechazaUuidVersionCuatroSinVarianteRfc() {
        UUID varianteReservada = UUID.fromString("123e4567-e89b-42d3-0234-426614174000");
        assertThatThrownBy(() -> new AnimalId(varianteReservada))
                .isInstanceOf(DatoAnimalInvalidoException.class);
        assertThatThrownBy(() -> new OperacionId(varianteReservada))
                .isInstanceOf(DatoAnimalInvalidoException.class);
    }
}
