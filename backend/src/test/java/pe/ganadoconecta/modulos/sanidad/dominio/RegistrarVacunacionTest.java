package pe.ganadoconecta.modulos.sanidad.dominio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.*;
import pe.ganadoconecta.modulos.sanidad.aplicacion.*;

class RegistrarVacunacionTest {
    @Test void autorizaAntesDePersistirYNoEscribeSiSeDeniega() {
        var auth=mock(AutorizarAccesoGanaderiaTransaccional.class);
        var repo=mock(RepositorioVacunaciones.class);
        UnidadDeTrabajo uow=new UnidadDeTrabajo() { public <T> T ejecutar(Supplier<T> trabajo) { return trabajo.get(); } };
        var clock=Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"),ZoneOffset.UTC);
        var caso=new RegistrarVacunacion(uow,auth,repo,clock);
        var actor=UUID.randomUUID(); var sesion=UUID.randomUUID(); var g=UUID.randomUUID();
        var solicitud=new SolicitudVacunacion(g,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,
                "Vacuna",LocalDate.of(2026,1,1),null,null,null,null);
        var resultado=new ResultadoVacunacion(solicitud.operationId(),solicitud.eventId(),ResultadoVacunacion.Estado.APLICADO,clock.instant());
        when(repo.registrar(any(),any(),any(),eq(1))).thenReturn(resultado);
        assertThat(caso.ejecutar(solicitud,actor,sesion)).isEqualTo(resultado);
        var orden=inOrder(auth,repo);
        orden.verify(auth).autorizar(actor,sesion,g,PermisoGanaderia.ESCRIBIR,clock.instant());
        orden.verify(repo).registrar(any(),eq(solicitud.operationId()),any(),eq(1));
        reset(repo);
        when(auth.autorizar(any(),any(),any(),any(),any())).thenThrow(new AccesoGanaderiaDenegadoException());
        assertThatThrownBy(() -> caso.ejecutar(solicitud,actor,sesion)).isInstanceOf(AccesoGanaderiaDenegadoException.class);
        verifyNoInteractions(repo);
    }
}
