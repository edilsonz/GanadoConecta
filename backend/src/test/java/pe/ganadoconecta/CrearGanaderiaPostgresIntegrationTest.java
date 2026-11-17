package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioGanaderiasPostgres;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaGanaderiaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RegistrarUsuario;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;
import pe.ganadoconecta.modulos.identidad.dominio.NombreGanaderia;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class CrearGanaderiaPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String ORIGEN = "http://127.0.0.1:5173";
    private static final String PREFIJO = "ganaderia-test-";
    private static final String CONTRASENA = "contraseña-sintética-segura";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistrarUsuario registrar;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transacciones;

    @AfterEach
    void limpiarDatosSinteticos() {
        transacciones.executeWithoutResult(estado -> {
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
            jdbc.update("""
                    DELETE FROM identidad.membresia m
                    USING identidad.ganaderia g, identidad.usuario u
                    WHERE m.ganaderia_id = g.id
                      AND g.propietario_usuario_id = u.id
                      AND u.email_normalizado LIKE ?
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
    void creaGanaderiaYPropietarioAtomicosConReplayIdempotente() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("correcta");
        UUID ganaderiaId = UUID.randomUUID();
        String cuerpo = json(ganaderiaId, "  Ganaderi\u0301a Los Pinos  ");

        crear(cuenta.jwt(), cuerpo)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(ganaderiaId.toString()))
                .andExpect(jsonPath("$.nombre").value("Ganadería Los Pinos"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.rol").value("PROPIETARIO"));

        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM identidad.ganaderia g
                JOIN identidad.membresia m ON m.ganaderia_id = g.id
                WHERE g.id = ? AND g.propietario_usuario_id = ?
                  AND m.usuario_id = ? AND m.rol = 'PROPIETARIO'
                  AND m.estado = 'ACTIVA'
                """, Integer.class, ganaderiaId, cuenta.usuarioId(), cuenta.usuarioId()))
                .isEqualTo(1);

        crear(cuenta.jwt(), cuerpo).andExpect(status().isOk());
        assertThat(cantidad("identidad.ganaderia", "id", ganaderiaId)).isEqualTo(1);
        assertThat(cantidad("identidad.membresia", "ganaderia_id", ganaderiaId)).isEqualTo(1);

        crear(cuenta.jwt(), json(ganaderiaId, "Otro nombre"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("GANADERIA_NO_DISPONIBLE"));
    }

    @Test
    void rechazaSolicitudSinAutenticacion() throws Exception {
        UUID ganaderiaId = UUID.randomUUID();

        mvc.perform(post("/api/v1/ganaderias")
                        .with(csrf().asHeader())
                        .header(HttpHeaders.ORIGIN, ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ganaderiaId, "Los Pinos")))
                .andExpect(status().isUnauthorized());

        assertThat(cantidad("identidad.ganaderia", "id", ganaderiaId)).isZero();
    }

    @Test
    void rechazaEscrituraAutenticadaSinCsrf() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("sin-csrf");
        UUID ganaderiaId = UUID.randomUUID();

        mvc.perform(post("/api/v1/ganaderias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cuenta.jwt())
                        .header(HttpHeaders.ORIGIN, ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ganaderiaId, "Los Pinos")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));

        assertThat(cantidad("identidad.ganaderia", "id", ganaderiaId)).isZero();

        mvc.perform(post("/api/v1/ganaderias")
                        .with(csrf().asHeader())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cuenta.jwt())
                        .header(HttpHeaders.ORIGIN, "https://origen-ajeno.invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ganaderiaId, "Los Pinos")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));

        assertThat(cantidad("identidad.ganaderia", "id", ganaderiaId)).isZero();
    }

    @Test
    void rechazaDatosInvalidosYPropietarioEnviadoPorCliente() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("invalidos");
        UUID ganaderiaId = UUID.randomUUID();

        crear(cuenta.jwt(), json(ganaderiaId, "   "))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        crear(cuenta.jwt(), objectMapper.writeValueAsString(java.util.Map.of(
                        "id", UUID.randomUUID(),
                        "nombre", "Los Pinos",
                        "propietarioUsuarioId", UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CAMPO_NO_ADMITIDO"));

        assertThat(cantidad("identidad.ganaderia", "id", ganaderiaId)).isZero();
    }

    @Test
    void revierteGanaderiaSiFallaLaMembresia() {
        RegistrarUsuario.Resultado cuenta = registrar.ejecutar(new RegistrarUsuario.Comando(
                correo("atomica"), CONTRASENA));
        RepositorioGanaderiasPostgres repositorio = new RepositorioGanaderiasPostgres(
                jdbc, transacciones);
        UUID membresiaId = UUID.randomUUID();
        UUID primeraId = UUID.randomUUID();
        UUID fallidaId = UUID.randomUUID();
        repositorio.crear(new GanaderiaNueva(primeraId, NombreGanaderia.desde("Primera"),
                cuenta.id()), membresiaId);

        assertThatThrownBy(() -> repositorio.crear(new GanaderiaNueva(
                fallidaId, NombreGanaderia.desde("Debe revertirse"), cuenta.id()),
                membresiaId))
                .isInstanceOf(PersistenciaGanaderiaException.class);

        assertThat(cantidad("identidad.ganaderia", "id", primeraId)).isEqualTo(1);
        assertThat(cantidad("identidad.ganaderia", "id", fallidaId)).isZero();
    }

    private Cuenta crearCuentaAutenticada(String sufijo) throws Exception {
        RegistrarUsuario.Resultado cuenta = registrar.ejecutar(new RegistrarUsuario.Comando(
                correo(sufijo), CONTRASENA));
        String solicitud = objectMapper.writeValueAsString(java.util.Map.of(
                "correo", cuenta.correo(), "contrasena", CONTRASENA));
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf().asHeader())
                        .header(HttpHeaders.ORIGIN, ORIGEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode respuesta = objectMapper.readTree(login.getResponse().getContentAsString());
        return new Cuenta(cuenta.id(), respuesta.path("accessToken").asText());
    }

    private org.springframework.test.web.servlet.ResultActions crear(
            String jwt, String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/ganaderias")
                .with(csrf().asHeader())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(HttpHeaders.ORIGIN, ORIGEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private String json(UUID id, String nombre) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("id", id, "nombre", nombre));
    }

    private String correo(String sufijo) {
        return PREFIJO + sufijo + "-" + UUID.randomUUID() + "@ganadoconecta.invalid";
    }

    private int cantidad(String tabla, String columna, UUID id) {
        if (!(tabla.equals("identidad.ganaderia") || tabla.equals("identidad.membresia"))) {
            throw new IllegalArgumentException("Tabla de prueba no permitida");
        }
        if (!(columna.equals("id") || columna.equals("ganaderia_id"))) {
            throw new IllegalArgumentException("Columna de prueba no permitida");
        }
        return jdbc.queryForObject(
                "SELECT count(*) FROM " + tabla + " WHERE " + columna + " = ?",
                Integer.class, id);
    }

    private record Cuenta(UUID usuarioId, String jwt) {
    }
}
