package pe.ganadoconecta.modulos.sanidad.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoSanitarioTest {
    EventoSanitario crear(TipoEventoSanitario tipo,String descripcion,LocalDate fecha){
        return new EventoSanitario(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),
                tipo,fecha,null,descripcion,null,null,null,null,null,null,
                UUID.randomUUID(),Instant.now());
    }
    @Test void admiteLosTresHechosNoClinicos(){
        for(var tipo:new TipoEventoSanitario[]{TipoEventoSanitario.DESPARASITACION,
                TipoEventoSanitario.TRATAMIENTO_SANITARIO,
                TipoEventoSanitario.REVISION_VETERINARIA})
            assertThat(crear(tipo,"Hecho informado",LocalDate.now()).tipo()).isEqualTo(tipo);
    }
    @Test void exigeDescripcionYFechaNoFutura(){
        assertThatThrownBy(()->crear(TipoEventoSanitario.DESPARASITACION," ",LocalDate.now())).isInstanceOf(DatoSanitarioInvalidoException.class);
        var hoy=LocalDate.of(2026,1,1);
        assertThatThrownBy(()->crear(TipoEventoSanitario.REVISION_VETERINARIA,"Revisión",hoy.plusDays(1)).validarRealizadoNoFuturo(hoy)).isInstanceOf(DatoSanitarioInvalidoException.class);
    }
}
