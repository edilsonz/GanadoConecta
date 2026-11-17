package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioAutenticable;

class IniciarSesionTest {
    private static final Instant AHORA = Instant.parse("2026-11-17T18:00:00Z");
    private static final String CORREO = "persona@ganadoconecta.invalid";
    private static final String CLAVE = "contraseña-sintética-segura";

    @Test
    void credencialesValidasCreanSesionDeVeinticuatroHorasYJwtDeDiezMinutos() {
        UUID usuarioId = UUID.randomUUID();
        UUID sesionId = UUID.randomUUID();
        UUID verificadorId = UUID.randomUUID();
        SesionesFalsas sesiones = new SesionesFalsas();
        IniciarSesion caso = caso(Optional.of(usuario(usuarioId,
                UsuarioAutenticable.Estado.ACTIVO)), sesiones, sesionId, verificadorId);

        IniciarSesion.Resultado resultado = caso.ejecutar(
                new IniciarSesion.Comando(" PERSONA@GANADOCONECTA.INVALID ", CLAVE, "192.0.2.1"));

        assertThat(resultado.usuarioId()).isEqualTo(usuarioId);
        assertThat(resultado.sesionId()).isEqualTo(sesionId);
        assertThat(resultado.sesionVenceEn()).isEqualTo(AHORA.plusSeconds(86_400));
        assertThat(resultado.tokenAcceso().expiraEn()).isEqualTo(AHORA.plusSeconds(600));
        assertThat(sesiones.creada.verificadorId()).isEqualTo(verificadorId);
        assertThat(sesiones.creada.huellaRenovacion()).hasSize(64);
        assertThat(resultado.toString()).doesNotContain(CLAVE, resultado.tokenRenovacion());
    }

    @Test
    void inexistenteInactivoYContrasenaIncorrectaProducenElMismoErrorGenerico() {
        for (Optional<UsuarioAutenticable> usuario : java.util.List.of(
                Optional.<UsuarioAutenticable>empty(),
                Optional.of(usuario(UUID.randomUUID(), UsuarioAutenticable.Estado.INACTIVO)),
                Optional.of(new UsuarioAutenticable(UUID.randomUUID(),
                        CorreoElectronico.desde(CORREO), "hash:otra",
                        UsuarioAutenticable.Estado.ACTIVO)))) {
            CodificadorFalso codificador = new CodificadorFalso();
            IniciarSesion caso = caso(usuario, new SesionesFalsas(), codificador,
                    UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> caso.ejecutar(
                    new IniciarSesion.Comando(CORREO, CLAVE, "192.0.2.2")))
                    .isExactlyInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Las credenciales no son válidas");
            assertThat(codificador.comparaciones).isEqualTo(1);
        }
    }

    @Test
    void correoInvalidoTambienUsaHashSimuladoYNoCreaSesion() {
        CodificadorFalso codificador = new CodificadorFalso();
        SesionesFalsas sesiones = new SesionesFalsas();
        IniciarSesion caso = caso(Optional.empty(), sesiones, codificador,
                UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> caso.ejecutar(
                new IniciarSesion.Comando("correo-invalido", CLAVE, "192.0.2.3")))
                .isExactlyInstanceOf(CredencialesInvalidasException.class);
        assertThat(codificador.ultimoHash).isEqualTo("hash:simulado");
        assertThat(sesiones.creada).isNull();
    }

    private IniciarSesion caso(Optional<UsuarioAutenticable> usuario,
            SesionesFalsas sesiones, UUID sesionId, UUID verificadorId) {
        return caso(usuario, sesiones, new CodificadorFalso(), sesionId, verificadorId);
    }

    private IniciarSesion caso(Optional<UsuarioAutenticable> usuario,
            SesionesFalsas sesiones, CodificadorFalso codificador,
            UUID sesionId, UUID verificadorId) {
        Queue<UUID> ids = new ArrayDeque<>(java.util.List.of(sesionId, verificadorId));
        return new IniciarSesion(
                correo -> usuario,
                codificador,
                sesiones,
                new RenovacionesFalsas(),
                new TokensFalsos(),
                new LimiteSinBloqueo(),
                ids::remove,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                "hash:simulado");
    }

    private UsuarioAutenticable usuario(UUID id, UsuarioAutenticable.Estado estado) {
        return new UsuarioAutenticable(id, CorreoElectronico.desde(CORREO),
                "hash:" + CLAVE, estado);
    }

    private static final class CodificadorFalso implements CodificadorContrasena {
        private int comparaciones;
        private String ultimoHash;

        @Override
        public String codificar(String contrasena) {
            return "hash:" + contrasena;
        }

        @Override
        public boolean coincide(String contrasena, String hash) {
            comparaciones++;
            ultimoHash = hash;
            return hash.equals("hash:" + contrasena);
        }
    }

    private static final class SesionesFalsas implements RepositorioSesiones {
        private NuevaSesion creada;

        @Override
        public void crear(NuevaSesion sesion) {
            creada = sesion;
        }

        @Override
        public Rotacion rotar(String actual, UUID sucesorId, String sucesor, Instant ahora) {
            return Rotacion.invalida();
        }

        @Override
        public void revocarPorHuella(String huella, Instant ahora) {
        }

        @Override
        public boolean estaVigente(UUID sesionId, UUID usuarioId, Instant ahora) {
            return false;
        }
    }

    private static final class RenovacionesFalsas implements GeneradorCredencialesRenovacion {
        @Override
        public CredencialRenovacion generar() {
            return new CredencialRenovacion("refresh-secreto", "a".repeat(64));
        }

        @Override
        public String calcularHuella(String valor) {
            return "a".repeat(64);
        }
    }

    private static final class TokensFalsos implements TokensAcceso {
        @Override
        public TokenEmitido emitir(UUID usuarioId, UUID sesionId,
                Instant emitidoEn, Instant expiraEn) {
            return new TokenEmitido("jwt-secreto", expiraEn);
        }

        @Override
        public IdentidadToken verificar(String token) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class LimiteSinBloqueo implements LimiteIntentosLogin {
        @Override
        public void verificarPermitido(String correo, String origen, Instant ahora) {
        }

        @Override
        public void registrarFallo(String correo, String origen, Instant ahora) {
        }
    }
}
