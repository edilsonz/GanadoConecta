package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"postgres", "openapi"})
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class OpenApiPostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping rutas;

    @Test
    void elContratoIncluyeCadaOperacionRealYResuelveLasReferencias() throws Exception {
        JsonNode contrato = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(contrato.path("openapi").asText()).startsWith("3.0.");
        var esperadas = new TreeSet<String>();
        rutas.getHandlerMethods().keySet().forEach(ruta -> ruta.getPatternValues().stream()
                .filter(path -> path.startsWith("/api/v1/"))
                .forEach(path -> ruta.getMethodsCondition().getMethods().forEach(
                        metodo -> esperadas.add(metodo.name().toLowerCase(Locale.ROOT) + " " + path))));
        esperadas.add("get /api/health");
        var documentadas = new TreeSet<String>();
        var ids = new TreeSet<String>();
        var verbos = Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");
        contrato.path("paths").properties().forEach(ruta -> ruta.getValue().properties().stream()
                .filter(operacion -> verbos.contains(operacion.getKey())).forEach(operacion -> {
                    documentadas.add(operacion.getKey() + " " + ruta.getKey());
                    var valor = operacion.getValue();
                    assertThat(valor.path("summary").asText()).isNotBlank();
                    assertThat(valor.path("description").asText()).isNotBlank();
                    assertThat(ids.add(valor.path("operationId").asText())).isTrue();
                    assertThat(valor.path("parameters")).allSatisfy(parametro ->
                            assertThat(parametro.path("name").asText()).isNotIn("csrfToken", "parametros"));
                }));
        assertThat(documentadas).isEqualTo(esperadas);
        assertThat(contrato.findValuesAsText("$ref")).isNotEmpty().allSatisfy(referencia ->
                assertThat(contrato.at(referencia.substring(1)).isMissingNode()).as(referencia).isFalse());
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
    }

    @Test
    void swaggerNoModificaElControlDeAccesoNiCsrf() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ganaderias")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/registro").header("Origin", "http://127.0.0.1:5173")
                .contentType("application/json").content("{}")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/csrf'].get.parameters").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/ganaderias/{ganaderiaId}/animales'].get.parameters[*].name")
                        .value(org.hamcrest.Matchers.hasItems("limit", "cursor")))
                .andExpect(jsonPath("$.paths['/api/v1/ganaderias'].post.security[0].sesionJWT").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/ganaderias'].post.security[0].cookieCSRF").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/renovar'].post.security[0].cookieRenovacion").isArray());
    }
}
