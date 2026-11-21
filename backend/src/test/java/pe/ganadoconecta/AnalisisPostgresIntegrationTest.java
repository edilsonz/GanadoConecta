package pe.ganadoconecta;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class AnalisisPostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired TokensAcceso tokens;
    private UUID propietario;
    private UUID ganaderia;
    private UUID animal;

    @BeforeEach void preparar() {
        propietario = usuario();
        ganaderia = ganaderia(propietario);
        animal = animal(ganaderia, propietario);
    }

    @Test void analizaDatosConfirmadosConLimitesInclusivosYNoDevuelveNotasPrivadas() throws Exception {
        var otroAnimal = animal(ganaderia, propietario);
        jdbc.update("""
                INSERT INTO sanidad.evento_sanitario(id,ganaderia_id,animal_id,tipo,fecha_evento,descripcion,creado_por,creado_en)
                SELECT gen_random_uuid(),?,?,'DESPARASITACION',fecha,'Descripción privada',?,now()
                FROM (VALUES (DATE '2026-09-01'), (DATE '2026-09-30'), (DATE '2026-10-01')) AS fechas(fecha)
                """, ganaderia, animal, propietario);
        jdbc.update("""
                INSERT INTO reproduccion.evento_reproductivo(id,ganaderia_id,animal_id,tipo,fecha_evento,estado_gestacion,creado_por,creado_en)
                VALUES (?,?,?,'EVALUACION_GESTACION',DATE '2026-09-30','INDETERMINADA',?,now()),
                       (?,?,?,'MONTA',DATE '2026-09-30',null,?,now())
                """, UUID.randomUUID(), ganaderia, animal, propietario, UUID.randomUUID(), ganaderia, otroAnimal, propietario);
        jdbc.update("""
                INSERT INTO ganado.medicion_peso(id,ganaderia_id,animal_id,kilos,fecha,actor_id,registrado_en)
                SELECT gen_random_uuid(),?,?,kilos,fecha,?,now() FROM (VALUES
                    (90,DATE '2026-09-01'),(100.25,DATE '2026-09-15'),(98.50,DATE '2026-09-30'),(150,DATE '2026-10-01')
                ) AS pesos(kilos,fecha)
                """, ganaderia, animal, propietario);
        mvc.perform(periodo(ganaderia, sesion(propietario)).param("animalId", animal.toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.periodo.desde").value("2026-09-01"))
                .andExpect(jsonPath("$.periodo.hasta").value("2026-09-30"))
                .andExpect(jsonPath("$.sanidad.total").value(2))
                .andExpect(jsonPath("$.sanidad.porTipo[0].tipo").value("DESPARASITACION"))
                .andExpect(jsonPath("$.sanidad.porTipo[0].descripcion").doesNotExist())
                .andExpect(jsonPath("$.reproduccion.montas").value(0))
                .andExpect(jsonPath("$.reproduccion.indeterminadas").value(1))
                .andExpect(jsonPath("$.pesajes.length()").value(2))
                .andExpect(jsonPath("$.pesajes[0].fecha").value("2026-09-15"))
                .andExpect(jsonPath("$.pesajes[0].variacion").value(nullValue()))
                .andExpect(jsonPath("$.pesajes[1].fecha").value("2026-09-30"))
                .andExpect(jsonPath("$.pesajes[1].variacion").value(-1.75));
    }

    @Test void devuelveAlertasEstablesYRetiraLasCompletadasSinCrearEventos() throws Exception {
        var hoy = LocalDate.now(ZoneId.of("America/Lima"));
        var vencida = actividad(ganaderia, animal, propietario, hoy.minusDays(1), "REVISION_VETERINARIA");
        var campana = actividad(ganaderia, null, propietario, hoy, "CAMPANA_VACUNACION");
        var segundo = animal(ganaderia, propietario);
        jdbc.update("INSERT INTO sanidad.participante_agenda(actividad_id,ganaderia_id,animal_id) VALUES (?,?,?),(?,?,?)",
                campana, ganaderia, animal, campana, ganaderia, segundo);
        actividad(ganaderia, animal, propietario, hoy.plusDays(1), "REVISION_VETERINARIA");
        var externa = usuario();
        var ajena = ganaderia(externa);
        actividad(ajena, animal(ajena, externa), externa, hoy, "REVISION_VETERINARIA");
        var token = sesion(propietario);
        for (int llamada = 0; llamada < 2; llamada++) {
            mvc.perform(alertas(ganaderia, token)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.fechaCorte").value(hoy.toString()))
                    .andExpect(jsonPath("$.avisos.length()").value(2))
                    .andExpect(jsonPath("$.avisos[0].actividadId").value(vencida.toString()))
                    .andExpect(jsonPath("$.avisos[0].estado").value("VENCIDA"))
                    .andExpect(jsonPath("$.avisos[1].actividadId").value(campana.toString()))
                    .andExpect(jsonPath("$.avisos[1].estado").value("PENDIENTE"))
                    .andExpect(jsonPath("$.avisos[1].observaciones").doesNotExist())
                    .andExpect(jsonPath("$.avisos[1].participantes").doesNotExist());
        }
        jdbc.update("""
                UPDATE sanidad.actividad_agenda SET estado='COMPLETADA',version=2,completado_en=now(),completado_por=?
                WHERE id=?
                """, propietario, vencida);
        mvc.perform(alertas(ganaderia, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(1));
        org.junit.jupiter.api.Assertions.assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM sanidad.evento_sanitario WHERE ganaderia_id=?", Long.class, ganaderia));
    }

    @Test void revalidaMembresiaYNiegaRecursosAjenosConUnaSesionValida() throws Exception {
        var lector = usuario();
        miembro(ganaderia, lector, "LECTOR");
        var externo = usuario();
        var ajena = ganaderia(externo);
        var otroAnimal = animal(ajena, externo);
        var token = sesion(lector);
        mvc.perform(get(ruta(ganaderia, "analisis"))).andExpect(status().isUnauthorized());
        mvc.perform(get(ruta(ganaderia, "alertas"))).andExpect(status().isUnauthorized());
        mvc.perform(periodo(ganaderia, token)).andExpect(status().isOk());
        mvc.perform(alertas(ganaderia, token)).andExpect(status().isOk());
        mvc.perform(periodo(ganaderia, token).param("animalId", otroAnimal.toString())).andExpect(status().isForbidden());
        mvc.perform(periodo(ajena, token)).andExpect(status().isForbidden());
        mvc.perform(alertas(ajena, token)).andExpect(status().isForbidden());
        jdbc.update("UPDATE identidad.membresia SET estado='REVOCADA',revocado_en=now() WHERE usuario_id=? AND ganaderia_id=?",
                lector, ganaderia);
        mvc.perform(periodo(ganaderia, token)).andExpect(status().isForbidden());
        mvc.perform(alertas(ganaderia, token)).andExpect(status().isForbidden());
    }

    @Test void distingueConsultaVaciaDeParametrosInvalidosYRechazaAmpliarAmbito() throws Exception {
        var token = sesion(propietario);
        mvc.perform(periodo(ganaderia, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sanidad.total").value(0)).andExpect(jsonPath("$.pesajes").isEmpty());
        mvc.perform(alertas(ganaderia, token)).andExpect(status().isOk()).andExpect(jsonPath("$.avisos").isEmpty());
        mvc.perform(periodo(ganaderia, token).param("desde", "2026-09-02")).andExpect(status().isBadRequest());
        mvc.perform(periodo(ganaderia, token).param("animalId", "")).andExpect(status().isBadRequest());
        mvc.perform(periodo(ganaderia, token).param("ganaderiaId", UUID.randomUUID().toString())).andExpect(status().isBadRequest());
        mvc.perform(alertas(ganaderia, token).param("incluirAjenas", "true")).andExpect(status().isBadRequest());
    }

    private UUID usuario() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')",
                id, id + "@analisis.invalid");
        return id;
    }

    private UUID ganaderia(UUID actor) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Sintética',?)", id, actor);
        miembro(id, actor, "PROPIETARIO");
        return id;
    }

    private void miembro(UUID finca, UUID actor, String rol) {
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,?)",
                UUID.randomUUID(), finca, actor, rol);
    }

    private UUID animal(UUID finca, UUID actor) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO ganado.animal(id,ganaderia_id,sexo,creado_por,creado_en) VALUES (?,?,'HEMBRA',?,now())",
                id, finca, actor);
        return id;
    }

    private UUID actividad(UUID finca, UUID res, UUID actor, LocalDate fecha, String tipo) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sanidad.actividad_agenda(id,ganaderia_id,animal_id,tipo,fecha_prevista,descripcion,creado_por,creado_en)
                VALUES (?,?,?,?,?,'Actividad sintética',?,now())
                """, id, finca, res, tipo, fecha, actor);
        return id;
    }

    private String sesion(UUID usuario) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')",
                id, usuario);
        var ahora = Instant.now();
        return "Bearer " + tokens.emitir(usuario, id, ahora, ahora.plusSeconds(600)).valor();
    }

    private static String ruta(UUID finca, String recurso) { return "/api/v1/ganaderias/" + finca + "/" + recurso; }

    private MockHttpServletRequestBuilder periodo(UUID finca, String token) {
        return get(ruta(finca, "analisis")).header("Authorization", token).param("desde", "2026-09-01").param("hasta", "2026-09-30");
    }

    private MockHttpServletRequestBuilder alertas(UUID finca, String token) {
        return get(ruta(finca, "alertas")).header("Authorization", token);
    }
}
