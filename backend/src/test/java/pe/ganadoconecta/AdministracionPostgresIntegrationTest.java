package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
@Transactional
class AdministracionPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String RUTA = "/api/v1/administracion";
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokensAcceso tokens;

    @Test void exigePermisoDePlataformaIndependienteYVigente() throws Exception {
        var usuario = usuario("ACTIVO");
        var propietario = usuario("ACTIVO");
        var moderador = usuario("ACTIVO");
        ganaderia(propietario);
        jdbc.update("INSERT INTO identidad.permiso_moderacion(usuario_id, habilitado) VALUES (?,true)", moderador);
        mvc.perform(get(RUTA + "/usuarios")).andExpect(status().isUnauthorized());
        for (UUID actor : new UUID[]{usuario, propietario, moderador}) {
            String bearer = sesion(actor);
            for (String recurso : new String[]{"/acceso", "/usuarios", "/estadisticas"}) {
                mvc.perform(get(RUTA + recurso).header("Authorization", bearer)).andExpect(status().isForbidden());
            }
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.auditoria_administracion WHERE actor_id IN (?,?,?)
                """, Long.class, usuario, propietario, moderador)).isZero();
        var admin = conceder();
        String autorizado = sesion(admin);
        mvc.perform(get(RUTA + "/acceso").header("Authorization", autorizado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permitido").value(true));
        mvc.perform(get("/api/v1/moderacion/reportes").header("Authorization", autorizado))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE identidad.permiso_administracion SET habilitado=false WHERE usuario_id=?", admin);
        mvc.perform(get(RUTA + "/usuarios").header("Authorization", autorizado)).andExpect(status().isForbidden());
    }

    @Test void paginaFiltraExponeSoloCamposMinimosYAudita() throws Exception {
        UUID administrador = conceder();
        String token = sesion(administrador);
        UUID inactivo = usuario("INACTIVO");
        for (int i = 0; i < 23; i++) usuario("ACTIVO");
        var primera = mvc.perform(get(RUTA + "/usuarios").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse();
        var pagina = json.readTree(primera.getContentAsString());
        var ids = new HashSet<String>();
        pagina.get("items").forEach(item -> ids.add(item.get("id").asText()));
        var siguiente = mvc.perform(get(RUTA + "/usuarios").header("Authorization", token)
                .param("cursor", pagina.get("siguienteCursor").asText())).andExpect(status().isOk())
                .andReturn().getResponse();
        var items = json.readTree(siguiente.getContentAsString()).get("items");
        assertThat(items.isEmpty()).isFalse();
        items.forEach(item -> assertThat(ids).doesNotContain(item.get("id").asText()));
        var encontrado = mvc.perform(get(RUTA + "/usuarios").header("Authorization", token)
                .param("estado", "INACTIVO").param("buscar", inactivo + "@administracion.invalid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(inactivo.toString()))
                .andReturn().getResponse();
        var campos = json.readTree(encontrado.getContentAsString()).get("items").get(0).properties();
        assertThat(campos.stream().map(java.util.Map.Entry::getKey)).containsExactlyInAnyOrder("id", "correo", "estado", "creadoEn");
        mvc.perform(get(RUTA + "/usuarios").header("Authorization", token).param("buscar", inactivo.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(inactivo.toString()));
        mvc.perform(get(RUTA + "/usuarios").header("Authorization", token).param("estado", "PENDIENTE"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM identidad.auditoria_administracion WHERE actor_id=? AND operacion='CONSULTAR_USUARIOS'",
                Long.class, administrador)).isEqualTo(4);
    }

    @Test void agregaCuentasYGanaderiasRealesYRegistraConsulta() throws Exception {
        var admin = conceder();
        usuario("INACTIVO");
        ganaderia(admin);
        long activas = jdbc.queryForObject("SELECT count(*) FROM identidad.usuario WHERE estado='ACTIVO'", Long.class);
        long cuentas = jdbc.queryForObject("SELECT count(*) FROM identidad.usuario", Long.class);
        long ganaderias = jdbc.queryForObject("SELECT count(*) FROM identidad.ganaderia", Long.class);
        long ganaderiasActivas = jdbc.queryForObject("SELECT count(*) FROM identidad.ganaderia WHERE estado='ACTIVA'", Long.class);
        mvc.perform(get(RUTA + "/estadisticas").header("Authorization", sesion(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cuentas").value((int) cuentas))
                .andExpect(jsonPath("$.cuentasActivas").value((int) activas))
                .andExpect(jsonPath("$.ganaderias").value((int) ganaderias))
                .andExpect(jsonPath("$.ganaderiasActivas").value((int) ganaderiasActivas));
        assertThat(jdbc.queryForObject("SELECT operacion FROM identidad.auditoria_administracion WHERE actor_id=?", String.class, admin))
                .isEqualTo("CONSULTAR_ESTADISTICAS");
    }

    private UUID usuario(String estado) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash,estado) VALUES (?,?,'$argon2id$fixture',?)",
                id, id + "@administracion.invalid", estado);
        return id;
    }

    private UUID conceder() {
        var id = usuario("ACTIVO");
        jdbc.update("INSERT INTO identidad.permiso_administracion(usuario_id,habilitado) VALUES (?,true)", id);
        return id;
    }

    private void ganaderia(UUID propietario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Prueba sintética',?)", id, propietario);
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,'PROPIETARIO')",
                UUID.randomUUID(), id, propietario);
    }

    private String sesion(UUID usuario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')", id, usuario);
        var ahora = Instant.now();
        return "Bearer " + tokens.emitir(usuario, id, ahora, ahora.plusSeconds(600)).valor();
    }
}
