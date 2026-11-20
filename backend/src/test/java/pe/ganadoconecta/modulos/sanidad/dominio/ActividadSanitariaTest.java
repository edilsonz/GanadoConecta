package pe.ganadoconecta.modulos.sanidad.dominio;
import static org.assertj.core.api.Assertions.*;import java.time.*;import java.util.*;import org.junit.jupiter.api.Test;
class ActividadSanitariaTest{
 ActividadSanitaria crear(LocalDate f,EstadoActividadSanitaria e){return new ActividadSanitaria(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,TipoActividadSanitaria.VACUNACION,f,"Control informado",null,e,e==EstadoActividadSanitaria.PENDIENTE?1:2,UUID.randomUUID(),Instant.now(),e==EstadoActividadSanitaria.COMPLETADA?UUID.randomUUID():null,e==EstadoActividadSanitaria.COMPLETADA?Instant.now():null);}
 @Test void distingueProximaVencidaYCompletada(){var hoy=LocalDate.of(2026,11,20);assertThat(crear(hoy.plusDays(1),EstadoActividadSanitaria.PENDIENTE).estadoVisible(hoy)).isEqualTo("PROXIMA");assertThat(crear(hoy.minusDays(1),EstadoActividadSanitaria.PENDIENTE).estadoVisible(hoy)).isEqualTo("VENCIDA");assertThat(crear(hoy.minusDays(1),EstadoActividadSanitaria.COMPLETADA).estadoVisible(hoy)).isEqualTo("COMPLETADA");}
 @Test void programacionNoAdmiteFechaPasada(){var hoy=LocalDate.of(2026,11,20);assertThatThrownBy(()->crear(hoy.minusDays(1),EstadoActividadSanitaria.PENDIENTE).validarFechaProgramable(hoy)).isInstanceOf(DatoSanitarioInvalidoException.class);}
}
