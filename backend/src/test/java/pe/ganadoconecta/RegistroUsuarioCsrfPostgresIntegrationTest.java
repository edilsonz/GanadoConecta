package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class RegistroUsuarioCsrfPostgresIntegrationTest {
    private static final String PREFIJO = "registro-csrf-test-";
    private static final String ORIGEN = "http://127.0.0.1:5173";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void eliminarDatosSinteticos() {
        jdbc.update("DELETE FROM identidad.usuario WHERE email_normalizado LIKE ?",
                PREFIJO + "%@ganadoconecta.invalid");
    }

    @Test
    void obtieneCsrfYRegistraPorHttpRealConPersistenciaPostgres() throws Exception {
        ResponseEntity<String> csrf = rest.getForEntity("/api/v1/auth/csrf", String.class);
        assertThat(csrf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csrf.getHeaders().getCacheControl()).contains("no-store");

        String setCookie = csrf.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .isNotBlank()
                .contains("__Host-GC-CSRF=")
                .contains("Path=/")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Strict");
        String cookie = setCookie.substring(0, setCookie.indexOf(';'));
        JsonNode cuerpoCsrf = objectMapper.readTree(csrf.getBody());
        String token = cuerpoCsrf.path("token").asText();
        assertThat(token).isNotBlank();

        String correo = PREFIJO + UUID.randomUUID() + "@ganadoconecta.invalid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setOrigin(ORIGEN);
        headers.set(HttpHeaders.COOKIE, cookie);
        headers.set("X-CSRF-TOKEN", token);
        String solicitud = objectMapper.writeValueAsString(java.util.Map.of(
                "correo", correo,
                "contrasena", "contraseña-sintética-segura"));

        ResponseEntity<String> registro = rest.exchange(
                "/api/v1/auth/registro",
                HttpMethod.POST,
                new HttpEntity<>(solicitud, headers),
                String.class);

        assertThat(registro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registro.getHeaders().getCacheControl()).isEqualTo("no-store");
        JsonNode cuerpoRegistro = objectMapper.readTree(registro.getBody());
        assertThat(cuerpoRegistro.path("correo").asText()).isEqualTo(correo);
        assertThat(cuerpoRegistro.path("estado").asText()).isEqualTo("ACTIVO");
        assertThat(cuerpoRegistro.has("contrasena")).isFalse();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.usuario WHERE email_normalizado = ?
                """, Integer.class, correo)).isEqualTo(1);
    }
}
