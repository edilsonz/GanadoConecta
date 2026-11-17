package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class PostgresConectividadIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private TestRestTemplate rest;

    @Value("${DB_HOST:127.0.0.1}")
    private String databaseHost;

    @Value("${POSTGRES_PORT:5433}")
    private int publishedPort;

    @Value("${POSTGRES_DB:ganadoconecta}")
    private String databaseName;

    @Test
    void conectaAlPostgres17DeDockerPorElPuertoPublicado() {
        Map<String, Object> result = jdbc.queryForMap("""
                SELECT current_database() AS database_name,
                       inet_server_port() AS server_port,
                       current_setting('server_version_num')::integer AS server_version
                """);

        assertThat(dataSourceProperties.getUrl())
                .isEqualTo("jdbc:postgresql://%s:%d/%s".formatted(
                        databaseHost, publishedPort, databaseName));
        assertThat(databaseHost).isEqualTo("127.0.0.1");
        assertThat(publishedPort).isEqualTo(5433);
        assertThat(result.get("database_name")).isEqualTo("ganadoconecta");
        assertThat(result.get("server_port")).isEqualTo(5432);
        assertThat((Integer) result.get("server_version"))
                .isBetween(170000, 179999);
    }

    @Test
    void saludPermanecePublicaSinExponerDetallesDeLaBase() {
        ResponseEntity<String> response = rest.getForEntity("/api/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }
}
