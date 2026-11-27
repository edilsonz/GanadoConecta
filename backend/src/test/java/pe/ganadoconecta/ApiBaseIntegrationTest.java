package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
class ApiBaseIntegrationTest {
    @Autowired
    private TestRestTemplate rest;

    @Test
    void saludPublicaNoExponeComponentesNiConfiguracion() {
        ResponseEntity<String> response = rest.getForEntity("/api/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void noExponeRutasDeNegocioNiDetallesDeActuator() {
        for (String path : new String[]{"/api/animales", "/api/auth/me", "/api/env", "/api/health/detalles"}) {
            assertThat(rest.getForEntity(path, String.class).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void saludNoPermiteEscrituras() {
        assertThat(rest.postForEntity("/api/health", null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void noPublicaLaDocumentacionSinPerfilExplicito() {
        for (String ruta : new String[]{"/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs.yaml"}) {
            assertThat(rest.getForEntity(ruta, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
