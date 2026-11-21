package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class IdentidadSchemaIntegrationTest extends SoporteJwtPostgres {
    private static final String HASH_ARGON2ID_SINTETICO =
            "$argon2id$fixture-sintetica-no-utilizable";

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void creaYValidaLasMigracionesVersionadas() {
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

        assertThat(jdbc.queryForObject("""
                SELECT version
                FROM public.flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """, String.class)).isEqualTo("12");
        assertThat(jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'identidad'
                ORDER BY table_name
                """, String.class)).containsExactly(
                        "auditoria_membresia",
                        "ganaderia",
                        "intento_login",
                        "invitacion",
                        "membresia",
                        "sesion_dispositivo",
                        "usuario",
                        "verificador_renovacion");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'identidad'
                  AND table_name = 'usuario'
                """, String.class))
                .contains("password_hash")
                .doesNotContain("password", "contrasena");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'identidad'
                  AND table_name = 'verificador_renovacion'
                """, String.class))
                .contains("huella_sha256")
                .doesNotContain("token", "token_renovacion");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'identidad'
                  AND table_name = 'intento_login'
                """, String.class))
                .contains("clave_sha256", "fallos", "bloqueado_hasta")
                .doesNotContain("correo", "origen");
        assertThat(jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'ganado'
                ORDER BY table_name
                """, String.class)).containsExactly(
                        "animal", "operacion_confirmada");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'ganado' AND table_name = 'animal'
                """, String.class)).contains(
                        "id", "ganaderia_id", "sexo", "version", "creado_por",
                        "colores_identificacion_textil",
                        "ubicacion_identificacion_textil");
        assertThat(jdbc.queryForObject("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'ganado'
                  AND indexname = 'ix_animal_ganaderia_listado'
                """, String.class)).contains(
                        "ganaderia_id", "creado_en DESC", "id DESC");
    }

    @Test
    void exigeEmailUnicoYHashArgon2id() throws SQLException {
        try (Connection connection = transaction()) {
            UUID firstUser = UUID.randomUUID();
            insertUser(connection, firstUser, "persona-prueba@ganadoconecta.invalid",
                    HASH_ARGON2ID_SINTETICO);

            SQLException duplicate = catchThrowableOfType(
                    () -> insertUser(connection, UUID.randomUUID(),
                            "persona-prueba@ganadoconecta.invalid", HASH_ARGON2ID_SINTETICO),
                    SQLException.class);
            assertThat(duplicate.getSQLState()).isEqualTo("23505");
            connection.rollback();
        }

        try (Connection connection = transaction()) {
            SQLException plainText = catchThrowableOfType(
                    () -> insertUser(connection, UUID.randomUUID(),
                            "otra-persona@ganadoconecta.invalid", "texto-plano-no-permitido"),
                    SQLException.class);
            assertThat(plainText.getSQLState()).isEqualTo("23514");
            connection.rollback();
        }
    }

    @Test
    void exigeMembresiaPropietarioActivaAlConfirmarGanaderia() throws SQLException {
        try (Connection connection = transaction()) {
            UUID ownerId = UUID.randomUUID();
            UUID farmId = UUID.randomUUID();
            insertUser(connection, ownerId, uniqueEmail(ownerId), HASH_ARGON2ID_SINTETICO);
            insertFarm(connection, farmId, ownerId);
            insertMembership(connection, UUID.randomUUID(), farmId, ownerId,
                    "PROPIETARIO", "ACTIVA");
            forceDeferredConstraints(connection);
            connection.rollback();
        }

        try (Connection connection = transaction()) {
            UUID ownerId = UUID.randomUUID();
            insertUser(connection, ownerId, uniqueEmail(ownerId), HASH_ARGON2ID_SINTETICO);
            insertFarm(connection, UUID.randomUUID(), ownerId);

            SQLException missingOwnerMembership = catchThrowableOfType(
                    () -> forceDeferredConstraints(connection), SQLException.class);
            assertThat(missingOwnerMembership.getSQLState()).isEqualTo("23514");
            connection.rollback();
        }
    }

    @Test
    void impideDosMembresiasParaElMismoUsuarioYGanaderia() throws SQLException {
        try (Connection connection = transaction()) {
            UUID ownerId = UUID.randomUUID();
            UUID farmId = UUID.randomUUID();
            insertUser(connection, ownerId, uniqueEmail(ownerId), HASH_ARGON2ID_SINTETICO);
            insertFarm(connection, farmId, ownerId);
            insertMembership(connection, UUID.randomUUID(), farmId, ownerId,
                    "PROPIETARIO", "ACTIVA");

            SQLException duplicate = catchThrowableOfType(
                    () -> insertMembership(connection, UUID.randomUUID(), farmId, ownerId,
                            "EDITOR", "ACTIVA"), SQLException.class);
            assertThat(duplicate.getSQLState()).isEqualTo("23505");
            connection.rollback();
        }
    }

    @Test
    void impideSegundoPropietarioActivoYReferenciasHuerfanas() throws SQLException {
        try (Connection connection = transaction()) {
            UUID ownerId = UUID.randomUUID();
            UUID secondOwnerId = UUID.randomUUID();
            UUID farmId = UUID.randomUUID();
            insertUser(connection, ownerId, uniqueEmail(ownerId), HASH_ARGON2ID_SINTETICO);
            insertUser(connection, secondOwnerId, uniqueEmail(secondOwnerId),
                    HASH_ARGON2ID_SINTETICO);
            insertFarm(connection, farmId, ownerId);
            insertMembership(connection, UUID.randomUUID(), farmId, ownerId,
                    "PROPIETARIO", "ACTIVA");

            SQLException secondOwner = catchThrowableOfType(
                    () -> insertMembership(connection, UUID.randomUUID(), farmId,
                            secondOwnerId, "PROPIETARIO", "ACTIVA"), SQLException.class);
            assertThat(secondOwner.getSQLState()).isEqualTo("23505");
            connection.rollback();
        }

        try (Connection connection = transaction()) {
            UUID userId = UUID.randomUUID();
            insertUser(connection, userId, uniqueEmail(userId), HASH_ARGON2ID_SINTETICO);

            SQLException orphanMembership = catchThrowableOfType(
                    () -> insertMembership(connection, UUID.randomUUID(), UUID.randomUUID(),
                            userId, "LECTOR", "ACTIVA"), SQLException.class);
            assertThat(orphanMembership.getSQLState()).isEqualTo("23503");
            connection.rollback();
        }
    }

    @Test
    void conservaSoloHuellasSha256YUnVerificadorActivoPorSesion() throws SQLException {
        try (Connection connection = transaction()) {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            insertUser(connection, userId, uniqueEmail(userId), HASH_ARGON2ID_SINTETICO);
            insertSession(connection, sessionId, userId);
            insertVerifier(connection, UUID.randomUUID(), sessionId, new byte[32]);

            byte[] anotherFingerprint = new byte[32];
            anotherFingerprint[0] = 1;
            SQLException secondActive = catchThrowableOfType(
                    () -> insertVerifier(connection, UUID.randomUUID(), sessionId,
                            anotherFingerprint), SQLException.class);
            assertThat(secondActive.getSQLState()).isEqualTo("23505");
            connection.rollback();
        }

        try (Connection connection = transaction()) {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            insertUser(connection, userId, uniqueEmail(userId), HASH_ARGON2ID_SINTETICO);
            insertSession(connection, sessionId, userId);

            SQLException invalidFingerprint = catchThrowableOfType(
                    () -> insertVerifier(connection, UUID.randomUUID(), sessionId,
                            new byte[31]), SQLException.class);
            assertThat(invalidFingerprint.getSQLState()).isEqualTo("23514");
            connection.rollback();
        }
    }

    @Test
    void limitaLaSesionOnlineAUnMaximoDeVeinticuatroHoras() throws SQLException {
        try (Connection connection = transaction()) {
            UUID userId = UUID.randomUUID();
            insertUser(connection, userId, uniqueEmail(userId), HASH_ARGON2ID_SINTETICO);

            SQLException excessiveDuration = catchThrowableOfType(
                    () -> insertSession(connection, UUID.randomUUID(), userId, 25),
                    SQLException.class);
            assertThat(excessiveDuration.getSQLState()).isEqualTo("23514");
            connection.rollback();
        }
    }

    private Connection transaction() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private String uniqueEmail(UUID id) {
        return id + "@ganadoconecta.invalid";
    }

    private void insertUser(Connection connection, UUID id, String email, String passwordHash)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO identidad.usuario (id, email_normalizado, password_hash)
                VALUES (?, ?, ?)
                """)) {
            statement.setObject(1, id);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.executeUpdate();
        }
    }

    private void insertFarm(Connection connection, UUID farmId, UUID ownerId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO identidad.ganaderia (id, nombre, propietario_usuario_id)
                VALUES (?, 'Ganaderia sintetica', ?)
                """)) {
            statement.setObject(1, farmId);
            statement.setObject(2, ownerId);
            statement.executeUpdate();
        }
    }

    private void insertMembership(Connection connection, UUID membershipId, UUID farmId,
            UUID userId, String role, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO identidad.membresia
                    (id, ganaderia_id, usuario_id, rol, estado)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, membershipId);
            statement.setObject(2, farmId);
            statement.setObject(3, userId);
            statement.setString(4, role);
            statement.setString(5, status);
            statement.executeUpdate();
        }
    }

    private void insertSession(Connection connection, UUID sessionId, UUID userId)
            throws SQLException {
        insertSession(connection, sessionId, userId, 24);
    }

    private void insertSession(Connection connection, UUID sessionId, UUID userId,
            int durationHours) throws SQLException {
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO identidad.sesion_dispositivo
                    (id, usuario_id, creado_en, vence_en)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setObject(1, sessionId);
            statement.setObject(2, userId);
            statement.setObject(3, createdAt);
            statement.setObject(4, createdAt.plusHours(durationHours));
            statement.executeUpdate();
        }
    }

    private void insertVerifier(Connection connection, UUID verifierId, UUID sessionId,
            byte[] fingerprint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO identidad.verificador_renovacion
                    (id, sesion_id, huella_sha256)
                VALUES (?, ?, ?)
                """)) {
            statement.setObject(1, verifierId);
            statement.setObject(2, sessionId);
            statement.setBytes(3, fingerprint);
            statement.executeUpdate();
        }
    }

    private void forceDeferredConstraints(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET CONSTRAINTS ALL IMMEDIATE");
        }
    }
}
