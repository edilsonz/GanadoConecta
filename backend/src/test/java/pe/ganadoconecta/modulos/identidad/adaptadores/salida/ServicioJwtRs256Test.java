package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokenAccesoInvalidoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

class ServicioJwtRs256Test {
    private static KeyPair claves;

    @BeforeAll
    static void generarClavesEfimeras() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        claves = generator.generateKeyPair();
    }

    @Test
    void emiteRs256ConClaimsObligatoriosYExpiracionDeDiezMinutos() {
        Instant ahora = Instant.parse("2026-11-17T18:00:00Z");
        RelojMutable reloj = new RelojMutable(ahora);
        ServicioJwtRs256 jwt = servicio(reloj, "ganadoconecta", "ganadoconecta-web");
        UUID usuario = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();

        TokensAcceso.TokenEmitido emitido = jwt.emitir(
                usuario, sesion, ahora, ahora.plusSeconds(600));
        TokensAcceso.IdentidadToken identidad = jwt.verificar(emitido.valor());

        assertThat(identidad.usuarioId()).isEqualTo(usuario);
        assertThat(identidad.sesionId()).isEqualTo(sesion);
        assertThat(identidad.tokenId()).isNotNull();
        assertThat(identidad.emitidoEn()).isEqualTo(ahora);
        assertThat(identidad.expiraEn()).isEqualTo(ahora.plusSeconds(600));
        assertThat(emitido.toString()).doesNotContain(emitido.valor()).contains("PROTEGIDO");
    }

    @Test
    void rechazaTokenExpiradoAlteradoYConEmisorOAudienciaDistintos() {
        Instant ahora = Instant.parse("2026-11-17T18:00:00Z");
        RelojMutable reloj = new RelojMutable(ahora);
        ServicioJwtRs256 jwt = servicio(reloj, "ganadoconecta", "ganadoconecta-web");
        String token = jwt.emitir(UUID.randomUUID(), UUID.randomUUID(),
                ahora, ahora.plusSeconds(600)).valor();

        reloj.fijar(ahora.plusSeconds(601));
        assertThatThrownBy(() -> jwt.verificar(token))
                .isInstanceOf(TokenAccesoInvalidoException.class);

        reloj.fijar(ahora);
        String alterado = token.substring(0, token.length() - 2) + "aa";
        assertThatThrownBy(() -> jwt.verificar(alterado))
                .isInstanceOf(TokenAccesoInvalidoException.class);
        assertThatThrownBy(() -> servicio(reloj, "otro", "ganadoconecta-web").verificar(token))
                .isInstanceOf(TokenAccesoInvalidoException.class);
        assertThatThrownBy(() -> servicio(reloj, "ganadoconecta", "otra").verificar(token))
                .isInstanceOf(TokenAccesoInvalidoException.class);
    }

    private ServicioJwtRs256 servicio(Clock reloj, String emisor, String audiencia) {
        return new ServicioJwtRs256(
                (RSAPublicKey) claves.getPublic(),
                (RSAPrivateKey) claves.getPrivate(),
                emisor, audiencia, reloj);
    }

    static final class RelojMutable extends Clock {
        private Instant actual;

        RelojMutable(Instant actual) {
            this.actual = actual;
        }

        void fijar(Instant nuevo) {
            this.actual = nuevo;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return actual;
        }
    }
}
