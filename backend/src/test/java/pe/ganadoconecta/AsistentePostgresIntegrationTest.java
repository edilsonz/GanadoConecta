package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.*;
import pe.ganadoconecta.modulos.analisis.adaptadores.salida.MotorAsistenteLocal;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties = "ganadoconecta.seguridad.origen-permitido=https://127.0.0.1:5173")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class AsistentePostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired TokensAcceso tokens;
    @Autowired ObjectMapper json;
    @MockitoBean Motor motor;
    private UUID propietario;
    private UUID lector;
    private UUID ganaderia;
    private UUID animal;
    private String token;

    @BeforeEach void preparar() {
        propietario = usuario();
        lector = usuario();
        ganaderia = ganaderia(propietario);
        membresia(ganaderia, lector, "LECTOR");
        animal = animal(ganaderia, propietario);
        token = sesion(lector);
    }

    @Test void proyectaPesosConfirmadosYCitaAmbasFuentesSinEnviarNotas() throws Exception {
        pesos();
        when(motor.inferir(anyString(), anyList())).thenAnswer(invocacion -> {
            List<Hecho> hechos = invocacion.getArgument(1);
            org.junit.jupiter.api.Assertions.assertEquals(List.of(Tipo.ULTIMO_PESAJE, Tipo.PAREJA_PESAJES, Tipo.VARIACION),
                    hechos.stream().map(Hecho::tipo).toList());
            org.junit.jupiter.api.Assertions.assertFalse(hechos.toString().contains("Nota privada"));
            return new Seleccion(Estado.RESPUESTA, Estilo.REGISTROS, List.of("H3"));
        });
        mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.texto").value(org.hamcrest.Matchers.containsString("+15 kg del 01/09/2026 al 30/09/2026")))
                .andExpect(jsonPath("$.fuentes.length()").value(2))
                .andExpect(jsonPath("$.fuentes[0].fecha").value("2026-09-01"))
                .andExpect(jsonPath("$.fuentes[1].fecha").value("2026-09-30"));
    }

    @Test void noLlamaAlMotorSinDatosNiPermisosOConCsrfIncorrecto() throws Exception {
        var externo = usuario();
        var ajena = ganaderia(externo);
        var animalAjeno = animal(ajena, externo);
        mvc.perform(consulta(ganaderia, animal)).andExpect(status().isForbidden());
        mvc.perform(consulta(ganaderia, animal, "https://ajeno.invalid").with(csrf().asHeader()))
                .andExpect(status().isForbidden());
        mvc.perform(consulta(ganaderia, animalAjeno).with(csrf().asHeader())).andExpect(status().isForbidden());
        mvc.perform(consulta(ajena, animalAjeno).with(csrf().asHeader())).andExpect(status().isForbidden());
        mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INSUFICIENTE")).andExpect(jsonPath("$.fuentes").isEmpty());
        verifyNoInteractions(motor);
    }

    @Test void descartaRespuestaSiSeRevocaMembresiaDuranteInferencia() throws Exception {
        pesos();
        when(motor.inferir(anyString(), anyList())).thenAnswer(invocacion -> {
            jdbc.update("UPDATE identidad.membresia SET estado='REVOCADA',revocado_en=now() WHERE usuario_id=? AND ganaderia_id=?",
                    lector, ganaderia);
            return new Seleccion(Estado.RESPUESTA, Estilo.DIRECTO, List.of("H3"));
        });
        mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader())).andExpect(status().isForbidden())
                .andExpect(content().string(""));
        verify(motor).inferir(anyString(), anyList());
    }

    @Test void unaReferenciaInventadaOUnFalloNoSePublicanComoRespuesta() throws Exception {
        pesos();
        when(motor.inferir(anyString(), anyList()))
                .thenReturn(new Seleccion(Estado.RESPUESTA, Estilo.DIRECTO, List.of("H99")))
                .thenThrow(new Indisponible());
        for (int i = 0; i < 2; i++) {
            mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader())).andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(""));
        }
    }

    @Test void respuestaHttpInvalidaYCaidaDelMotorDejanDisponiblesLasOperacionesPrincipales(
            CapturedOutput salida) throws Exception {
        pesos();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var cuerpo = new AtomicReference<String>();
        String secreto = UUID.randomUUID().toString().replace("-", "");
        var adaptador = new MotorAsistenteLocal(servidor.getAddress().getPort(), secreto, json);
        when(motor.inferir(anyString(), anyList())).thenAnswer(
                invocacion -> adaptador.inferir(invocacion.getArgument(0), invocacion.getArgument(1)));
        servidor.createContext("/interno/v1/inferir", intercambio -> {
            try {
                intercambio.getRequestBody().readAllBytes();
                byte[] bytes = cuerpo.get().getBytes(StandardCharsets.UTF_8);
                intercambio.sendResponseHeaders(200, bytes.length);
                intercambio.getResponseBody().write(bytes);
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
        try {
            for (String invalida : List.of("{", "{}", "{\"estado\":\"AJENO\"}",
                    "{\"estado\":\"RESPUESTA\",\"estilo\":\"DIRECTO\",\"referencias\":[\"H99\"]}")) {
                cuerpo.set(invalida);
                mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader()))
                        .andExpect(status().isServiceUnavailable()).andExpect(content().string(""));
            }
        } finally {
            servidor.stop(0);
        }
        mvc.perform(consulta(ganaderia, animal).with(csrf().asHeader()))
                .andExpect(status().isServiceUnavailable()).andExpect(content().string(""));
        assertThat(salida.getAll().lines().filter(linea -> linea.contains("Asistente interno: resultado no disponible")).count())
                .as("advertencias de fallos del motor").isGreaterThanOrEqualTo(5);
        assertThat(salida.getAll().contains(secreto)).as("credencial interna ausente de los registros").isFalse();

        String ruta = "/api/v1/ganaderias/" + ganaderia + "/animales";
        String credencial = sesion(propietario);
        UUID alta = UUID.randomUUID();
        mvc.perform(post(ruta).header("Authorization", credencial).header("Origin", "https://127.0.0.1:5173")
                .with(csrf().asHeader()).contentType("application/json").content("""
                    {"animalId":"%s","operationId":"%s","tipo":"CREAR_ANIMAL","schemaVersion":2,
                     "baseVersion":0,"sexo":"HEMBRA","nombre":"Continuidad sintética"}
                    """.formatted(alta, UUID.randomUUID()))).andExpect(status().isCreated());
        mvc.perform(get(ruta + "/" + alta).header("Authorization", credencial))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Continuidad sintética"));
        mvc.perform(get(ruta + "/" + animal + "/eventos-sanitarios").header("Authorization", credencial))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/v1/catalogo")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ganado.medicion_peso WHERE animal_id=?", Long.class, animal))
                .isEqualTo(3);
    }

    private MockHttpServletRequestBuilder consulta(UUID finca, UUID animalId) {
        return consulta(finca, animalId, "https://127.0.0.1:5173");
    }

    private MockHttpServletRequestBuilder consulta(UUID finca, UUID animalId, String origen) {
        return post("/api/v1/ganaderias/" + finca + "/asistente").header("Authorization", token)
                .header("Origin", origen).contentType("application/json").content("""
                    {"pregunta":"¿Cuánto cambió el peso?","tema":"PESO","desde":"2026-09-01",
                     "hasta":"2026-09-30","animalId":"%s"}
                    """.formatted(animalId));
    }

    private void pesos() {
        jdbc.update("""
                INSERT INTO ganado.medicion_peso (id,ganaderia_id,animal_id,kilos,fecha,observaciones,actor_id,registrado_en)
                SELECT gen_random_uuid(),?,?,kilos,fecha,'Nota privada',?,now()
                FROM (VALUES (120,DATE '2026-09-01'), (135,DATE '2026-09-30'), (180,DATE '2026-10-01')) p(kilos,fecha)
                """, ganaderia, animal, propietario);
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')",
                id, id + "@asistente.invalid");
        return id;
    }

    private UUID ganaderia(UUID actor) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Sintética',?)", id, actor);
        membresia(id, actor, "PROPIETARIO");
        return id;
    }

    private void membresia(UUID finca, UUID actor, String rol) {
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,?)",
                UUID.randomUUID(), finca, actor, rol);
    }

    private UUID animal(UUID finca, UUID actor) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO ganado.animal(id,ganaderia_id,sexo,creado_por,creado_en) VALUES (?,?,'HEMBRA',?,now())", id, finca, actor);
        return id;
    }

    private String sesion(UUID usuario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')", id, usuario);
        var ahora = Instant.now();
        return "Bearer " + tokens.emitir(usuario, id, ahora, ahora.plusSeconds(600)).valor();
    }
}
