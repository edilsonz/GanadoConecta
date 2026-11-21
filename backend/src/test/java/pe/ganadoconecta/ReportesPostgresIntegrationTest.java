package pe.ganadoconecta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "ganadoconecta.postgres.integration-test", matches = "true")
@Transactional
class ReportesPostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired TokensAcceso tokens;
    UUID propietario, ganaderia, animal;

    @BeforeEach void preparar() {
        propietario = usuario();
        ganaderia = ganaderia(propietario);
        animal = animal(ganaderia, propietario);
    }

    @Test void cuentaSoloHechosFiltradosConCampanasYPartosSinDuplicacion() throws Exception {
        UUID segundo = animal(ganaderia, propietario);
        UUID vacuna = sanitario(animal, "VACUNACION", "2026-09-01");
        sanitario(segundo, "DESPARASITACION", "2026-09-30");
        sanitario(animal, "VACUNACION", "2026-08-31");
        var campana = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sanidad.actividad_agenda(id,ganaderia_id,tipo,fecha_prevista,descripcion,estado,version,
                    creado_por,creado_en,completado_por,completado_en,fecha_atencion,nombre_vacuna)
                VALUES (?,?,'CAMPANA_VACUNACION',DATE '2026-09-01','Sintética','COMPLETADA',2,?,now(),?,now(),DATE '2026-09-01','Sintética')
                """, campana, ganaderia, propietario, propietario);
        jdbc.update("INSERT INTO sanidad.participante_agenda VALUES (?,?,?,?),(?,?,?,NULL)",
                campana, ganaderia, animal, vacuna, campana, ganaderia, segundo);
        eventoReproductivo("MONTA", null);
        eventoReproductivo("INSEMINACION", null);
        eventoReproductivo("EVALUACION_GESTACION", "CONFIRMADA");
        eventoReproductivo("EVALUACION_GESTACION", "NO_CONFIRMADA");
        var parto = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reproduccion.parto(id,ganaderia_id,madre_id,fecha_parto,resultado,creado_por,creado_en)
                VALUES (?,?,?,DATE '2026-09-30','CON_CRIAS_REGISTRADAS',?,now())
                """, parto, ganaderia, animal, propietario);
        jdbc.update("INSERT INTO reproduccion.parto_cria(ganaderia_id,parto_id,cria_id) VALUES (?,?,?)", ganaderia, parto, segundo);
        String token = sesion(propietario);
        mvc.perform(periodo(ganaderia, token)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.sanidad.total").value(2))
                .andExpect(jsonPath("$.sanidad.porTipo[0].tipo").value("DESPARASITACION"))
                .andExpect(jsonPath("$.sanidad.porTipo[0].total").value(1))
                .andExpect(jsonPath("$.sanidad.porTipo[1].tipo").value("VACUNACION"))
                .andExpect(jsonPath("$.sanidad.porTipo[1].total").value(1))
                .andExpect(jsonPath("$.reproduccion.montas").value(1))
                .andExpect(jsonPath("$.reproduccion.inseminaciones").value(1))
                .andExpect(jsonPath("$.reproduccion.evaluaciones").value(2))
                .andExpect(jsonPath("$.reproduccion.confirmadas").value(1))
                .andExpect(jsonPath("$.reproduccion.noConfirmadas").value(1))
                .andExpect(jsonPath("$.reproduccion.partos").value(1))
                .andExpect(jsonPath("$.reproduccion.crias").value(1))
                .andExpect(jsonPath("$.peso.mediciones").isEmpty());
        mvc.perform(periodo(ganaderia, token).param("animalId", segundo.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sanidad.total").value(1))
                .andExpect(jsonPath("$.reproduccion.evaluaciones").value(0))
                .andExpect(jsonPath("$.reproduccion.partos").value(0))
                .andExpect(jsonPath("$.reproduccion.crias").value(0));
    }

    @Test void permiteRolesVigentesExcluyeAjenosYRevocados() throws Exception {
        var lector = usuario();
        var editor = usuario();
        var externo = usuario();
        var ajena = ganaderia(externo);
        var animalAjeno = animal(ajena, externo);
        miembro(ganaderia, lector, "LECTOR");
        miembro(ganaderia, editor, "EDITOR");
        mvc.perform(get(ruta(ganaderia))).andExpect(status().isUnauthorized());
        for (UUID actor : new UUID[]{propietario, lector, editor}) {
            mvc.perform(periodo(ganaderia, sesion(actor))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.sanidad.total").value(0));
        }
        var acceso = sesion(lector);
        mvc.perform(periodo(ganaderia, acceso).param("animalId", animalAjeno.toString())).andExpect(status().isForbidden());
        mvc.perform(periodo(ganaderia, acceso).param("animalId", UUID.randomUUID().toString())).andExpect(status().isForbidden());
        mvc.perform(periodo(ajena, acceso)).andExpect(status().isForbidden());
        mvc.perform(periodo(ganaderia, sesion(externo))).andExpect(status().isForbidden());
        jdbc.update("UPDATE identidad.membresia SET estado='REVOCADA',revocado_en=now() WHERE usuario_id=? AND ganaderia_id=?", lector, ganaderia);
        mvc.perform(periodo(ganaderia, acceso)).andExpect(status().isForbidden());
    }

    @Test void calculaPesoPorAnimalConLimiteYDeltaDentroDelPeriodo() throws Exception {
        jdbc.update("""
                INSERT INTO ganado.medicion_peso(id,ganaderia_id,animal_id,kilos,fecha,actor_id,registrado_en)
                SELECT gen_random_uuid(),?,?,100+n,DATE '2026-09-01',?,now()+n*interval '1 second'
                FROM generate_series(1,52) n
                """, ganaderia, animal, propietario);
        jdbc.update("""
                INSERT INTO ganado.medicion_peso(id,ganaderia_id,animal_id,kilos,fecha,actor_id,registrado_en)
                VALUES (gen_random_uuid(),?,?,999,DATE '2026-08-31',?,now())
                """, ganaderia, animal, propietario);
        var token = sesion(propietario);
        mvc.perform(periodo(ganaderia, token).param("animalId", animal.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.peso.total").value(52)).andExpect(jsonPath("$.peso.mediciones.length()").value(50))
                .andExpect(jsonPath("$.peso.mediciones[0].kilos").value(103))
                .andExpect(jsonPath("$.peso.mediciones[0].variacion").value(1))
                .andExpect(jsonPath("$.peso.ultimo.kilos").value(152))
                .andExpect(jsonPath("$.peso.ultimo.fecha").value("2026-09-01"));
        mvc.perform(periodo(ganaderia, token)).andExpect(status().isOk()).andExpect(jsonPath("$.peso.total").value(0));
    }

    @Test void rechazaFechasOParametrosInvalidosYRepresentaPesoVacio() throws Exception {
        var token = sesion(propietario);
        mvc.perform(get(ruta(ganaderia)).header("Authorization", token)
                .param("desde", "2026-09-30").param("hasta", "2026-09-01")).andExpect(status().isBadRequest());
        mvc.perform(get(ruta(ganaderia)).header("Authorization", token)
                .param("desde", "mal").param("hasta", "2026-09-30")).andExpect(status().isBadRequest());
        mvc.perform(periodo(ganaderia, token).param("privado", "true")).andExpect(status().isBadRequest());
        mvc.perform(periodo(ganaderia, token).param("animalId", animal.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.peso.mediciones").isEmpty())
                .andExpect(jsonPath("$.reproduccion.crias").value(0));
    }

    private UUID sanitario(UUID animal, String tipo, String fecha) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sanidad.evento_sanitario(id,ganaderia_id,animal_id,tipo,fecha_evento,nombre,descripcion,creado_por,creado_en)
                VALUES (?,?,?,?,?::date,?,'Sintético',?,now())
                """, id, ganaderia, animal, tipo, fecha, "VACUNACION".equals(tipo) ? "Vacuna sintética" : null, propietario);
        return id;
    }

    private void eventoReproductivo(String tipo, String estado) {
        jdbc.update("""
                INSERT INTO reproduccion.evento_reproductivo(id,ganaderia_id,animal_id,tipo,fecha_evento,estado_gestacion,creado_por,creado_en)
                VALUES (?,?,?,?,DATE '2026-09-01',?,?,now())
                """, UUID.randomUUID(), ganaderia, animal, tipo, estado, propietario);
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')",
                id, id + "@reportes.invalid");
        return id;
    }

    private UUID ganaderia(UUID propietario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Sintética',?)", id, propietario);
        miembro(id, propietario, "PROPIETARIO");
        return id;
    }

    private void miembro(UUID finca, UUID usuario, String rol) {
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,?)",
                UUID.randomUUID(), finca, usuario, rol);
    }

    private UUID animal(UUID finca, UUID actor) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO ganado.animal(id,ganaderia_id,sexo,creado_por,creado_en,observaciones) VALUES (?,?,'HEMBRA',?,now(),'Nota privada sintética')",
                id, finca, actor);
        return id;
    }

    private String sesion(UUID usuario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')", id, usuario);
        var ahora = Instant.now();
        return "Bearer " + tokens.emitir(usuario, id, ahora, ahora.plusSeconds(600)).valor();
    }
    private String ruta(UUID finca) { return "/api/v1/ganaderias/" + finca + "/reportes"; }
    private MockHttpServletRequestBuilder periodo(UUID finca, String token) {
        return get(ruta(finca)).header("Authorization", token).param("desde", "2026-09-01").param("hasta", "2026-09-30");
    }
}
