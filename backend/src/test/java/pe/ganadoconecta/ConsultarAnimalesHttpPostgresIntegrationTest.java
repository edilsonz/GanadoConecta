package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=WARN"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class ConsultarAnimalesHttpPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String PREFIJO = "consulta-animal-test-";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transacciones;
    @Autowired
    private TokensAcceso tokens;

    @AfterEach
    void limpiarDatosSinteticos() {
        transacciones.executeWithoutResult(estado -> {
            jdbc.update("""
                    DELETE FROM ganado.operacion_confirmada o
                    USING identidad.usuario u
                    WHERE o.actor_usuario_id = u.id
                      AND u.email_normalizado LIKE ?
                    """, PREFIJO + "%@ganadoconecta.invalid");
            jdbc.update("""
                    DELETE FROM ganado.animal a
                    USING identidad.usuario u
                    WHERE a.creado_por = u.id
                      AND u.email_normalizado LIKE ?
                    """, PREFIJO + "%@ganadoconecta.invalid");
            jdbc.update("""
                    DELETE FROM identidad.sesion_dispositivo s
                    USING identidad.usuario u
                    WHERE s.usuario_id = u.id AND u.email_normalizado LIKE ?
                    """, PREFIJO + "%@ganadoconecta.invalid");
            jdbc.update("""
                    DELETE FROM identidad.membresia m
                    USING identidad.usuario u
                    WHERE m.usuario_id = u.id AND u.email_normalizado LIKE ?
                    """, PREFIJO + "%@ganadoconecta.invalid");
            jdbc.update("""
                    DELETE FROM identidad.ganaderia g
                    USING identidad.usuario u
                    WHERE g.propietario_usuario_id = u.id
                      AND u.email_normalizado LIKE ?
                    """, PREFIJO + "%@ganadoconecta.invalid");
            jdbc.update("DELETE FROM identidad.usuario WHERE email_normalizado LIKE ?",
                    PREFIJO + "%@ganadoconecta.invalid");
        });
    }

    @Test
    void listadoVacioUsaContratoYNoStore() throws Exception {
        Cuenta propietario = crearCuenta("vacio");
        UUID ganaderiaId = crearGanaderia(propietario);

        listar(propietario.jwt(), ganaderiaId, "")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.siguienteCursor").isEmpty())
                .andExpect(jsonPath("$.consultadoEn").isString());
    }

    @Test
    void paginaEnOrdenEstableSinOmisionesNiDuplicados() throws Exception {
        Cuenta propietario = crearCuenta("pagina");
        UUID ganaderiaId = crearGanaderia(propietario);
        Instant instante = Instant.parse("2026-11-18T20:00:00Z");
        UUID primero = UUID.fromString("00000000-0000-4000-8000-000000000003");
        UUID segundo = UUID.fromString("00000000-0000-4000-8000-000000000002");
        UUID tercero = UUID.fromString("00000000-0000-4000-8000-000000000001");
        insertarAnimal(ganaderiaId, propietario.usuarioId(), tercero, instante, "Tercera");
        insertarAnimal(ganaderiaId, propietario.usuarioId(), primero, instante, "Primera");
        insertarAnimal(ganaderiaId, propietario.usuarioId(), segundo, instante, "Segunda");

        JsonNode pagina1 = json(listar(propietario.jwt(), ganaderiaId, "?limit=2")
                .andExpect(status().isOk()).andReturn());
        assertThat(pagina1.at("/items/0/animalId").asText()).isEqualTo(primero.toString());
        assertThat(pagina1.at("/items/1/animalId").asText()).isEqualTo(segundo.toString());
        String cursor = pagina1.get("siguienteCursor").asText();
        assertThat(cursor).isNotBlank();

        JsonNode pagina2 = json(listar(propietario.jwt(), ganaderiaId,
                "?limit=2&cursor=" + cursor).andExpect(status().isOk()).andReturn());
        assertThat(pagina2.get("items")).hasSize(1);
        assertThat(pagina2.at("/items/0/animalId").asText()).isEqualTo(tercero.toString());
        assertThat(pagina2.get("siguienteCursor").isNull()).isTrue();
    }

    @Test
    void fichaDevuelveSoloCamposAprobadosDeLaGanaderia() throws Exception {
        Cuenta propietario = crearCuenta("ficha");
        UUID ganaderiaId = crearGanaderia(propietario);
        UUID animalId = UUID.randomUUID();
        insertarAnimal(ganaderiaId, propietario.usuarioId(), animalId,
                Instant.parse("2026-11-18T19:00:00Z"), "Peña");

        ficha(propietario.jwt(), ganaderiaId, animalId, "")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.ganaderiaId").value(ganaderiaId.toString()))
                .andExpect(jsonPath("$.sexo").value("HEMBRA"))
                .andExpect(jsonPath("$.nombre").value("Peña"))
                .andExpect(jsonPath("$.coloresIdentificacionTextil").isEmpty())
                .andExpect(jsonPath("$.ubicacionIdentificacionTextil").isEmpty())
                .andExpect(jsonPath("$.categoria").value("VAQUILLA"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.creadoEn").isString())
                .andExpect(jsonPath("$.creadoPor").doesNotExist());
    }

    @Test
    void propietarioEditorYLectorActivosPuedenConsultar() throws Exception {
        Cuenta propietario = crearCuenta("roles-propietario");
        Cuenta editor = crearCuenta("roles-editor");
        Cuenta lector = crearCuenta("roles-lector");
        UUID ganaderiaId = crearGanaderia(propietario);
        crearMembresia(ganaderiaId, editor.usuarioId(), "EDITOR");
        crearMembresia(ganaderiaId, lector.usuarioId(), "LECTOR");
        UUID animalId = UUID.randomUUID();
        insertarAnimal(ganaderiaId, propietario.usuarioId(), animalId,
                Instant.now().minusSeconds(10), "Visible");

        for (Cuenta cuenta : new Cuenta[]{propietario, editor, lector}) {
            listar(cuenta.jwt(), ganaderiaId, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].animalId").value(animalId.toString()));
            ficha(cuenta.jwt(), ganaderiaId, animalId, "")
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rechazaMiembroRevocadoYUsuarioAjenoSinRevelarDatos() throws Exception {
        Cuenta propietario = crearCuenta("aislamiento-propietario");
        Cuenta lector = crearCuenta("aislamiento-lector");
        Cuenta ajeno = crearCuenta("aislamiento-ajeno");
        UUID ganaderiaId = crearGanaderia(propietario);
        crearMembresia(ganaderiaId, lector.usuarioId(), "LECTOR");
        revocar(ganaderiaId, lector.usuarioId());

        for (Cuenta cuenta : new Cuenta[]{lector, ajeno}) {
            listar(cuenta.jwt(), ganaderiaId, "")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo")
                            .value("ACCESO_GANADERIA_DENEGADO"))
                    .andExpect(jsonPath("$.ganaderiaId").doesNotExist());
        }
    }

    @Test
    void animalInexistenteODeOtraGanaderiaDevuelveElMismo404() throws Exception {
        Cuenta propietario = crearCuenta("no-encontrado");
        UUID ganaderiaId = crearGanaderia(propietario);
        UUID otraGanaderiaId = crearGanaderia(propietario);
        UUID animalAjenoAlAmbito = UUID.randomUUID();
        insertarAnimal(otraGanaderiaId, propietario.usuarioId(), animalAjenoAlAmbito,
                Instant.now().minusSeconds(10), "Otra");

        for (UUID animalId : new UUID[]{UUID.randomUUID(), animalAjenoAlAmbito}) {
            ficha(propietario.jwt(), ganaderiaId, animalId, "")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("ANIMAL_NO_ENCONTRADO"))
                    .andExpect(jsonPath("$.animalId").doesNotExist());
        }
    }

    @Test
    void exigeAutenticacionYValidaParametrosDeConsulta() throws Exception {
        Cuenta propietario = crearCuenta("contrato");
        UUID ganaderiaId = crearGanaderia(propietario);
        listar(null, ganaderiaId, "")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"));

        String cursorConFechaInvalida = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("no-es-fecha|" + UUID.randomUUID())
                        .getBytes(StandardCharsets.UTF_8));
        for (String consulta : new String[]{
                "?limit=0", "?limit=101", "?limit=no-numero", "?cursor=invalido",
                "?cursor=" + cursorConFechaInvalida,
                "?orden=nombre", "?limit=2&limit=3"}) {
            listar(propietario.jwt(), ganaderiaId, consulta)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        }
        ficha(propietario.jwt(), ganaderiaId, UUID.randomUUID(), "?extra=1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    private org.springframework.test.web.servlet.ResultActions listar(
            String jwt, UUID ganaderiaId, String consulta) throws Exception {
        var solicitud = get("/api/v1/ganaderias/" + ganaderiaId + "/animales" + consulta);
        if (jwt != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        return mvc.perform(solicitud);
    }

    private org.springframework.test.web.servlet.ResultActions ficha(
            String jwt, UUID ganaderiaId, UUID animalId, String consulta) throws Exception {
        var solicitud = get("/api/v1/ganaderias/" + ganaderiaId
                + "/animales/" + animalId + consulta);
        if (jwt != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        return mvc.perform(solicitud);
    }

    private Cuenta crearCuenta(String sufijo) {
        UUID usuarioId = UUID.randomUUID();
        UUID sesionId = UUID.randomUUID();
        Instant ahora = Instant.now();
        transacciones.executeWithoutResult(estado -> {
            jdbc.update("""
                    INSERT INTO identidad.usuario
                        (id, email_normalizado, password_hash, estado)
                    VALUES (?, ?, '$argon2id$fixture-sintetica', 'ACTIVO')
                    """, usuarioId,
                    PREFIJO + sufijo + "-" + usuarioId + "@ganadoconecta.invalid");
            jdbc.update("""
                    INSERT INTO identidad.sesion_dispositivo
                        (id, usuario_id, creado_en, vence_en)
                    VALUES (?, ?, ?, ?)
                    """, sesionId, usuarioId,
                    OffsetDateTime.ofInstant(ahora.minusSeconds(1), ZoneOffset.UTC),
                    OffsetDateTime.ofInstant(ahora.plusSeconds(3600), ZoneOffset.UTC));
        });
        return new Cuenta(usuarioId,
                tokens.emitir(usuarioId, sesionId, ahora, ahora.plusSeconds(600)).valor());
    }

    private UUID crearGanaderia(Cuenta propietario) {
        UUID ganaderiaId = UUID.randomUUID();
        transacciones.executeWithoutResult(estado -> {
            jdbc.update("""
                    INSERT INTO identidad.ganaderia
                        (id, nombre, propietario_usuario_id, estado)
                    VALUES (?, 'Ganadería consulta sintética', ?, 'ACTIVA')
                    """, ganaderiaId, propietario.usuarioId());
            crearMembresia(ganaderiaId, propietario.usuarioId(), "PROPIETARIO");
        });
        return ganaderiaId;
    }

    private void crearMembresia(UUID ganaderiaId, UUID usuarioId, String rol) {
        jdbc.update("""
                INSERT INTO identidad.membresia
                    (id, ganaderia_id, usuario_id, rol, estado)
                VALUES (?, ?, ?, ?, 'ACTIVA')
                """, UUID.randomUUID(), ganaderiaId, usuarioId, rol);
    }

    private void revocar(UUID ganaderiaId, UUID usuarioId) {
        jdbc.update("""
                UPDATE identidad.membresia
                SET estado = 'REVOCADA', revocado_en = CURRENT_TIMESTAMP,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, ganaderiaId, usuarioId);
    }

    private void insertarAnimal(UUID ganaderiaId, UUID creadoPor, UUID animalId,
            Instant creadoEn, String nombre) {
        jdbc.update("""
                INSERT INTO ganado.animal (
                    id, ganaderia_id, sexo, nombre, raza, fecha_nacimiento,
                    nacimiento_estimado, categoria, observaciones, estado,
                    version, creado_por, creado_en
                ) VALUES (?, ?, 'HEMBRA', ?, 'Criolla', ?, false, 'VAQUILLA',
                          'Dato sintético', 'ACTIVO', 1, ?, ?)
                """, animalId, ganaderiaId, nombre, LocalDate.of(2025, 1, 10),
                creadoPor, OffsetDateTime.ofInstant(creadoEn, ZoneOffset.UTC));
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsByteArray());
    }

    private record Cuenta(UUID usuarioId, String jwt) {
    }
}
