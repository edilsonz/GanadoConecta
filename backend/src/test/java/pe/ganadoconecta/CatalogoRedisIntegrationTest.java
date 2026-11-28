package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.catalogo.adaptadores.salida.AnunciosPostgres;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.CachePublica;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Filtros;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Pagina;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties = {"ganadoconecta.catalogo.cache.habilitada=true",
        "ganadoconecta.catalogo.cache.ttl-segundos=2", "spring.data.redis.timeout=500ms"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.redis.integration-test", matches = "true")
class CatalogoRedisIntegrationTest extends SoporteJwtPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired StringRedisTemplate redis;
    @Autowired CachePublica cache;
    @Autowired MeterRegistry metricas;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired MockMvc mvc;
    @Autowired TokensAcceso tokens;
    final UUID usuario = UUID.randomUUID(), sesion = UUID.randomUUID(), ganaderia = UUID.randomUUID(),
            animal = UUID.randomUUID(), anuncio = UUID.randomUUID();
    final String ubicacion = "Sintética " + anuncio;
    final String url = "/api/v1/catalogo";

    @BeforeEach void datosSinteticos() {
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).startsWith("ganadoconecta_e2e_");
        new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')",
                    usuario, usuario + "@redis.invalid");
            jdbc.update("""
                    INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en)
                    VALUES (?,?,now(),now()+interval '1 hour')
                    """, sesion, usuario);
            jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Redis sintético',?)",
                    ganaderia, usuario);
            jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,'PROPIETARIO')",
                    UUID.randomUUID(), ganaderia, usuario);
            jdbc.update("""
                    INSERT INTO ganado.animal(id,ganaderia_id,sexo,categoria,raza,observaciones,creado_por,creado_en)
                    VALUES (?,?,'HEMBRA','VACA','Criolla','CAMPO PRIVADO DE PRUEBA',?,now())
                    """, animal, ganaderia, usuario);
            jdbc.update("""
                    INSERT INTO catalogo.anuncio(id,ganaderia_id,animal_id,titulo,caracteristicas,ubicacion_general,
                        precio_pen,genealogia_autorizada,estado,actualizado_en,publicado_en)
                    VALUES (?,?,?,'Anuncio sintético','Datos públicos',?,120,false,'PUBLICADO',now(),now())
                    """, anuncio, ganaderia, animal, ubicacion);
        });
    }

    @AfterEach void limitarVisibilidadDeLaFixture() {
        jdbc.update("UPDATE identidad.ganaderia SET estado='INACTIVA' WHERE id=?", ganaderia);
    }

    @Test void consultaFriaAciertoProyeccionFiltrosPaginacionYCaducidad() throws Exception {
        double ausencias = metrica("ausencia"), aciertos = metrica("acierto");
        String fria = publico(ubicacion, 1);
        assertThat(fria).contains(anuncio.toString()).doesNotContain(
                "CAMPO PRIVADO", "ganaderiaId", "animalId", "password", "accessToken");
        assertThat(metrica("ausencia")).isEqualTo(ausencias + 1);
        assertThat(publico(ubicacion, 1)).isEqualTo(fria);
        assertThat(metrica("acierto")).isEqualTo(aciertos + 1);
        assertThat(publico(ubicacion, 2)).doesNotContain(anuncio.toString());
        assertThat(publico(ubicacion + " distinto", 1)).doesNotContain(anuncio.toString());
        var candidatas = redis.keys("gc:catalogo:publico:v1:*:" + revision() + ":*");
        assertThat(candidatas).hasSize(3);
        assertThat(candidatas.stream().map(redis.opsForValue()::get).anyMatch(
                valor -> valor != null && valor.contains(anuncio.toString())
                        && !valor.contains("CAMPO PRIVADO") && !valor.contains(ganaderia.toString()))).isTrue();
        await().atMost(Duration.ofSeconds(5)).until(() -> redis.countExistingKeys(candidatas) == 0);
        assertThat(publico(ubicacion, 1)).isEqualTo(fria);
        assertThat(metrica("ausencia")).isEqualTo(ausencias + 4);
    }

    @Test void confirmacionInvalidaAlEditarPausarRetirarOVender() throws Exception {
        assertThat(publico(ubicacion, 1)).contains("Anuncio sintético");
        var revisionAnterior = revision();
        new TransactionTemplate(transacciones).executeWithoutResult(tx -> {
            jdbc.update("UPDATE catalogo.anuncio SET titulo='Revertido' WHERE id=?", anuncio);
            tx.setRollbackOnly();
        });
        assertThat(revision()).isEqualTo(revisionAnterior);
        assertThat(publico(ubicacion, 1)).contains("Anuncio sintético").doesNotContain("Revertido");
        jdbc.update("UPDATE catalogo.anuncio SET titulo='Editado' WHERE id=?", anuncio);
        assertThat(publico(ubicacion, 1)).contains("Editado").doesNotContain("Anuncio sintético");
        for (String estado : new String[]{"PAUSADO", "RETIRADO"}) {
            jdbc.update("UPDATE catalogo.anuncio SET estado='PUBLICADO' WHERE id=?", anuncio);
            assertThat(publico(ubicacion, 1)).contains(anuncio.toString());
            jdbc.update("UPDATE catalogo.anuncio SET estado=? WHERE id=?", estado, anuncio);
            assertThat(publico(ubicacion, 1)).doesNotContain(anuncio.toString());
        }
        jdbc.update("UPDATE catalogo.anuncio SET estado='PUBLICADO' WHERE id=?", anuncio);
        assertThat(publico(ubicacion, 1)).contains(anuncio.toString());
        Instant ahora = Instant.now();
        String jwt = tokens.emitir(usuario, sesion, ahora, ahora.plusSeconds(600)).valor();
        mvc.perform(post("/api/v1/ganaderias/{g}/animales/{animal}/venta", ganaderia, animal)
                .header("Origin", "http://127.0.0.1:5173")
                .header("Authorization", "Bearer " + jwt).with(csrf())
                .contentType("application/json").content("{\"operationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());
        assertThat(publico(ubicacion, 1)).doesNotContain(anuncio.toString());
    }

    @Test void unaLecturaEnVueloNoReintroduceUnaPublicacionPausada() throws Exception {
        var filtros = new Filtros(null, null, null, ubicacion, null, null, 1);
        var origen = new AnunciosPostgres(jdbc);
        var consultada = new CountDownLatch(1);
        var continuar = new CountDownLatch(1);
        try (var trabajadores = Executors.newVirtualThreadPerTaskExecutor()) {
            var inicial = trabajadores.submit(() -> cache.obtener(filtros, () -> {
                var pagina = new Pagina(origen.buscar(filtros, 21, 0), null);
                consultada.countDown();
                esperar(continuar);
                return pagina;
            }));
            try {
                assertThat(consultada.await(5, TimeUnit.SECONDS)).isTrue();
                jdbc.update("UPDATE catalogo.anuncio SET estado='PAUSADO' WHERE id=?", anuncio);
            } finally {
                continuar.countDown();
            }
            assertThat(inicial.get(5, TimeUnit.SECONDS).items()).hasSize(1);
            assertThat(publico(ubicacion, 1)).doesNotContain(anuncio.toString());
        }
    }

    @Test void editarMientrasUnaVentaYaBloqueoElAnimalNoInvierteElOrdenDeBloqueo() throws Exception {
        assertThat(publico(ubicacion, 1)).contains(anuncio.toString());
        var animalMutado = new CountDownLatch(1);
        var anuncioEditado = new CountDownLatch(1);
        var unidad = new TransactionTemplate(transacciones);
        try (var trabajadores = Executors.newVirtualThreadPerTaskExecutor()) {
            var venta = trabajadores.submit(() -> unidad.executeWithoutResult(tx -> {
                jdbc.update("UPDATE ganado.animal SET estado='VENDIDO' WHERE id=?", animal);
                animalMutado.countDown();
                esperar(anuncioEditado);
                jdbc.update("UPDATE catalogo.anuncio SET estado='CERRADO_VENTA' WHERE id=?", anuncio);
            }));
            var edicion = trabajadores.submit(() -> {
                esperar(animalMutado);
                unidad.executeWithoutResult(tx -> {
                    jdbc.update("UPDATE catalogo.anuncio SET titulo='Edición concurrente' WHERE id=?", anuncio);
                    anuncioEditado.countDown();
                });
            });
            venta.get(10, TimeUnit.SECONDS);
            edicion.get(10, TimeUnit.SECONDS);
        }
        assertThat(publico(ubicacion, 1)).doesNotContain(anuncio.toString());
    }

    private static void esperar(CountDownLatch hito) {
        try {
            if (!hito.await(5, TimeUnit.SECONDS)) throw new AssertionError("No se alcanzó el hito de concurrencia");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private UUID revision() {
        return jdbc.queryForObject("SELECT revision FROM catalogo.revision_publica WHERE unica", UUID.class);
    }

    private double metrica(String resultado) {
        return metricas.counter("ganadoconecta.catalogo.cache.consultas", "resultado", resultado).count();
    }

    private String publico(String zona, int pagina) throws Exception {
        return mvc.perform(get(url).param("ubicacion", zona).param("pagina", String.valueOf(pagina)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
