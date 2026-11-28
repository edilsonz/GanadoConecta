package pe.ganadoconecta.modulos.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;

class PublicarAnuncioTest {
    private final Ambito ambito = new Ambito(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    private final UUID anuncio = UUID.randomUUID();
    private final Repositorio repo = mock(Repositorio.class);
    private final CatalogoAnuncios caso = new CatalogoAnuncios(new UnidadDeTrabajo() {
        @Override public <T> T ejecutar(Supplier<T> trabajo) { return trabajo.get(); }
    }, mock(AutorizarAccesoGanaderiaTransaccional.class), repo, mock(Fotos.class),
        mock(PrepararFoto.class), Clock.systemUTC(), (filtros, origen) -> origen.get());

    @ParameterizedTest
    @EnumSource(value = Estado.class, names = {"BORRADOR", "PAUSADO"})
    void publicaSoloDesdeEstadosEditables(Estado estado) {
        var publicado = new Privado(anuncio, null, Estado.PUBLICADO, 3, java.util.List.of());
        when(repo.privado(ambito.ganaderia(), ambito.animal(), anuncio))
                .thenReturn(new Privado(anuncio, null, estado, 2, java.util.List.of()), publicado);

        assertThat(caso.cambiarEstado(ambito, anuncio, 2, Accion.PUBLICAR)).isEqualTo(publicado);
        var orden = inOrder(repo);
        orden.verify(repo).exigirAnimalActivo(ambito.ganaderia(), ambito.animal());
        orden.verify(repo).estado(eq(anuncio), eq(Estado.PUBLICADO), any());
    }

    @ParameterizedTest
    @EnumSource(value = Estado.class, names = {"PUBLICADO", "RETIRADO", "CERRADO_VENTA"})
    void rechazaLaPublicacionEnLosDemasEstados(Estado estado) {
        when(repo.privado(ambito.ganaderia(), ambito.animal(), anuncio))
                .thenReturn(new Privado(anuncio, null, estado, 2, java.util.List.of()));

        assertThatThrownBy(() -> caso.cambiarEstado(ambito, anuncio, 2, Accion.PUBLICAR))
                .isInstanceOfSatisfying(Fallo.class, fallo -> assertThat(fallo.tipo).isEqualTo(Error.CONFLICTO));
        verify(repo, never()).estado(any(), any(), any());
    }
}
