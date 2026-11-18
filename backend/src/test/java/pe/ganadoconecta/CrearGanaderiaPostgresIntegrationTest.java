package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void listaSoloGanaderiasConMembresiaActivaDelUsuario() throws Exception {
        Cuenta actor = crearCuentaAutenticada("lista-actor");
        Cuenta otro = crearCuentaAutenticada("lista-otro");
        UUID propiaId = UUID.randomUUID();
        UUID compartidaId = UUID.randomUUID();
        UUID ajenaId = UUID.randomUUID();

        crear(actor.jwt(), json(propiaId, "Amanecer")).andExpect(status().isCreated());
        crear(otro.jwt(), json(compartidaId, "Bosque compartido")).andExpect(status().isCreated());
        crear(otro.jwt(), json(ajenaId, "Campo privado")).andExpect(status().isCreated());
        jdbc.update("""
                INSERT INTO identidad.membresia
                    (id, ganaderia_id, usuario_id, rol, estado)
                VALUES (?, ?, ?, 'LECTOR', 'ACTIVA')
                """, UUID.randomUUID(), compartidaId, actor.usuarioId());

        listar(actor.jwt())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(propiaId.toString()))
                .andExpect(jsonPath("$[0].rol").value("PROPIETARIO"))
                .andExpect(jsonPath("$[1].id").value(compartidaId.toString()))
                .andExpect(jsonPath("$[1].rol").value("LECTOR"))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(ajenaId.toString()))))
                .andExpect(jsonPath("$[0].propietarioUsuarioId").doesNotExist())
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist());

        jdbc.update("""
                UPDATE identidad.membresia
                SET estado = 'REVOCADA', revocado_en = CURRENT_TIMESTAMP,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, compartidaId, actor.usuarioId());

        listar(actor.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propiaId.toString()));
    }

    @Test
    void listaVaciaParaUsuarioAutenticadoSinGanaderias() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("lista-vacia");

        listar(cuenta.jwt())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rechazaListadoSinAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/ganaderias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaContextoDePropietarioConValidacionUtc() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("acceso-propietario");
        UUID ganaderiaId = UUID.randomUUID();
        crear(cuenta.jwt(), json(ganaderiaId, "Los Pinos"))
                .andExpect(status().isCreated());

        acceso(cuenta.jwt(), ganaderiaId.toString())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.ganaderiaId").value(ganaderiaId.toString()))
                .andExpect(jsonPath("$.rol").value("PROPIETARIO"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.validadoEn")
                        .value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.nombre").doesNotExist())
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @Test
    void consultaContextoParaEditorYLectorConMembresiaActiva() throws Exception {
        Cuenta actor = crearCuentaAutenticada("acceso-familiar");
        Cuenta otro = crearCuentaAutenticada("acceso-dueno");
        UUID editableId = UUID.randomUUID();
        UUID lecturaId = UUID.randomUUID();
        crear(otro.jwt(), json(editableId, "La Esperanza")).andExpect(status().isCreated());
        crear(otro.jwt(), json(lecturaId, "El Retiro")).andExpect(status().isCreated());
        agregarMembresia(editableId, actor.usuarioId(), "EDITOR");
        agregarMembresia(lecturaId, actor.usuarioId(), "LECTOR");

        acceso(actor.jwt(), editableId.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("EDITOR"));
        acceso(actor.jwt(), lecturaId.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LECTOR"));
    }

    @Test
    void niegaSinDistinguirGanaderiaAjenaRevocadaInactivaOInexistente() throws Exception {
        Cuenta actor = crearCuentaAutenticada("acceso-denegado");
        Cuenta otro = crearCuentaAutenticada("acceso-ajeno");
        UUID ajenaId = UUID.randomUUID();
        UUID revocadaId = UUID.randomUUID();
        UUID inactivaId = UUID.randomUUID();
        crear(otro.jwt(), json(ajenaId, "Ajena")).andExpect(status().isCreated());
        crear(otro.jwt(), json(revocadaId, "Revocada")).andExpect(status().isCreated());
        crear(actor.jwt(), json(inactivaId, "Inactiva")).andExpect(status().isCreated());
        agregarMembresia(revocadaId, actor.usuarioId(), "LECTOR");
        jdbc.update("""
                UPDATE identidad.membresia
                SET estado = 'REVOCADA', revocado_en = CURRENT_TIMESTAMP,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, revocadaId, actor.usuarioId());
        jdbc.update("""
                UPDATE identidad.ganaderia
                SET estado = 'INACTIVA', actualizado_en = CURRENT_TIMESTAMP
                WHERE id = ?
                """, inactivaId);

        for (UUID id : java.util.List.of(ajenaId, revocadaId, inactivaId,
                UUID.randomUUID())) {
            acceso(actor.jwt(), id.toString())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACCESO_GANADERIA_DENEGADO"))
                    .andExpect(jsonPath("$.mensajeSeguro")
                            .value("No tiene acceso a la ganadería solicitada."))
                    .andExpect(jsonPath("$.ganaderiaId").doesNotExist())
                    .andExpect(jsonPath("$.rol").doesNotExist());
        }
    }

    @Test
    void rechazaContextoSinAutenticacionOIdentificadorValido() throws Exception {
        Cuenta cuenta = crearCuentaAutenticada("acceso-id-invalido");

        mvc.perform(get("/api/v1/ganaderias/{id}/acceso", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        acceso(cuenta.jwt(), "no-es-uuid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void propietarioRevocaEditorIdempotentementeYElJwtPrevioPierdeAcceso()
            throws Exception {
        Cuenta propietario = crearCuentaAutenticada("revoca-propietario");
        Cuenta editor = crearCuentaAutenticada("revoca-editor");
        UUID ganaderiaId = UUID.randomUUID();
        crear(propietario.jwt(), json(ganaderiaId, "La Arboleda"))
                .andExpect(status().isCreated());
        agregarMembresia(ganaderiaId, editor.usuarioId(), "EDITOR");
        acceso(editor.jwt(), ganaderiaId.toString()).andExpect(status().isOk());

        revocar(propietario.jwt(), ganaderiaId, editor.usuarioId(), true, ORIGEN)
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM identidad.membresia
                WHERE ganaderia_id = ? AND usuario_id = ?
                  AND rol = 'EDITOR' AND estado = 'REVOCADA'
                  AND revocado_en IS NOT NULL
                """, Integer.class, ganaderiaId, editor.usuarioId())).isEqualTo(1);
        acceso(editor.jwt(), ganaderiaId.toString())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_GANADERIA_DENEGADO"));

        revocar(propietario.jwt(), ganaderiaId, editor.usuarioId(), true, ORIGEN)
                .andExpect(status().isNoContent());
        assertThat(cantidad("identidad.membresia", "ganaderia_id", ganaderiaId))
                .isEqualTo(2);
    }

    @Test
    void editorNoPuedeRevocarAOtroMiembro() throws Exception {
        Cuenta propietario = crearCuentaAutenticada("revoca-dueno");
        Cuenta editor = crearCuentaAutenticada("revoca-actor-editor");
        Cuenta lector = crearCuentaAutenticada("revoca-lector");
        UUID ganaderiaId = UUID.randomUUID();
        crear(propietario.jwt(), json(ganaderiaId, "El Encanto"))
                .andExpect(status().isCreated());
        agregarMembresia(ganaderiaId, editor.usuarioId(), "EDITOR");
        agregarMembresia(ganaderiaId, lector.usuarioId(), "LECTOR");

        revocar(editor.jwt(), ganaderiaId, lector.usuarioId(), true, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_GANADERIA_DENEGADO"));

        assertThat(estadoMembresia(ganaderiaId, lector.usuarioId())).isEqualTo("ACTIVA");
    }

    @Test
    void protegePropietarioYNoRevelaObjetivosAjenosOInexistentes() throws Exception {
        Cuenta propietario = crearCuentaAutenticada("revoca-protegido");
        Cuenta otroPropietario = crearCuentaAutenticada("revoca-ajeno");
        Cuenta miembroAjeno = crearCuentaAutenticada("revoca-miembro-ajeno");
        UUID ganaderiaId = UUID.randomUUID();
        UUID ajenaId = UUID.randomUUID();
        crear(propietario.jwt(), json(ganaderiaId, "El Roble"))
                .andExpect(status().isCreated());
        crear(otroPropietario.jwt(), json(ajenaId, "La Rivera"))
                .andExpect(status().isCreated());
        agregarMembresia(ajenaId, miembroAjeno.usuarioId(), "LECTOR");

        revocar(propietario.jwt(), ganaderiaId, propietario.usuarioId(), true, ORIGEN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROPIETARIO_NO_REVOCABLE"));
        for (UUID objetivo : java.util.List.of(miembroAjeno.usuarioId(), UUID.randomUUID())) {
            revocar(propietario.jwt(), ganaderiaId, objetivo, true, ORIGEN)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("MEMBRESIA_NO_ENCONTRADA"))
                    .andExpect(jsonPath("$.usuarioId").doesNotExist());
        }
        assertThat(estadoMembresia(ganaderiaId, propietario.usuarioId()))
                .isEqualTo("ACTIVA");
    }

    @Test
    void exigeAutenticacionCsrfYOrigenValidoParaRevocar() throws Exception {
        Cuenta propietario = crearCuentaAutenticada("revoca-seguridad");
        Cuenta lector = crearCuentaAutenticada("revoca-seguridad-lector");
        UUID ganaderiaId = UUID.randomUUID();
        crear(propietario.jwt(), json(ganaderiaId, "Santa Clara"))
                .andExpect(status().isCreated());
        agregarMembresia(ganaderiaId, lector.usuarioId(), "LECTOR");

        revocar(null, ganaderiaId, lector.usuarioId(), true, ORIGEN)
                .andExpect(status().isUnauthorized());
        revocar(propietario.jwt(), ganaderiaId, lector.usuarioId(), false, ORIGEN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CSRF_INVALIDO"));
        revocar(propietario.jwt(), ganaderiaId, lector.usuarioId(), true,
                "https://origen-ajeno.invalid")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORIGEN_NO_PERMITIDO"));

        assertThat(estadoMembresia(ganaderiaId, lector.usuarioId())).isEqualTo("ACTIVA");
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

    private org.springframework.test.web.servlet.ResultActions listar(String jwt) throws Exception {
        return mvc.perform(get("/api/v1/ganaderias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON));
    }

    private org.springframework.test.web.servlet.ResultActions acceso(
            String jwt, String ganaderiaId) throws Exception {
        return mvc.perform(get("/api/v1/ganaderias/{id}/acceso", ganaderiaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON));
    }

    private org.springframework.test.web.servlet.ResultActions revocar(String jwt,
            UUID ganaderiaId, UUID usuarioId, boolean conCsrf, String origen)
            throws Exception {
        var solicitud = delete("/api/v1/ganaderias/{ganaderiaId}/membresias/{usuarioId}",
                ganaderiaId, usuarioId).header(HttpHeaders.ORIGIN, origen);
        if (jwt != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        if (conCsrf) {
            solicitud.with(csrf().asHeader());
        }
        return mvc.perform(solicitud);
    }

    private void agregarMembresia(UUID ganaderiaId, UUID usuarioId, String rol) {
        jdbc.update("""
                INSERT INTO identidad.membresia
                    (id, ganaderia_id, usuario_id, rol, estado)
                VALUES (?, ?, ?, ?, 'ACTIVA')
                """, UUID.randomUUID(), ganaderiaId, usuarioId, rol);
    }

    private String json(UUID id, String nombre) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("id", id, "nombre", nombre));
    }

    private String correo(String sufijo) {
        return PREFIJO + sufijo + "-" + UUID.randomUUID() + "@ganadoconecta.invalid";
    }

    private String estadoMembresia(UUID ganaderiaId, UUID usuarioId) {
        return jdbc.queryForObject("""
                SELECT estado
                FROM identidad.membresia
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, String.class, ganaderiaId, usuarioId);
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
