package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.ServicioJwtRs256;
import pe.ganadoconecta.modulos.identidad.aplicacion.CodificadorContrasena;
import pe.ganadoconecta.modulos.identidad.aplicacion.CredencialesInvalidasException;
import pe.ganadoconecta.modulos.identidad.aplicacion.DemasiadosIntentosAutenticacionException;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorCredencialesRenovacion;
import pe.ganadoconecta.modulos.identidad.aplicacion.IniciarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.LimiteIntentosLogin;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RenovarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioCredenciales;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioSesiones;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioUsuarios;
import pe.ganadoconecta.modulos.identidad.aplicacion.SesionNoValidaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.ValidarTokenAcceso;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class NucleoAutenticacionPostgresIntegrationTest {
    private static final String PREFIJO = "login-test-";
    private static final String CONTRASENA = "contraseña-sintética-segura";
    private static KeyPair claves;

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RepositorioUsuarios usuarios;
    @Autowired
    private RepositorioCredenciales credenciales;
    @Autowired
    private RepositorioSesiones sesiones;
    @Autowired
    private GeneradorCredencialesRenovacion renovaciones;
    @Autowired
    private CodificadorContrasena codificador;
    @Autowired
    private LimiteIntentosLogin limite;

    private final Set<String> correosUsados = new HashSet<>();
    private final Set<String> origenesUsados = new HashSet<>();

    @BeforeAll
    static void generarClavesEfimeras() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        claves = generator.generateKeyPair();
    }

    @AfterEach
    void limpiarSoloDatosSinteticos() throws Exception {
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
        for (String correo : correosUsados) {
            borrarIntento("CORREO", correo);
        }
        for (String origen : origenesUsados) {
            borrarIntento("ORIGEN", origen);
        }
    }

    @Test
    void loginValidoCreaSesionYVerificadorProtegidoYJwtVinculado() {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String correo = crearUsuario("valido", "ACTIVO");
        String origen = origen("192.0.2.101");
        Contexto contexto = contexto(ahora);

        IniciarSesion.Resultado resultado = contexto.login().ejecutar(
                new IniciarSesion.Comando(correo, CONTRASENA, origen));

        assertThat(jdbc.queryForObject("""
                SELECT EXTRACT(EPOCH FROM (vence_en - creado_en))::bigint
                FROM identidad.sesion_dispositivo WHERE id = ?
                """, Long.class, resultado.sesionId())).isEqualTo(86_400L);
        assertThat(jdbc.queryForObject("""
                SELECT encode(huella_sha256, 'hex')
                FROM identidad.verificador_renovacion WHERE sesion_id = ?
                """, String.class, resultado.sesionId()))
                .isEqualTo(renovaciones.calcularHuella(resultado.tokenRenovacion()))
                .doesNotContain(resultado.tokenRenovacion());
        assertThat(contexto.validar().ejecutar(resultado.tokenAcceso().valor()).usuarioId())
                .isEqualTo(resultado.usuarioId());
        assertThat(resultado.tokenAcceso().expiraEn()).isEqualTo(ahora.plusSeconds(600));
    }

    @Test
    void inexistenteInactivoYClaveIncorrectaSonIndistinguiblesYSinSesion() throws Exception {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String activo = crearUsuario("clave-mala", "ACTIVO");
        String inactivo = crearUsuario("inactivo", "INACTIVO");
        String inexistente = correo("inexistente");
        Contexto contexto = contexto(ahora);

        for (CasoInvalido caso : java.util.List.of(
                new CasoInvalido(activo, "clave-incorrecta-larga", origen("192.0.2.102")),
                new CasoInvalido(inactivo, CONTRASENA, origen("192.0.2.103")),
                new CasoInvalido(inexistente, CONTRASENA, origen("192.0.2.104")))) {
            assertThatThrownBy(() -> contexto.login().ejecutar(
                    new IniciarSesion.Comando(caso.correo(), caso.clave(), caso.origen())))
                    .isExactlyInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Las credenciales no son válidas");
        }
        assertThat(cantidadSesionesSinteticas()).isZero();
        for (CasoInvalido caso : java.util.List.of(
                new CasoInvalido(activo, "", "192.0.2.102"),
                new CasoInvalido(inactivo, "", "192.0.2.103"),
                new CasoInvalido(inexistente, "", "192.0.2.104"))) {
            assertThat(cantidadIntento("CORREO", caso.correo())).isEqualTo(1);
            assertThat(cantidadIntento("ORIGEN", caso.origen())).isEqualTo(1);
        }
    }

    @Test
    void diezFallosBloqueanTemporalmenteInclusoConClaveCorrecta() {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String correo = crearUsuario("limite", "ACTIVO");
        String origen = origen("192.0.2.105");
        Contexto contexto = contexto(ahora);

        for (int intento = 1; intento < 10; intento++) {
            assertThatThrownBy(() -> contexto.login().ejecutar(
                    new IniciarSesion.Comando(correo, "clave-incorrecta-larga", origen)))
                    .isExactlyInstanceOf(CredencialesInvalidasException.class);
        }
        assertThatThrownBy(() -> contexto.login().ejecutar(
                new IniciarSesion.Comando(correo, "clave-incorrecta-larga", origen)))
                .isExactlyInstanceOf(DemasiadosIntentosAutenticacionException.class);
        assertThatThrownBy(() -> contexto.login().ejecutar(
                new IniciarSesion.Comando(correo, CONTRASENA, origen)))
                .isExactlyInstanceOf(DemasiadosIntentosAutenticacionException.class);
        assertThat(cantidadSesionesSinteticas()).isZero();
    }

    @Test
    void rotacionConsumeAnteriorYReutilizarloRevocaSesionYJwt() {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String correo = crearUsuario("rotacion", "ACTIVO");
        Contexto contexto = contexto(ahora);
        IniciarSesion.Resultado login = contexto.login().ejecutar(
                new IniciarSesion.Comando(correo, CONTRASENA, origen("192.0.2.106")));

        RenovarSesion.Resultado renovada = contexto.renovar().ejecutar(login.tokenRenovacion());
        assertThat(renovada.sesionId()).isEqualTo(login.sesionId());
        assertThat(renovada.sesionVenceEn()).isEqualTo(login.sesionVenceEn());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.verificador_renovacion
                WHERE sesion_id = ? AND estado = 'ACTIVO'
                """, Integer.class, login.sesionId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.verificador_renovacion
                WHERE sesion_id = ? AND estado = 'CONSUMIDO'
                """, Integer.class, login.sesionId())).isEqualTo(1);

        assertThatThrownBy(() -> contexto.renovar().ejecutar(login.tokenRenovacion()))
                .isExactlyInstanceOf(SesionNoValidaException.class);
        assertThat(jdbc.queryForObject("""
                SELECT revocado_en IS NOT NULL FROM identidad.sesion_dispositivo WHERE id = ?
                """, Boolean.class, login.sesionId())).isTrue();
        assertThatThrownBy(() -> contexto.validar().ejecutar(renovada.tokenAcceso().valor()))
                .isExactlyInstanceOf(SesionNoValidaException.class);
    }

    @Test
    void sesionVencidaORevocadaInvalidaJwtAunqueLaFirmaSeaCorrecta() {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String correo = crearUsuario("estado-sesion", "ACTIVO");
        Contexto contexto = contexto(ahora);
        UUID usuarioId = jdbc.queryForObject(
                "SELECT id FROM identidad.usuario WHERE email_normalizado = ?",
                UUID.class, correo);
        UUID sesionId = UUID.randomUUID();
        var refresh = renovaciones.generar();
        sesiones.crear(new RepositorioSesiones.NuevaSesion(
                sesionId, usuarioId, ahora, ahora.plusSeconds(300),
                UUID.randomUUID(), refresh.huellaSha256()));
        String token = contexto.jwt().emitir(
                usuarioId, sesionId, ahora, ahora.plusSeconds(600)).valor();

        contexto.reloj().fijar(ahora.plusSeconds(301));
        assertThatThrownBy(() -> contexto.validar().ejecutar(token))
                .isExactlyInstanceOf(SesionNoValidaException.class);

        contexto.reloj().fijar(ahora);
        jdbc.update("UPDATE identidad.sesion_dispositivo SET revocado_en = ? WHERE id = ?",
                java.time.OffsetDateTime.ofInstant(ahora.plusSeconds(1), ZoneOffset.UTC), sesionId);
        assertThatThrownBy(() -> contexto.validar().ejecutar(token))
                .isExactlyInstanceOf(SesionNoValidaException.class);
    }

    @Test
    void falloAlGuardarVerificadorRevierteLaSesionCompleta() {
        Instant ahora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String correo = crearUsuario("rollback", "ACTIVO");
        UUID usuarioId = jdbc.queryForObject(
                "SELECT id FROM identidad.usuario WHERE email_normalizado = ?",
                UUID.class, correo);
        UUID primeraSesion = UUID.randomUUID();
        UUID segundaSesion = UUID.randomUUID();
        String huellaRepetida = renovaciones.generar().huellaSha256();
        sesiones.crear(new RepositorioSesiones.NuevaSesion(
                primeraSesion, usuarioId, ahora, ahora.plusSeconds(86_400),
                UUID.randomUUID(), huellaRepetida));

        assertThatThrownBy(() -> sesiones.crear(new RepositorioSesiones.NuevaSesion(
                segundaSesion, usuarioId, ahora, ahora.plusSeconds(86_400),
                UUID.randomUUID(), huellaRepetida)))
                .isExactlyInstanceOf(PersistenciaIdentidadException.class);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.sesion_dispositivo WHERE id = ?
                """, Integer.class, primeraSesion)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM identidad.sesion_dispositivo WHERE id = ?
                """, Integer.class, segundaSesion)).isZero();
    }

    private Contexto contexto(Instant ahora) {
        RelojMutable reloj = new RelojMutable(ahora);
        ServicioJwtRs256 jwt = new ServicioJwtRs256(
                (RSAPublicKey) claves.getPublic(), (RSAPrivateKey) claves.getPrivate(),
                "ganadoconecta", "ganadoconecta-web", reloj);
        IniciarSesion login = new IniciarSesion(
                credenciales, codificador, sesiones, renovaciones, jwt, limite,
                UUID::randomUUID, reloj, codificador.codificar("comparación-sintética-constante"));
        return new Contexto(reloj, jwt, login,
                new RenovarSesion(sesiones, renovaciones, jwt, UUID::randomUUID, reloj),
                new ValidarTokenAcceso(jwt, sesiones, reloj));
    }

    private String crearUsuario(String sufijo, String estado) {
        String correo = correo(sufijo);
        UUID id = UUID.randomUUID();
        usuarios.crear(new UsuarioNuevo(id, CorreoElectronico.desde(correo),
                codificador.codificar(CONTRASENA)));
        if (!"ACTIVO".equals(estado)) {
            jdbc.update("UPDATE identidad.usuario SET estado = ?, actualizado_en = CURRENT_TIMESTAMP WHERE id = ?",
                    estado, id);
        }
        return correo;
    }

    private String correo(String sufijo) {
        String valor = PREFIJO + sufijo + "-" + UUID.randomUUID()
                + "@ganadoconecta.invalid";
        correosUsados.add(valor);
        return valor;
    }

    private String origen(String valor) {
        origenesUsados.add(valor);
        return valor;
    }

    private int cantidadSesionesSinteticas() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identidad.sesion_dispositivo s
                JOIN identidad.usuario u ON u.id = s.usuario_id
                WHERE u.email_normalizado LIKE ?
                """, Integer.class, PREFIJO + "%@ganadoconecta.invalid");
    }

    private void borrarIntento(String dimension, String clave) throws Exception {
        jdbc.update("DELETE FROM identidad.intento_login WHERE dimension = ? AND clave_sha256 = ?",
                dimension, huella(clave));
    }

    private int cantidadIntento(String dimension, String clave) throws Exception {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identidad.intento_login
                WHERE dimension = ? AND clave_sha256 = ?
                """, Integer.class, dimension, huella(clave));
    }

    private byte[] huella(String clave) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(clave.getBytes(StandardCharsets.UTF_8));
    }

    private record Contexto(RelojMutable reloj, ServicioJwtRs256 jwt,
            IniciarSesion login, RenovarSesion renovar, ValidarTokenAcceso validar) {
    }

    private record CasoInvalido(String correo, String clave, String origen) {
    }

    private static final class RelojMutable extends Clock {
        private Instant actual;

        private RelojMutable(Instant actual) {
            this.actual = actual;
        }

        void fijar(Instant nuevo) {
            actual = nuevo;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return actual;
        }
    }
}
