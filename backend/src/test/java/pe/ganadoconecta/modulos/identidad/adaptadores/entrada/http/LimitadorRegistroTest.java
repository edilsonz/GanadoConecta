package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LimitadorRegistroTest {
    @Test
    void limitaPorOrigenYLiberaLaVentanaSinBloqueoPermanente() {
        RelojMutable reloj = new RelojMutable(Instant.parse("2026-11-17T18:00:00Z"));
        LimitadorRegistro limitador = new LimitadorRegistro(2, Duration.ofMinutes(15), 100, reloj);

        assertThat(limitador.intentar("192.0.2.1").permitido()).isTrue();
        assertThat(limitador.intentar("192.0.2.1").permitido()).isTrue();
        LimitadorRegistro.Resultado rechazado = limitador.intentar("192.0.2.1");
        assertThat(rechazado.permitido()).isFalse();
        assertThat(rechazado.reintentarEnSegundos()).isBetween(1L, 900L);
        assertThat(limitador.intentar("192.0.2.2").permitido()).isTrue();

        reloj.avanzar(Duration.ofMinutes(15));
        assertThat(limitador.intentar("192.0.2.1").permitido()).isTrue();
    }

    private static final class RelojMutable extends Clock {
        private Instant instante;

        private RelojMutable(Instant instante) {
            this.instante = instante;
        }

        void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
