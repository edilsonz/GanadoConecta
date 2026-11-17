package pe.ganadoconecta.modulos.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RenovarYValidarSesionTest {
    private static final Instant AHORA = Instant.parse("2026-11-17T18:00:00Z");

    @Test
    void renovacionNoExtiendeSesionYLimitaJwtAlVencimientoAbsoluto() {
        UUID usuario = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        SesionesFalsas sesiones = new SesionesFalsas(RepositorioSesiones.Rotacion.valida(
                sesion, usuario, AHORA.plusSeconds(300)), true);
        RenovarSesion renovar = new RenovarSesion(sesiones, new RenovacionesFalsas(),
                new TokensFalsos(), UUID::randomUUID, Clock.fixed(AHORA, ZoneOffset.UTC));

        RenovarSesion.Resultado resultado = renovar.ejecutar("refresh-anterior");

        assertThat(resultado.sesionVenceEn()).isEqualTo(AHORA.plusSeconds(300));
        assertThat(resultado.tokenAcceso().expiraEn()).isEqualTo(AHORA.plusSeconds(300));
        assertThat(resultado.toString()).doesNotContain("refresh-sucesor");
    }

    @Test
    void reutilizacionYEstadoDeSesionRevocadoSeRechazan() {
        SesionesFalsas reutilizada = new SesionesFalsas(
                RepositorioSesiones.Rotacion.reutilizada(), false);
        RenovarSesion renovar = new RenovarSesion(reutilizada, new RenovacionesFalsas(),
                new TokensFalsos(), UUID::randomUUID, Clock.fixed(AHORA, ZoneOffset.UTC));
        assertThatThrownBy(() -> renovar.ejecutar("refresh-consumido"))
                .isExactlyInstanceOf(SesionNoValidaException.class);

        UUID usuario = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        TokensAcceso tokens = new TokensFalsos(usuario, sesion);
        ValidarTokenAcceso validar = new ValidarTokenAcceso(tokens,
                new SesionesFalsas(RepositorioSesiones.Rotacion.invalida(), false),
                Clock.fixed(AHORA, ZoneOffset.UTC));
        assertThatThrownBy(() -> validar.ejecutar("jwt-firmado"))
                .isExactlyInstanceOf(SesionNoValidaException.class);
    }

    private static final class SesionesFalsas implements RepositorioSesiones {
        private final Rotacion rotacion;
        private final boolean vigente;

        SesionesFalsas(Rotacion rotacion, boolean vigente) {
            this.rotacion = rotacion;
            this.vigente = vigente;
        }

        @Override
        public void crear(NuevaSesion sesion) {
        }

        @Override
        public Rotacion rotar(String actual, UUID sucesorId, String sucesor, Instant ahora) {
            return rotacion;
        }

        @Override
        public void revocarPorHuella(String huella, Instant ahora) {
        }

        @Override
        public boolean estaVigente(UUID sesionId, UUID usuarioId, Instant ahora) {
            return vigente;
        }
    }

    private static final class RenovacionesFalsas implements GeneradorCredencialesRenovacion {
        @Override
        public CredencialRenovacion generar() {
            return new CredencialRenovacion("refresh-sucesor", "b".repeat(64));
        }

        @Override
        public String calcularHuella(String valor) {
            return "a".repeat(64);
        }
    }

    private static final class TokensFalsos implements TokensAcceso {
        private final UUID usuario;
        private final UUID sesion;

        TokensFalsos() {
            this(UUID.randomUUID(), UUID.randomUUID());
        }

        TokensFalsos(UUID usuario, UUID sesion) {
            this.usuario = usuario;
            this.sesion = sesion;
        }

        @Override
        public TokenEmitido emitir(UUID usuarioId, UUID sesionId,
                Instant emitidoEn, Instant expiraEn) {
            return new TokenEmitido("jwt-sucesor", expiraEn);
        }

        @Override
        public IdentidadToken verificar(String token) {
            return new IdentidadToken(usuario, sesion, UUID.randomUUID(),
                    AHORA, AHORA.plusSeconds(600));
        }
    }
}
