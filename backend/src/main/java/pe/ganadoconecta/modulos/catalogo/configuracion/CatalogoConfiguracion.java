package pe.ganadoconecta.modulos.catalogo.configuracion;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.catalogo.adaptadores.salida.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;

@Configuration(proxyBeanMethods=false)
@Profile("postgres")
class CatalogoConfiguracion {
    @Bean CatalogoAnuncios catalogoAnuncios(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional auth,
            JdbcTemplate jdbc, Clock reloj, @Value("${GANADOCONECTA_FOTOS_DIRECTORIO:}") String ruta) {
        return new CatalogoAnuncios(unidad, auth, new AnunciosPostgres(jdbc),
                new FotosDirectorioPrivado(ruta), new FotoJpeg(), reloj);
    }
}
