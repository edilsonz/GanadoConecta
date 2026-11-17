package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

class RegistrarUsuarioTest {
    @Test
    void entregaAlRepositorioSoloCorreoNormalizadoYHash() {
        AtomicReference<UsuarioNuevo> guardado = new AtomicReference<>();
        UUID id = UUID.fromString("d27c7809-a6c4-430a-a12e-f8d813b5d9e7");
        RepositorioUsuarios repositorio = guardado::set;
        CodificadorContrasena codificador = new CodificadorContrasena() {
            @Override
            public String codificar(String contrasena) {
                return "$argon2id$hash-sintetico";
            }

            @Override
            public boolean coincide(String contrasena, String hash) {
                return false;
            }
        };
        RegistrarUsuario casoUso = new RegistrarUsuario(repositorio, codificador, () -> id);

        RegistrarUsuario.Resultado resultado = casoUso.ejecutar(
                new RegistrarUsuario.Comando(
                        " Persona@Ejemplo.com ", "contraseña-segura-para-prueba"));

        assertThat(resultado.id()).isEqualTo(id);
        assertThat(resultado.correo()).isEqualTo("persona@ejemplo.com");
        assertThat(resultado.estado()).isEqualTo("ACTIVO");
        assertThat(guardado.get().correo().valor()).isEqualTo("persona@ejemplo.com");
        assertThat(guardado.get().passwordHash()).isEqualTo("$argon2id$hash-sintetico");
        assertThat(guardado.get().passwordHash()).doesNotContain("contraseña-segura-para-prueba");
    }
}
