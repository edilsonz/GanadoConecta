package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConflictoRegistroAnimalException;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.ResultadoRegistroAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.SolicitudRegistroAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.Categoria;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioGanaderias;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=WARN"})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
class RegistrarAnimalPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String PREFIJO = "animal-test-";

    @Autowired
    private RegistrarAnimal registrar;
    @Autowired
    private RepositorioGanaderias repositorioGanaderias;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transacciones;

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
    void propietarioYEditorRegistranConIntegridadEIdempotencia() {
        Escenario escenario = crearEscenario();
        SolicitudRegistroAnimal altaPropietario = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Luna", "A-10");

        ResultadoRegistroAnimal creado = registrar.ejecutar(altaPropietario,
                escenario.propietarioId(), escenario.sesionPropietarioId());
        ResultadoRegistroAnimal repetido = registrar.ejecutar(altaPropietario,
                escenario.propietarioId(), escenario.sesionPropietarioId());
        ResultadoRegistroAnimal editor = registrar.ejecutar(solicitud(
                        escenario.ganaderiaId(), UUID.randomUUID(), UUID.randomUUID(),
                        "Sol", "A-10"),
                escenario.editorId(), escenario.sesionEditorId());
        SolicitudRegistroAnimal soloReverso = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Nube", null);
        soloReverso = new SolicitudRegistroAnimal(soloReverso.ganaderiaId(),
                soloReverso.animalId(), soloReverso.operacionId(), soloReverso.tipo(),
                soloReverso.schemaVersion(), soloReverso.baseVersion(),
                new DatosAnimal(Sexo.HEMBRA, "Nube", null, "A-10", null, null,
                        "Criolla", null, null, Categoria.VAQUILLA, null));
        ResultadoRegistroAnimal reverso = registrar.ejecutar(soloReverso,
                escenario.editorId(), escenario.sesionEditorId());

        assertThat(creado.estado()).isEqualTo(
                ResultadoRegistroAnimal.EstadoResultado.APLICADO);
        assertThat(repetido.estado()).isEqualTo(
                ResultadoRegistroAnimal.EstadoResultado.REPROCESADO);
        assertThat(repetido.confirmadoEn()).isEqualTo(creado.confirmadoEn());
        assertThat(editor.areteCoincidente()).isTrue();
        assertThat(reverso.areteCoincidente()).isFalse();
        assertThat(cantidad("ganado.animal", "ganaderia_id", escenario.ganaderiaId()))
                .isEqualTo(3);
        assertThat(cantidad("ganado.operacion_confirmada", "ganaderia_id",
                escenario.ganaderiaId())).isEqualTo(3);
    }

    @Test
    void rechazaLectorAjenoYRevocadoSinPersistir() {
        Escenario escenario = crearEscenario();

        assertThatThrownBy(() -> registrar.ejecutar(solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Lector", null),
                escenario.lectorId(), escenario.sesionLectorId()))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThatThrownBy(() -> registrar.ejecutar(solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Ajeno", null),
                escenario.ajenoId(), escenario.sesionAjenoId()))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);

        revocarDirectamente(escenario.ganaderiaId(), escenario.editorId());
        assertThatThrownBy(() -> registrar.ejecutar(solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Revocado", null),
                escenario.editorId(), escenario.sesionEditorId()))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThat(cantidad("ganado.animal", "ganaderia_id", escenario.ganaderiaId()))
                .isZero();
    }

    @Test
    void detectaConflictosSinCrearRegistrosParciales() {
        Escenario escenario = crearEscenario();
        UUID animalId = UUID.randomUUID();
        UUID operacionId = UUID.randomUUID();
        registrar.ejecutar(solicitud(escenario.ganaderiaId(), animalId, operacionId,
                "Original", null), escenario.propietarioId(),
                escenario.sesionPropietarioId());

        assertThatThrownBy(() -> registrar.ejecutar(solicitud(escenario.ganaderiaId(),
                animalId, operacionId, "Cambiado", null), escenario.propietarioId(),
                escenario.sesionPropietarioId()))
                .isInstanceOf(ConflictoRegistroAnimalException.class);
        assertThatThrownBy(() -> registrar.ejecutar(solicitud(escenario.ganaderiaId(),
                animalId, UUID.randomUUID(), "Original", null), escenario.propietarioId(),
                escenario.sesionPropietarioId()))
                .isInstanceOf(ConflictoRegistroAnimalException.class);

        assertThat(cantidad("ganado.animal", "ganaderia_id", escenario.ganaderiaId()))
                .isOne();
        assertThat(cantidad("ganado.operacion_confirmada", "ganaderia_id",
                escenario.ganaderiaId())).isOne();
    }

    @Test
    void dosReintentosConcurrentesCreanUnaSolaFicha() throws Exception {
        Escenario escenario = crearEscenario();
        SolicitudRegistroAnimal solicitud = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Concurrente", null);

        CompletableFuture<ResultadoRegistroAnimal> primero = CompletableFuture.supplyAsync(
                () -> registrar.ejecutar(solicitud, escenario.editorId(),
                        escenario.sesionEditorId()));
        CompletableFuture<ResultadoRegistroAnimal> segundo = CompletableFuture.supplyAsync(
                () -> registrar.ejecutar(solicitud, escenario.editorId(),
                        escenario.sesionEditorId()));
        ResultadoRegistroAnimal uno = primero.get(10, TimeUnit.SECONDS);
        ResultadoRegistroAnimal dos = segundo.get(10, TimeUnit.SECONDS);

        assertThat(Stream.of(uno.estado(), dos.estado()))
                .containsExactlyInAnyOrder(
                        ResultadoRegistroAnimal.EstadoResultado.APLICADO,
                        ResultadoRegistroAnimal.EstadoResultado.REPROCESADO);
        assertThat(cantidad("ganado.animal", "id", solicitud.animalId().valor())).isOne();
        assertThat(cantidad("ganado.operacion_confirmada", "operacion_id",
                solicitud.operacionId().valor())).isOne();
    }

    @Test
    void unidadDeTrabajoRevierteAnimalYComprobanteAnteFalloPosterior() {
        Escenario escenario = crearEscenario();
        SolicitudRegistroAnimal solicitud = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Temporal", null);

        assertThatThrownBy(() -> transacciones.executeWithoutResult(estado -> {
            registrar.ejecutar(solicitud, escenario.propietarioId(),
                    escenario.sesionPropietarioId());
            throw new IllegalStateException("fallo sintético posterior");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(cantidad("ganado.animal", "id", solicitud.animalId().valor())).isZero();
        assertThat(cantidad("ganado.operacion_confirmada", "operacion_id",
                solicitud.operacionId().valor())).isZero();
    }

    @Test
    void postgresRefuerzaSexoYCoherenciaDeCategoria() {
        Escenario escenario = crearEscenario();
        assertThatThrownBy(() -> transacciones.executeWithoutResult(estado -> jdbc.update("""
                INSERT INTO ganado.animal (
                    id, ganaderia_id, sexo, categoria, estado, version, creado_por, creado_en
                ) VALUES (?, ?, 'MACHO', 'VACA', 'ACTIVO', 1, ?, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), escenario.ganaderiaId(),
                escenario.propietarioId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revocacionConfirmadaPrimeroImpideElRegistroConcurrente() throws Exception {
        Escenario escenario = crearEscenario();
        SolicitudRegistroAnimal solicitud = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Carrera revoca", null);
        CompletableFuture<Throwable> intento = new CompletableFuture<>();

        transacciones.executeWithoutResult(estado -> {
            bloquearGanaderia(escenario.ganaderiaId());
            CompletableFuture.runAsync(() -> {
                try {
                    registrar.ejecutar(solicitud, escenario.editorId(),
                            escenario.sesionEditorId());
                    intento.complete(null);
                } catch (Throwable error) {
                    intento.complete(error);
                }
            });
            repositorioGanaderias.revocarMembresia(escenario.propietarioId(),
                    escenario.sesionPropietarioId(), escenario.ganaderiaId(),
                    escenario.editorId(), Instant.now());
        });

        assertThat(intento.get(10, TimeUnit.SECONDS))
                .isInstanceOf(AccesoGanaderiaDenegadoException.class);
        assertThat(cantidad("ganado.animal", "id", solicitud.animalId().valor())).isZero();
    }

    @Test
    void registroConfirmadoPrimeroPrecedeALaRevocacionConcurrente() throws Exception {
        Escenario escenario = crearEscenario();
        SolicitudRegistroAnimal solicitud = solicitud(escenario.ganaderiaId(),
                UUID.randomUUID(), UUID.randomUUID(), "Carrera registra", null);
        CompletableFuture<RepositorioGanaderias.ResultadoRevocacion> revocacion =
                new CompletableFuture<>();

        transacciones.executeWithoutResult(estado -> {
            bloquearGanaderia(escenario.ganaderiaId());
            CompletableFuture.runAsync(() -> revocacion.complete(
                    repositorioGanaderias.revocarMembresia(
                            escenario.propietarioId(), escenario.sesionPropietarioId(),
                            escenario.ganaderiaId(), escenario.editorId(), Instant.now())));
            registrar.ejecutar(solicitud, escenario.editorId(),
                    escenario.sesionEditorId());
        });

        assertThat(revocacion.get(10, TimeUnit.SECONDS))
                .isEqualTo(RepositorioGanaderias.ResultadoRevocacion.REVOCADA);
        assertThat(cantidad("ganado.animal", "id", solicitud.animalId().valor())).isOne();
        assertThat(estadoMembresia(escenario.ganaderiaId(), escenario.editorId()))
                .isEqualTo("REVOCADA");
    }

    private Escenario crearEscenario() {
        UUID propietario = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        UUID lector = UUID.randomUUID();
        UUID ajeno = UUID.randomUUID();
        UUID ganaderia = UUID.randomUUID();
        UUID sesionPropietario = UUID.randomUUID();
        UUID sesionEditor = UUID.randomUUID();
        UUID sesionLector = UUID.randomUUID();
        UUID sesionAjeno = UUID.randomUUID();
        transacciones.executeWithoutResult(estado -> {
            crearUsuario(propietario, "propietario");
            crearUsuario(editor, "editor");
            crearUsuario(lector, "lector");
            crearUsuario(ajeno, "ajeno");
            jdbc.update("""
                    INSERT INTO identidad.ganaderia
                        (id, nombre, propietario_usuario_id, estado)
                    VALUES (?, 'Ganadería sintética', ?, 'ACTIVA')
                    """, ganaderia, propietario);
            crearMembresia(ganaderia, propietario, "PROPIETARIO");
            crearMembresia(ganaderia, editor, "EDITOR");
            crearMembresia(ganaderia, lector, "LECTOR");
            crearSesion(sesionPropietario, propietario);
            crearSesion(sesionEditor, editor);
            crearSesion(sesionLector, lector);
            crearSesion(sesionAjeno, ajeno);
        });
        return new Escenario(ganaderia, propietario, sesionPropietario,
                editor, sesionEditor, lector, sesionLector, ajeno, sesionAjeno);
    }

    private void crearUsuario(UUID id, String sufijo) {
        jdbc.update("""
                INSERT INTO identidad.usuario
                    (id, email_normalizado, password_hash, estado)
                VALUES (?, ?, '$argon2id$prueba-sintetica', 'ACTIVO')
                """, id, PREFIJO + sufijo + "-" + id + "@ganadoconecta.invalid");
    }

    private void crearMembresia(UUID ganaderia, UUID usuario, String rol) {
        jdbc.update("""
                INSERT INTO identidad.membresia
                    (id, ganaderia_id, usuario_id, rol, estado)
                VALUES (?, ?, ?, ?, 'ACTIVA')
                """, UUID.randomUUID(), ganaderia, usuario, rol);
    }

    private void crearSesion(UUID sesion, UUID usuario) {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO identidad.sesion_dispositivo
                    (id, usuario_id, creado_en, vence_en)
                VALUES (?, ?, ?, ?)
                """, sesion, usuario, ahora.minusMinutes(1), ahora.plusHours(1));
    }

    private SolicitudRegistroAnimal solicitud(UUID ganaderia, UUID animal,
            UUID operacion, String nombre, String arete) {
        return new SolicitudRegistroAnimal(new GanaderiaId(ganaderia),
                new AnimalId(animal), new OperacionId(operacion),
                "CREAR_ANIMAL", 1, 0,
                new DatosAnimal(Sexo.HEMBRA, nombre, arete, null, null, null,
                        "Criolla", null, null, Categoria.VAQUILLA, null));
    }

    private void bloquearGanaderia(UUID ganaderiaId) {
        jdbc.queryForObject("SELECT id FROM identidad.ganaderia WHERE id = ? FOR UPDATE",
                UUID.class, ganaderiaId);
    }

    private void revocarDirectamente(UUID ganaderiaId, UUID usuarioId) {
        jdbc.update("""
                UPDATE identidad.membresia
                SET estado = 'REVOCADA', revocado_en = CURRENT_TIMESTAMP,
                    actualizado_en = CURRENT_TIMESTAMP
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, ganaderiaId, usuarioId);
    }

    private int cantidad(String tabla, String columna, UUID valor) {
        if (!(tabla.equals("ganado.animal")
                || tabla.equals("ganado.operacion_confirmada"))) {
            throw new IllegalArgumentException("Tabla no permitida");
        }
        if (!(columna.equals("id") || columna.equals("ganaderia_id")
                || columna.equals("operacion_id"))) {
            throw new IllegalArgumentException("Columna no permitida");
        }
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE "
                + columna + " = ?", Integer.class, valor);
    }

    private String estadoMembresia(UUID ganaderiaId, UUID usuarioId) {
        return jdbc.queryForObject("""
                SELECT estado FROM identidad.membresia
                WHERE ganaderia_id = ? AND usuario_id = ?
                """, String.class, ganaderiaId, usuarioId);
    }

    private record Escenario(UUID ganaderiaId,
            UUID propietarioId, UUID sesionPropietarioId,
            UUID editorId, UUID sesionEditorId,
            UUID lectorId, UUID sesionLectorId,
            UUID ajenoId, UUID sesionAjenoId) {
    }
}
