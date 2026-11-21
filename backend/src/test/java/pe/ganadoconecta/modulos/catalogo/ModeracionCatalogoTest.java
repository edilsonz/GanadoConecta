package pe.ganadoconecta.modulos.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.ModeracionCatalogo;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;

class ModeracionCatalogoTest {
    @Test void recepcionDeshabilitadaNoAccedeAInfraestructura() {
        var repo=mock(ModeracionCatalogo.Repositorio.class);
        var unidad=mock(UnidadDeTrabajo.class);
        var caso=new ModeracionCatalogo(unidad,repo,Clock.systemUTC(),false);
        assertThat(caso.recepcionExperimental()).isFalse();
        assertThatThrownBy(() -> caso.reportar(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"Prueba sintética"))
                .isInstanceOf(Fallo.class);
        verifyNoInteractions(repo,unidad);
    }
}
