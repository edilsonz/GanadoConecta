package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
class RegistrarAnimalHttpPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String ORIGEN = "http://127.0.0.1:5173";
    private static final String PREFIJO = "animal-http-test-";

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
    void propietarioRegistraAnimalConTodosLosCamposValidos() throws Exception {
        Cuenta propietario = crearCuenta("propietario");
        UUID ganaderiaId = crearGanaderia(propietario);
        UUID animalId = UUID.randomUUID();
        UUID operacionId = UUID.randomUUID();
        Map<String, Object> cuerpo = cuerpo(animalId, operacionId);

        registrar(propietario.jwt(), ganaderiaId, cuerpo, true, ORIGEN)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.operationId").value(operacionId.toString()))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.resultado").value("APLICADO"))
                .andExpect(jsonPath("$.versionCreada").value(1))
                .andExpect(jsonPath("$.confirmadoEn").isString())
                .andExpect(jsonPath("$.advertencias").isEmpty());

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM ganado.animal
                WHERE id = ? AND ganaderia_id = ? AND creado_por = ?
                  AND sexo = 'HEMBRA' AND nombre = 'Luna'
                  AND categoria = 'VAQUILLA' AND version = 1
                """, Integer.class, animalId, ganaderiaId, propietario.usuarioId()))
                .isOne();
    }

    @Test
    void repeticionIdenticaDevuelveReciboOriginal() throws Exception {
        Cuenta propietario = crearCuenta("replay");
        UUID ganaderiaId = crearGanaderia(propietario);
        Map<String, Object> cuerpo = cuerpo(UUID.randomUUID(), UUID.randomUUID());

        JsonNode primero = json(registrar(propietario.jwt(), ganaderiaId, cuerpo,
                true, ORIGEN).andExpect(status().isCreated()).andReturn());
        JsonNode segundo = json(registrar(propietario.jwt(), ganaderiaId, cuerpo,
                true, ORIGEN).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("REPROCESADO"))
                .andReturn());

        assertThat(segundo.get("confirmadoEn").asText())
                .isEqualTo(primero.get("confirmadoEn").asText());
        assertThat(cantidadAnimales(ganaderiaId)).isOne();
    }

    @Test
    void propietarioYEditorEscribenPeroLectorAjenoYRevocadoNo() throws Exception {
        Cuenta propietario = crearCuenta("roles-propietario");
        Cuenta editor = crearCuenta("roles-editor");
        Cuenta lector = crearCuenta("roles-lector");
        Cuenta ajeno = crearCuenta("roles-ajeno");
        UUID ganaderiaId = crearGanaderia(propietario);
        crearMembresia(ganaderiaId, editor.usuarioId(), "EDITOR");
        crearMembresia(ganaderiaId, lector.usuarioId(), "LECTOR");

        registrar(editor.jwt(), ganaderiaId,
                cuerpo(UUID.randomUUID(), UUID.randomUUID()), true, ORIGEN)
                .andExpect(status().isCreated());
        registrar(lector.jwt(), ganaderiaId,
                cuerpo(UUID.randomUUID(), UUID.randomUUID()), true, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ROL_INSUFICIENTE"));
        registrar(ajeno.jwt(), ganaderiaId,
                cuerpo(UUID.randomUUID(), UUID.randomUUID()), true, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_GANADERIA_DENEGADO"));

        revocar(ganaderiaId, editor.usuarioId());
        registrar(editor.jwt(), ganaderiaId,
                cuerpo(UUID.randomUUID(), UUID.randomUUID()), true, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("MEMBRESIA_REVOCADA"));
        assertThat(cantidadAnimales(ganaderiaId)).isOne();
    }

    @Test
    void exigeAutenticacionCsrfYOrigenPermitido() throws Exception {
        Cuenta propietario = crearCuenta("seguridad");
        UUID ganaderiaId = crearGanaderia(propietario);
        Map<String, Object> cuerpo = cuerpo(UUID.randomUUID(), UUID.randomUUID());

        registrar(null, ganaderiaId, cuerpo, true, ORIGEN)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"));
        registrar(propietario.jwt(), ganaderiaId, cuerpo, false, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        registrar(propietario.jwt(), ganaderiaId, cuerpo, true, "https://ajeno.invalid")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));
        assertThat(cantidadAnimales(ganaderiaId)).isZero();
    }

    @Test
    void rechazaSexoAusenteNuloVacioODesconocidoSinAlta() throws Exception {
        Cuenta propietario = crearCuenta("sexo");
        UUID ganaderiaId = crearGanaderia(propietario);
        for (Object sexo : new Object[]{null, "", "OTRO"}) {
            Map<String, Object> cuerpo = cuerpo(UUID.randomUUID(), UUID.randomUUID());
            cuerpo.put("sexo", sexo);
            registrar(propietario.jwt(), ganaderiaId, cuerpo, true, ORIGEN)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                    .andExpect(jsonPath("$.erroresPorCampo.sexo").exists());
        }
        Map<String, Object> ausente = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        ausente.remove("sexo");
        registrar(propietario.jwt(), ganaderiaId, ausente, true, ORIGEN)
                .andExpect(status().isUnprocessableEntity());
        assertThat(cantidadAnimales(ganaderiaId)).isZero();
    }

    @Test
    void aplicaCodigosDeEsquemaVersionFormatoYCamposNoAdmitidos() throws Exception {
        Cuenta propietario = crearCuenta("contrato");
        UUID ganaderiaId = crearGanaderia(propietario);

        Map<String, Object> esquema = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        esquema.put("schemaVersion", 2);
        registrar(propietario.jwt(), ganaderiaId, esquema, true, ORIGEN)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ESQUEMA_NO_SOPORTADO"));

        Map<String, Object> version = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        version.put("baseVersion", 1);
        registrar(propietario.jwt(), ganaderiaId, version, true, ORIGEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_INCOMPATIBLE"));

        Map<String, Object> extra = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        extra.put("actorUsuarioId", propietario.usuarioId());
        registrar(propietario.jwt(), ganaderiaId, extra, true, ORIGEN)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CAMPO_NO_ADMITIDO"));

        String uuidInvalido = objectMapper.writeValueAsString(
                cuerpo(UUID.randomUUID(), UUID.randomUUID()))
                .replaceFirst("\"animalId\":\"[^\"]+\"", "\"animalId\":\"no-uuid\"");
        registrarJson(propietario.jwt(), ganaderiaId, uuidInvalido, true, ORIGEN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));

        mvc.perform(post("/api/v1/ganaderias/{ganaderiaId}/animales?orden=creadoEn",
                        ganaderiaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                cuerpo(UUID.randomUUID(), UUID.randomUUID())))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + propietario.jwt())
                        .header(HttpHeaders.ORIGIN, ORIGEN)
                        .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        assertThat(cantidadAnimales(ganaderiaId)).isZero();
    }

    @Test
    void rechazaValidacionesDeCamposOpcionales() throws Exception {
        Cuenta propietario = crearCuenta("validaciones");
        UUID ganaderiaId = crearGanaderia(propietario);

        Map<String, Object> categoria = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        categoria.put("categoria", "TORO");
        Map<String, Object> fecha = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        fecha.put("fechaNacimiento", "2999-01-01");
        Map<String, Object> nombre = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        nombre.put("nombre", "x".repeat(101));
        Map<String, Object> estimada = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        estimada.remove("nacimientoEstimado");

        for (Map<String, Object> invalido : java.util.List.of(
                categoria, fecha, nombre, estimada)) {
            registrar(propietario.jwt(), ganaderiaId, invalido, true, ORIGEN)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
        assertThat(cantidadAnimales(ganaderiaId)).isZero();
    }

    @Test
    void conflictosNoRevelanElRegistroExistente() throws Exception {
        Cuenta propietario = crearCuenta("conflictos");
        UUID ganaderiaId = crearGanaderia(propietario);
        UUID animalId = UUID.randomUUID();
        UUID operacionId = UUID.randomUUID();
        Map<String, Object> original = cuerpo(animalId, operacionId);
        registrar(propietario.jwt(), ganaderiaId, original, true, ORIGEN)
                .andExpect(status().isCreated());

        Map<String, Object> cambiado = new LinkedHashMap<>(original);
        cambiado.put("nombre", "Otro nombre");
        registrar(propietario.jwt(), ganaderiaId, cambiado, true, ORIGEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"))
                .andExpect(jsonPath("$.animalId").doesNotExist());

        Map<String, Object> otraOperacion = new LinkedHashMap<>(original);
        otraOperacion.put("operationId", UUID.randomUUID());
        registrar(propietario.jwt(), ganaderiaId, otraOperacion, true, ORIGEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
        assertThat(cantidadAnimales(ganaderiaId)).isOne();
    }

    @Test
    void limitaElCuerpoA16KiBYAdvierteSoloAreteFrontal() throws Exception {
        Cuenta propietario = crearCuenta("limites");
        UUID ganaderiaId = crearGanaderia(propietario);
        Map<String, Object> demasiadoGrande = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        demasiadoGrande.put("observaciones", "x".repeat(17 * 1024));
        registrar(propietario.jwt(), ganaderiaId, demasiadoGrande, true, ORIGEN)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("CUERPO_DEMASIADO_GRANDE"));

        Map<String, Object> primero = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        primero.put("areteFrente", "A-10");
        registrar(propietario.jwt(), ganaderiaId, primero, true, ORIGEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advertencias").isEmpty());

        Map<String, Object> segundo = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        segundo.put("areteFrente", "a-10");
        registrar(propietario.jwt(), ganaderiaId, segundo, true, ORIGEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advertencias[0]").value("ARETE_COINCIDENTE"));

        Map<String, Object> reverso = cuerpo(UUID.randomUUID(), UUID.randomUUID());
        reverso.put("areteReverso", "A-10");
        registrar(propietario.jwt(), ganaderiaId, reverso, true, ORIGEN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advertencias").isEmpty());
        assertThat(cantidadAnimales(ganaderiaId)).isEqualTo(3);
    }

    private org.springframework.test.web.servlet.ResultActions registrar(
            String jwt, UUID ganaderiaId, Map<String, Object> cuerpo,
            boolean conCsrf, String origen) throws Exception {
        return registrarJson(jwt, ganaderiaId, objectMapper.writeValueAsString(cuerpo),
                conCsrf, origen);
    }

    private org.springframework.test.web.servlet.ResultActions registrarJson(
            String jwt, UUID ganaderiaId, String cuerpo,
            boolean conCsrf, String origen) throws Exception {
        var solicitud = post("/api/v1/ganaderias/{ganaderiaId}/animales", ganaderiaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
        if (jwt != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        if (origen != null) {
            solicitud.header(HttpHeaders.ORIGIN, origen);
        }
        if (conCsrf) {
            solicitud.with(csrf().asHeader());
        }
        return mvc.perform(solicitud);
    }

    private Map<String, Object> cuerpo(UUID animalId, UUID operacionId) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("operationId", operacionId);
        cuerpo.put("animalId", animalId);
        cuerpo.put("tipo", "CREAR_ANIMAL");
        cuerpo.put("schemaVersion", 1);
        cuerpo.put("baseVersion", 0);
        cuerpo.put("sexo", "HEMBRA");
        cuerpo.put("nombre", "Luna");
        cuerpo.put("areteFrente", null);
        cuerpo.put("areteReverso", null);
        cuerpo.put("raza", "Criolla");
        cuerpo.put("fechaNacimiento", "2025-01-10");
        cuerpo.put("nacimientoEstimado", false);
        cuerpo.put("categoria", "VAQUILLA");
        cuerpo.put("observaciones", "Dato sintético");
        return cuerpo;
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
        String jwt = tokens.emitir(usuarioId, sesionId, ahora, ahora.plusSeconds(600)).valor();
        return new Cuenta(usuarioId, sesionId, jwt);
    }

    private UUID crearGanaderia(Cuenta propietario) {
        UUID ganaderiaId = UUID.randomUUID();
        transacciones.executeWithoutResult(estado -> {
            jdbc.update("""
                    INSERT INTO identidad.ganaderia
                        (id, nombre, propietario_usuario_id, estado)
                    VALUES (?, 'Ganadería HTTP sintética', ?, 'ACTIVA')
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

    private int cantidadAnimales(UUID ganaderiaId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ganado.animal WHERE ganaderia_id = ?",
                Integer.class, ganaderiaId);
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsByteArray());
    }

    private record Cuenta(UUID usuarioId, UUID sesionId, String jwt) {
    }
}
