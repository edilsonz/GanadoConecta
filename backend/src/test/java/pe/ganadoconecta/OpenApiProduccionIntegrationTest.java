package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles({"openapi", "production"})
class OpenApiProduccionIntegrationTest {
    @Autowired TestRestTemplate rest;

    @Test
    void losPerfilesDeProduccionBloqueanLaDocumentacionAunqueSeSoliciteOpenApi() {
        for (String ruta : new String[]{"/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs.yaml"}) {
            assertThat(rest.getForEntity(ruta, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(rest.getForEntity("/api/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
