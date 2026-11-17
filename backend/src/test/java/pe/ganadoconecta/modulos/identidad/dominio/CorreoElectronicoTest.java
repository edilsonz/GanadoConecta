package pe.ganadoconecta.modulos.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CorreoElectronicoTest {
    @Test
    void normalizaEspaciosExterioresYMayusculas() {
        assertThat(CorreoElectronico.desde("  Persona.Prueba@EJEMPLO.COM  ").valor())
                .isEqualTo("persona.prueba@ejemplo.com");
    }

    @Test
    void rechazaFormatoInvalidoYEspaciosInteriores() {
        for (String correo : new String[]{"sin-arroba", "@ejemplo.com",
                "persona@", "dos@@ejemplo.com", "persona @ejemplo.com"}) {
            assertThatThrownBy(() -> CorreoElectronico.desde(correo))
                    .isInstanceOf(DatoRegistroInvalidoException.class);
        }
    }
}
