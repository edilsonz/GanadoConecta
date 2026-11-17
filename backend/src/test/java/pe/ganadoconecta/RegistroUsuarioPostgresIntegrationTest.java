package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.ganadoconecta.modulos.identidad.aplicacion.CodificadorContrasena;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaUsuarioException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioUsuarios;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class RegistroUsuarioPostgresIntegrationTest {
    private static final String RUTA = "/api/v1/auth/registro";
    private static final String ORIGEN = "http://127.0.0.1:5173";
    private static final String CONTRASENA = "contraseña-sintética-segura";
    private static final String PREFIJO = "registro-test-";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CodificadorContrasena codificador;

    @Autowired
    private RepositorioUsuarios repositorio;

    @AfterEach
    void eliminarDatosSinteticos() {
        jdbc.update("DELETE FROM identidad.usuario WHERE email_normalizado LIKE ?",
                PREFIJO + "%@ganadoconecta.invalid");
    }

    @Test
    void registraCuentaActivaSinSesionMembresiaNiContrasenaExpuesta() throws Exception {
        String local = PREFIJO + UUID.randomUUID();
        String correoEntrada = "  " + local.toUpperCase() + "@GANADOCONECTA.INVALID  ";
        String normalizado = local + "@ganadoconecta.invalid";

        mvc.perform(registro(correoEntrada, CONTRASENA, "192.0.2.10"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.correo").value(normalizado))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String hash = jdbc.queryForObject("""
                SELECT password_hash FROM identidad.usuario WHERE email_normalizado = ?
                """, String.class, normalizado);
        assertThat(hash).startsWith("$argon2id$").doesNotContain(CONTRASENA);
        assertThat(codificador.coincide(CONTRASENA, hash)).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.membresia m
                JOIN identidad.usuario u ON u.id = m.usuario_id
                WHERE u.email_normalizado = ?
                """, Integer.class, normalizado)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.sesion_dispositivo s
                JOIN identidad.usuario u ON u.id = s.usuario_id
                WHERE u.email_normalizado = ?
                """, Integer.class, normalizado)).isZero();
    }

    @Test
    void rechazaCorreoContrasenaYCamposNoAdmitidosSinEscribir() throws Exception {
        mvc.perform(registro("correo-invalido", CONTRASENA, "192.0.2.11"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.erroresPorCampo.correo").exists());

        mvc.perform(registro(correoNuevo(), "muy-corta", "192.0.2.12"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erroresPorCampo.contrasena").exists());

        int sufijoIp = 13;
        for (String campo : List.of("rol", "ganaderiaId", "estado")) {
            String correoConCampoExtra = correoNuevo();
            String ip = "192.0.2." + sufijoIp++;
            mvc.perform(post(RUTA)
                            .with(csrf().asHeader())
                            .with(request -> { request.setRemoteAddr(ip); return request; })
                            .header("Origin", ORIGEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"correo":"%s","contrasena":"%s","%s":"NO_ADMITIDO"}
                                    """.formatted(correoConCampoExtra, CONTRASENA, campo)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CAMPO_NO_ADMITIDO"))
                    .andExpect(jsonPath("$.erroresPorCampo." + campo).exists());
        }

        assertThat(cantidadUsuariosSinteticos()).isZero();
    }

    @Test
    void correoNormalizadoDuplicadoDevuelveConflictoGenerico() throws Exception {
        String local = PREFIJO + UUID.randomUUID();
        String normalizado = local + "@ganadoconecta.invalid";
        mvc.perform(registro(" " + local.toUpperCase() + "@GANADOCONECTA.INVALID ",
                        CONTRASENA, "192.0.2.20"))
                .andExpect(status().isCreated());

        mvc.perform(registro(normalizado, CONTRASENA, "192.0.2.21"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensajeSeguro").value("No fue posible completar el registro."))
                .andExpect(jsonPath("$.id").doesNotExist());

        assertThat(cantidadPorCorreo(normalizado)).isEqualTo(1);
    }

    @Test
    void dosRegistrosConcurrentesDelMismoCorreoCreanUnaSolaCuenta() throws Exception {
        String correo = correoNuevo();
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> primero = executor.submit(() ->
                    registrarConcurrente(correo, "198.51.100.10", preparados, iniciar));
            Future<Integer> segundo = executor.submit(() ->
                    registrarConcurrente(correo, "198.51.100.11", preparados, iniciar));
            preparados.await();
            iniciar.countDown();

            assertThat(List.of(primero.get(), segundo.get()))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(cantidadPorCorreo(correo)).isEqualTo(1);
    }

    @Test
    void falloDeRestriccionNoDejaRegistroParcial() {
        String correo = correoNuevo();
        UsuarioNuevo invalido = new UsuarioNuevo(
                UUID.randomUUID(), CorreoElectronico.desde(correo), "texto-plano-prohibido");

        assertThatThrownBy(() -> repositorio.crear(invalido))
                .isInstanceOf(PersistenciaUsuarioException.class);
        assertThat(cantidadPorCorreo(correo)).isZero();
    }

    @Test
    void limitaAbusoPorOrigenYConservaLosDemasEndpointsCerrados() throws Exception {
        String origenPrueba = "203.0.113.50";
        for (int intento = 0; intento < 10; intento++) {
            mvc.perform(registro(correoNuevo(), "corta", origenPrueba))
                    .andExpect(status().isUnprocessableEntity());
        }
        mvc.perform(registro(correoNuevo(), "corta", origenPrueba))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));

        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf().asHeader())
                        .header("Origin", ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        assertThat(cantidadUsuariosSinteticos()).isZero();
    }

    @Test
    void exigeOrigenYCsrfSinModificarDatos() throws Exception {
        mvc.perform(post(RUTA)
                        .with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(correoNuevo(), CONTRASENA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));

        mvc.perform(post(RUTA)
                        .header("Origin", ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(correoNuevo(), CONTRASENA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));

        assertThat(cantidadUsuariosSinteticos()).isZero();
    }

    @Test
    void rechazaCuerpoSuperiorADieciseisKibSinEscribir() throws Exception {
        mvc.perform(registro(correoNuevo(), "a".repeat(17_000), "192.0.2.30"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("CUERPO_DEMASIADO_GRANDE"));
        assertThat(cantidadUsuariosSinteticos()).isZero();
    }

    private int registrarConcurrente(String correo, String origenIp,
            CountDownLatch preparados, CountDownLatch iniciar) throws Exception {
        preparados.countDown();
        iniciar.await();
        return mvc.perform(registro(correo, CONTRASENA, origenIp))
                .andReturn().getResponse().getStatus();
    }

    private MockHttpServletRequestBuilder registro(String correo, String contrasena,
            String origenIp) {
        return post(RUTA)
                .with(csrf().asHeader())
                .with(request -> { request.setRemoteAddr(origenIp); return request; })
                .header("Origin", ORIGEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(correo, contrasena));
    }

    private String json(String correo, String contrasena) {
        return """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena);
    }

    private String correoNuevo() {
        return PREFIJO + UUID.randomUUID() + "@ganadoconecta.invalid";
    }

    private int cantidadPorCorreo(String correo) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identidad.usuario WHERE email_normalizado = ?
                """, Integer.class, correo);
    }

    private int cantidadUsuariosSinteticos() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identidad.usuario WHERE email_normalizado LIKE ?
                """, Integer.class, PREFIJO + "%@ganadoconecta.invalid");
    }

}
