package pe.ganadoconecta.modulos.catalogo.adaptadores.salida;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.CachePublica;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Filtros;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Pagina;

public final class CacheCatalogoRedis implements CachePublica {
    private static final Logger LOG = LoggerFactory.getLogger(CacheCatalogoRedis.class);
    private static final long PAUSA_NANOS = Duration.ofSeconds(2).toNanos();
    private static final String PREFIJO = "gc:catalogo:publico:v1:publicado-desc-id-desc:";
    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Duration ttl;
    private final Counter aciertos, ausencias, fallos, omisiones;
    private final AtomicLong reintentarDesde = new AtomicLong(System.nanoTime());

    public CacheCatalogoRedis(StringRedisTemplate redis, JdbcTemplate jdbc, ObjectMapper json,
            MeterRegistry metricas, long segundos) {
        if (segundos < 1 || segundos > 300) throw new IllegalArgumentException("TTL del catálogo fuera de rango");
        this.redis = redis;
        this.jdbc = jdbc;
        this.json = json;
        ttl = Duration.ofSeconds(segundos);
        aciertos = contador(metricas, "acierto");
        ausencias = contador(metricas, "ausencia");
        fallos = contador(metricas, "fallo");
        omisiones = contador(metricas, "omitida");
    }

    @Override public Pagina obtener(Filtros filtros, Supplier<Pagina> origen) {
        if (System.nanoTime() - reintentarDesde.get() < 0) {
            omisiones.increment();
            return origen.get();
        }
        String clave;
        try {
            UUID revision = jdbc.queryForObject("SELECT revision FROM catalogo.revision_publica WHERE unica", UUID.class);
            if (revision == null) throw new IllegalStateException("Falta la revisión del catálogo");
            clave = PREFIJO + revision + ":" + huella(json.writeValueAsBytes(filtros));
            String resultado = redis.opsForValue().get(clave);
            if (resultado != null) {
                Pagina pagina = json.readValue(resultado, Pagina.class);
                if (pagina == null || pagina.items() == null) throw new IllegalStateException("Página cacheada inválida");
                aciertos.increment();
                return pagina;
            }
            ausencias.increment();
        } catch (DataAccessException | JsonProcessingException | IllegalStateException error) {
            fallo(error);
            return origen.get();
        }

        Pagina pagina = origen.get();
        try {
            redis.opsForValue().set(clave, json.writeValueAsString(pagina), ttl);
        } catch (DataAccessException | JsonProcessingException | IllegalStateException error) {
            fallo(error);
        }
        return pagina;
    }

    private void fallo(Exception error) {
        fallos.increment();
        long ahora = System.nanoTime();
        long previo = reintentarDesde.get();
        if (ahora - previo >= 0 && reintentarDesde.compareAndSet(previo, ahora + PAUSA_NANOS)) {
            LOG.warn("Caché pública en pausa. Se consultará PostgreSQL y se reintentará Redis. Tipo: {}",
                    error.getClass().getSimpleName());
        }
    }

    private static Counter contador(MeterRegistry registro, String resultado) {
        return registro.counter("ganadoconecta.catalogo.cache.consultas", "resultado", resultado);
    }

    private static String huella(byte[] datos) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }
}
