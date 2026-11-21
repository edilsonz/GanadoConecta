package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties = "ganadoconecta.seguridad.origen-permitido=https://127.0.0.1:5173")
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
@Transactional
class AccesoFamiliarPostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TokensAcceso tokens;
    private Cuenta titular, familiar, ajeno;
    private UUID ganaderia;

    @BeforeEach void preparar() {
        titular = cuenta();
        familiar = cuenta();
        ajeno = cuenta();
        ganaderia = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id)
                VALUES (?,'Ganadería sintética',?)
                """, ganaderia, titular.id());
        jdbc.update("""
                INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol)
                VALUES (?,?,?,'PROPIETARIO')
                """, UUID.randomUUID(), ganaderia, titular.id());
    }

    @Test void aceptaUnaVezAuditaCambiosYRevocaPermisoRemoto() throws Exception {
        String codigo = emitir();
        aceptar(ajeno, codigo).andExpect(status().isConflict());
        aceptar(familiar, codigo).andExpect(status().isOk());
        aceptar(familiar, codigo).andExpect(status().isConflict());
        mvc.perform(get(ruta()).header("Authorization", familiar.jwt())).andExpect(status().isForbidden());
        enviar(put(ruta() + "/integrantes/" + familiar.id()), titular, Map.of("rol","LECTOR"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/ganaderias/" + ganaderia + "/acceso")
                .header("Authorization", familiar.jwt())).andExpect(jsonPath("$.rol").value("LECTOR"));
        enviar(put(ruta() + "/integrantes/" + titular.id()), titular, Map.of("rol","LECTOR"))
                .andExpect(status().isConflict());
        enviar(delete("/api/v1/ganaderias/" + ganaderia + "/membresias/" + familiar.id()), titular, null)
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/ganaderias/" + ganaderia + "/acceso")
                .header("Authorization", familiar.jwt())).andExpect(status().isForbidden());
        assertThat(jdbc.queryForList("""
                SELECT estado_nuevo FROM identidad.auditoria_membresia WHERE usuario_id=? ORDER BY id
                """, String.class, familiar.id())).containsExactly("ACTIVA","ACTIVA","REVOCADA");
    }

    @Test void protegeEmisionYRechazaCancelacionYExpiracion() throws Exception {
        enviar(post(ruta() + "/invitaciones"), ajeno, Map.of("correo", familiar.correo(), "rol", "EDITOR"))
                .andExpect(status().isForbidden());
        enviar(post(ruta() + "/invitaciones"), titular, Map.of("correo", familiar.correo(), "rol", "PROPIETARIO"))
                .andExpect(status().isBadRequest());
        String cancelada = emitir();
        UUID id = jdbc.queryForObject("SELECT id FROM identidad.invitacion WHERE ganaderia_id=?", UUID.class, ganaderia);
        enviar(delete(ruta() + "/invitaciones/" + id), titular, null).andExpect(status().isNoContent());
        aceptar(familiar, cancelada).andExpect(status().isConflict());
        String expirada = emitir();
        jdbc.update("""
                UPDATE identidad.invitacion SET creada_en=now()-interval '3 days', vence_en=now()-interval '1 day'
                WHERE ganaderia_id=? AND estado='PENDIENTE'
                """, ganaderia);
        mvc.perform(get(ruta()).header("Authorization", titular.jwt()))
                .andExpect(jsonPath("$.invitaciones[*].estado").value(org.hamcrest.Matchers.hasItem("EXPIRADA")));
        aceptar(familiar, expirada).andExpect(status().isConflict());
    }

    private String emitir() throws Exception {
        var respuesta = enviar(post(ruta() + "/invitaciones"), titular,
                Map.of("correo", familiar.correo(), "rol","EDITOR")).andExpect(status().isCreated()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsByteArray()).required("codigo").asText();
    }

    private ResultActions aceptar(Cuenta cuenta, String codigo) throws Exception {
        return enviar(post("/api/v1/invitaciones/aceptar"), cuenta, Map.of("codigo", codigo));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder solicitud, Cuenta cuenta, Object datos) throws Exception {
        solicitud.header("Authorization", cuenta.jwt()).header("Origin","https://127.0.0.1:5173").with(csrf().asHeader());
        if (datos != null) solicitud.contentType("application/json").content(json.writeValueAsBytes(datos));
        return mvc.perform(solicitud);
    }

    private String ruta() { return "/api/v1/ganaderias/" + ganaderia + "/familia"; }

    private Cuenta cuenta() {
        UUID id = UUID.randomUUID(), sesion = UUID.randomUUID();
        String correo = id + "@familia.invalid";
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')", id, correo);
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')", sesion, id);
        Instant ahora = Instant.now();
        return new Cuenta(id, correo, "Bearer " + tokens.emitir(id,sesion,ahora,ahora.plusSeconds(600)).valor());
    }

    private record Cuenta(UUID id, String correo, String jwt) {}
}
