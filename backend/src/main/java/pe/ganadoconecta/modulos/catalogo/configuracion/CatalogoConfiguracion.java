package pe.ganadoconecta.modulos.catalogo.configuracion;

import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.catalogo.adaptadores.salida.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.CachePublica;
import pe.ganadoconecta.modulos.catalogo.aplicacion.ModeracionCatalogo;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;

@Configuration(proxyBeanMethods=false)
@Profile("postgres")
class CatalogoConfiguracion {
    @Bean ModeracionCatalogo moderacionCatalogo(UnidadDeTrabajo unidad,JdbcTemplate jdbc,Clock reloj,
            @Value("${GANADOCONECTA_REPORTES_PRUEBA_AUTENTICADA:false}") boolean recepcion) {
        return new ModeracionCatalogo(unidad,new ModeracionPostgres(jdbc),reloj,recepcion);
    }
    @Bean CatalogoAnuncios catalogoAnuncios(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional auth,
            JdbcTemplate jdbc, Clock reloj, @Value("${GANADOCONECTA_FOTOS_DIRECTORIO:}") String ruta,
            ObjectProvider<CachePublica> cache) {
        return new CatalogoAnuncios(unidad, auth, new AnunciosPostgres(jdbc),
                new FotosDirectorioPrivado(ruta), new FotoJpeg(), reloj,
                cache.getIfAvailable(() -> (filtros, origen) -> origen.get()));
    }

    @Bean
    @ConditionalOnProperty(name = "ganadoconecta.catalogo.cache.habilitada", havingValue = "true")
    CachePublica cachePublica(StringRedisTemplate redis, JdbcTemplate jdbc, ObjectMapper json,
            MeterRegistry metricas, @Value("${ganadoconecta.catalogo.cache.ttl-segundos}") long segundos) {
        return new CacheCatalogoRedis(redis, jdbc, json, metricas, segundos);
    }
}
