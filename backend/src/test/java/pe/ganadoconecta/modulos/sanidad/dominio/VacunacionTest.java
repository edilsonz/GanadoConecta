package pe.ganadoconecta.modulos.sanidad.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VacunacionTest {
    private Vacunacion crear(String nombre, LocalDate fecha, LocalDate proxima) {
        return new Vacunacion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                nombre, fecha, null, null, null, proxima, UUID.randomUUID(), Instant.now());
    }
    @Test void admiteOpcionalesAusentes() {
        var v = crear("Vacuna registrada", LocalDate.of(2026, 1, 1), null);
        assertThat(v.responsable()).isNull();
        assertThat(v.proximaFecha()).isNull();
    }
    @Test void exigeNombreYFecha() {
        assertThatThrownBy(() -> crear(" ", LocalDate.now(), null)).isInstanceOf(DatoSanitarioInvalidoException.class);
        assertThatThrownBy(() -> crear("Vacuna", null, null)).isInstanceOf(DatoSanitarioInvalidoException.class);
    }
    @Test void rechazaFechasIncoherentes() {
        var hoy = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> crear("Vacuna", hoy, hoy.minusDays(1))).isInstanceOf(DatoSanitarioInvalidoException.class);
        assertThatThrownBy(() -> crear("Vacuna", hoy.plusDays(1), null).validarFechaNoFutura(hoy)).isInstanceOf(DatoSanitarioInvalidoException.class);
    }
    @Test void rechazaSeparadoresDeCanonizacion() {
        assertThatThrownBy(() -> crear("Vacuna\u001ftexto", LocalDate.now(), null)).isInstanceOf(DatoSanitarioInvalidoException.class);
    }
}
