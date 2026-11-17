package pe.ganadoconecta.modulos.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContrasenaRegistroTest {
    @Test
    void preservaExactamenteUnaContrasenaValida() {
        String valor = "  clave válida  ";
        assertThat(ContrasenaRegistro.desde(valor).valor()).isEqualTo(valor);
    }

    @Test
    void cuentaCaracteresUnicodeSinTruncar() {
        String quinceEmojis = "🐄".repeat(15);
        assertThat(ContrasenaRegistro.desde(quinceEmojis).valor()).isEqualTo(quinceEmojis);
        assertThatThrownBy(() -> ContrasenaRegistro.desde("a".repeat(14)))
                .isInstanceOf(DatoRegistroInvalidoException.class);
        assertThatThrownBy(() -> ContrasenaRegistro.desde("a".repeat(129)))
                .isInstanceOf(DatoRegistroInvalidoException.class);
    }
}
