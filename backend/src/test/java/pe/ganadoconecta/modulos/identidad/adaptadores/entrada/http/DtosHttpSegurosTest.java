package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtosHttpSegurosTest {
    @Test
    void representacionTextualNoExponeCredencialesDatosPersonalesNiCsrf() {
        String correo = "persona@ganadoconecta.invalid";
        String contrasena = "frase-secreta-sintetica";
        String csrf = "csrf-sintetico-no-publicar";

        assertThat(new RegistroUsuarioControlador.RegistroUsuarioSolicitud(
                correo, contrasena).toString())
                .doesNotContain(correo, contrasena)
                .contains("PROTEGIDOS");
        assertThat(new RegistroUsuarioControlador.RegistroUsuarioRespuesta(
                UUID.randomUUID(), correo, "ACTIVO").toString())
                .doesNotContain(correo)
                .contains("PROTEGIDOS");
        assertThat(new CsrfControlador.CsrfRespuesta(csrf).toString())
                .doesNotContain(csrf)
                .contains("PROTEGIDO");
    }
}
