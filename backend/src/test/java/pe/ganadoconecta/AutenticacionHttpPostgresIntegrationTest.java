package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.ganadoconecta.modulos.identidad.aplicacion.RegistrarUsuario;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class AutenticacionHttpPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String ORIGEN = "http://127.0.0.1:5173";
    private static final String PREFIJO = "auth-http-test-";
    private static final String CONTRASENA = "contraseña-sintética-segura";
    private static final String COOKIE = "__Host-GC-Refresh";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistrarUsuario registrar;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiarDatosSinteticos() throws Exception {
        jdbc.update("""
                DELETE FROM identidad.verificador_renovacion v
                USING identidad.sesion_dispositivo s, identidad.usuario u
                WHERE v.sesion_id = s.id AND s.usuario_id = u.id
                  AND u.email_normalizado LIKE ?
                """, PREFIJO + "%@ganadoconecta.invalid");
        jdbc.update("""
                DELETE FROM identidad.sesion_dispositivo s
                USING identidad.usuario u
                WHERE s.usuario_id = u.id AND u.email_normalizado LIKE ?
                """, PREFIJO + "%@ganadoconecta.invalid");
        jdbc.update("DELETE FROM identidad.usuario WHERE email_normalizado LIKE ?",
                PREFIJO + "%@ganadoconecta.invalid");
        jdbc.update("DELETE FROM identidad.intento_login WHERE clave_sha256 = ?",
                huella("198.51.100.90"));
    }

    @Test
    void loginCreaCookieSeguraYJwtQueAutorizaMe() throws Exception {
        String correo = crearCuenta("login");

        MvcResult login = login(correo, CONTRASENA, "198.51.100.80", null)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.usuarioId").isNotEmpty())
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.tokenRenovacion").doesNotExist())
                .andReturn();

        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith(COOKIE + "=")
                .contains("Path=/", "Max-Age=", "Secure", "HttpOnly", "SameSite=Strict")
                .doesNotContain("Domain=");
        JsonNode json = json(login);
        String jwt = json.get("accessToken").asText();
        String refresh = valorCookie(setCookie);
        assertThat(login.getResponse().getContentAsString()).doesNotContain(refresh);

        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.usuarioId").value(json.get("usuarioId").asText()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void erroresDeCredencialesSonGenericosYElLimiteDevuelve429() throws Exception {
        String correo = crearCuenta("errores");
        String mensaje = null;
        for (int intento = 1; intento <= 10; intento++) {
            MvcResult resultado = login(correo, "contraseña-incorrecta", "198.51.100.90", null)
                    .andExpect(intento < 10 ? status().isUnauthorized()
                            : status().isTooManyRequests())
                    .andExpect(jsonPath("$.accessToken").doesNotExist())
                    .andReturn();
            if (intento == 1) {
                mensaje = json(resultado).get("mensajeSeguro").asText();
            }
        }
        assertThat(mensaje).isEqualTo("Las credenciales no son válidas.");
        login(correo, CONTRASENA, "198.51.100.90", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"));
        assertThat(cantidadSesiones(correo)).isZero();
    }

    @Test
    void renovacionRotaCookieYReintentoTrasRespuestaPerdidaRevocaLaFamilia() throws Exception {
        String correo = crearCuenta("rotacion");
        MvcResult login = login(correo, CONTRASENA, "198.51.100.81", null)
                .andExpect(status().isOk()).andReturn();
        String cookieAnterior = valorCookie(
                login.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        MvcResult renovacion = renovar(cookieAnterior)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenRenovacion").doesNotExist())
                .andReturn();
        String cookieNueva = valorCookie(
                renovacion.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(cookieNueva).isNotEqualTo(cookieAnterior);

        renovar(cookieAnterior)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESION_NO_VALIDA"));
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + json(renovacion).get("accessToken").asText()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESION_NO_VALIDA"));
    }

    @Test
    void dosRenovacionesConcurrentesNoProducenDosSesionesValidas() throws Exception {
        String correo = crearCuenta("concurrente");
        MvcResult login = login(correo, CONTRASENA, "198.51.100.82", null)
                .andExpect(status().isOk()).andReturn();
        String refresh = valorCookie(login.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> primero = executor.submit(() ->
                    renovarConcurrente(refresh, preparados, iniciar));
            Future<MvcResult> segundo = executor.submit(() ->
                    renovarConcurrente(refresh, preparados, iniciar));
            preparados.await();
            iniciar.countDown();
            List<MvcResult> resultados = List.of(primero.get(), segundo.get());
            assertThat(resultados.stream().map(r -> r.getResponse().getStatus()).toList())
                    .containsExactlyInAnyOrder(200, 401);
            MvcResult correcta = resultados.stream()
                    .filter(r -> r.getResponse().getStatus() == 200).findFirst().orElseThrow();
            mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + json(correcta).get("accessToken").asText()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void logoutEsIdempotenteBorraCookieEInvalidaJwtAnterior() throws Exception {
        String correo = crearCuenta("logout");
        MvcResult login = login(correo, CONTRASENA, "198.51.100.83", null)
                .andExpect(status().isOk()).andReturn();
        String jwt = json(login).get("accessToken").asText();
        String refresh = valorCookie(login.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        logout(refresh)
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(COOKIE + "="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("Secure"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Strict"))));
        logout(refresh).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutReconoceTokenConsumidoYRevocaLaSesion() throws Exception {
        String correo = crearCuenta("logout-historico");
        MvcResult login = login(correo, CONTRASENA, "198.51.100.86", null)
                .andExpect(status().isOk()).andReturn();
        String cookieAnterior = valorCookie(
                login.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        MvcResult renovacion = renovar(cookieAnterior)
                .andExpect(status().isOk()).andReturn();

        logout(cookieAnterior).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + json(renovacion).get("accessToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sesionExpiradaYJwtAusenteSeRechazan() throws Exception {
        String correo = crearCuenta("expirada");
        MvcResult login = login(correo, CONTRASENA, "198.51.100.84", null)
                .andExpect(status().isOk()).andReturn();
        String jwt = json(login).get("accessToken").asText();
        jdbc.update("""
                UPDATE identidad.sesion_dispositivo s
                SET creado_en = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                    vence_en = CURRENT_TIMESTAMP - INTERVAL '1 hour'
                FROM identidad.usuario u
                WHERE s.usuario_id = u.id AND u.email_normalizado = ?
                """, correo);

        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"));
    }

    @Test
    void csrfUOrigenInvalidosNoCreanNiRevocanSesiones() throws Exception {
        String correo = crearCuenta("csrf");
        mvc.perform(post("/api/v1/auth/login")
                        .header("Origin", ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(correo, CONTRASENA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf().asHeader())
                        .header("Origin", "https://origen-ajeno.invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(correo, CONTRASENA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));
        assertThat(cantidadSesiones(correo)).isZero();

        MvcResult login = login(correo, CONTRASENA, "198.51.100.87", null)
                .andExpect(status().isOk()).andReturn();
        String jwt = json(login).get("accessToken").asText();
        String refresh = valorCookie(login.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", ORIGEN)
                        .cookie(new jakarta.servlet.http.Cookie(COOKIE, refresh)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void loginNuevoRevocaSesionPreviaReconocidaPorLaCookie() throws Exception {
        String correo = crearCuenta("relogin");
        MvcResult primero = login(correo, CONTRASENA, "198.51.100.85", null)
                .andExpect(status().isOk()).andReturn();
        String jwtAnterior = json(primero).get("accessToken").asText();
        String cookieAnterior = valorCookie(
                primero.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        login(correo, CONTRASENA, "198.51.100.85", cookieAnterior)
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAnterior))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String correo, String contrasena, String ip, String refresh) throws Exception {
        MockHttpServletRequestBuilder solicitud = post("/api/v1/auth/login")
                .with(csrf().asHeader())
                .with(request -> { request.setRemoteAddr(ip); return request; })
                .header("Origin", ORIGEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonLogin(correo, contrasena));
        if (refresh != null) {
            solicitud.cookie(new jakarta.servlet.http.Cookie(COOKIE, refresh));
        }
        return mvc.perform(solicitud);
    }

    private org.springframework.test.web.servlet.ResultActions renovar(String refresh)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/renovar")
                .with(csrf().asHeader())
                .header("Origin", ORIGEN)
                .cookie(new jakarta.servlet.http.Cookie(COOKIE, refresh)));
    }

    private org.springframework.test.web.servlet.ResultActions logout(String refresh)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/logout")
                .with(csrf().asHeader())
                .header("Origin", ORIGEN)
                .cookie(new jakarta.servlet.http.Cookie(COOKIE, refresh)));
    }

    private MvcResult renovarConcurrente(String refresh, CountDownLatch preparados,
            CountDownLatch iniciar) throws Exception {
        preparados.countDown();
        iniciar.await();
        return renovar(refresh).andReturn();
    }

    private String crearCuenta(String sufijo) {
        String correo = PREFIJO + sufijo + "-" + UUID.randomUUID()
                + "@ganadoconecta.invalid";
        registrar.ejecutar(new RegistrarUsuario.Comando(correo, CONTRASENA));
        return correo;
    }

    private int cantidadSesiones(String correo) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identidad.sesion_dispositivo s
                JOIN identidad.usuario u ON u.id = s.usuario_id
                WHERE u.email_normalizado = ?
                """, Integer.class, correo);
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsByteArray());
    }

    private String jsonLogin(String correo, String contrasena) {
        return """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena);
    }

    private String valorCookie(String setCookie) {
        assertThat(setCookie).isNotNull();
        int inicio = (COOKIE + "=").length();
        int fin = setCookie.indexOf(';', inicio);
        return setCookie.substring(inicio, fin);
    }

    private byte[] huella(String valor) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8));
    }
}
