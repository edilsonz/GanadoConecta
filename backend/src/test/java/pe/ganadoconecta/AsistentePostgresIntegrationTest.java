package pe.ganadoconecta;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.*;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties = "ganadoconecta.seguridad.origen-permitido=https://127.0.0.1:5173")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
@Transactional
class AsistentePostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired TokensAcceso tokens;
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
