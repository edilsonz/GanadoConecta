package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.aplicacion.IniciarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.RenovarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

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

        Instant ahora = Instant.parse("2026-11-17T18:00:00Z");
        String refresh = "refresh-sintetico-no-publicar";
        String jwt = "jwt-sintetico-no-publicar";
        TokensAcceso.TokenEmitido acceso = new TokensAcceso.TokenEmitido(
                jwt, ahora.plusSeconds(600));
        assertThat(new IniciarSesion.Comando(correo, contrasena, "192.0.2.1").toString())
                .doesNotContain(correo, contrasena)
                .contains("PROTEGIDOS");
        assertThat(new IniciarSesion.Resultado(
                UUID.randomUUID(), correo, UUID.randomUUID(), acceso,
                refresh, ahora.plusSeconds(86_400)).toString())
                .doesNotContain(correo, contrasena, refresh, jwt)
                .contains("PROTEGIDAS");
        assertThat(new RenovarSesion.Resultado(
                UUID.randomUUID(), UUID.randomUUID(), acceso,
                refresh, ahora.plusSeconds(86_400)).toString())
                .doesNotContain(refresh, jwt)
                .contains("PROTEGIDAS");
        assertThat(new AutenticacionControlador.LoginSolicitud(correo, contrasena).toString())
                .doesNotContain(correo, contrasena)
                .contains("PROTEGIDOS");
        assertThat(new AutenticacionControlador.LoginRespuesta(
                UUID.randomUUID(), correo, "ACTIVO", jwt, ahora.plusSeconds(600)).toString())
                .doesNotContain(correo, jwt)
                .contains("PROTEGIDAS");
        assertThat(new AutenticacionControlador.TokenAccesoRespuesta(
                jwt, ahora.plusSeconds(600)).toString())
                .doesNotContain(jwt)
                .contains("PROTEGIDAS");
    }
}
