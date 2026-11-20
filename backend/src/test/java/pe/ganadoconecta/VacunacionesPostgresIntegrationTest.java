package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties = {"debug=false", "logging.level.root=WARN", "ganadoconecta.seguridad.origen-permitido=https://127.0.0.1:5173"})
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named="ganadoconecta.postgres.integration-test", matches="true")
@Transactional
class VacunacionesPostgresIntegrationTest extends SoporteJwtPostgres {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TokensAcceso tokens;
    UUID usuario, sesion, ganaderia, animal;
    String jwt;
    @BeforeEach void preparar() {
        usuario=UUID.randomUUID(); sesion=UUID.randomUUID(); ganaderia=UUID.randomUUID(); animal=UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash,estado) VALUES (?,?,'$argon2id$fixture','ACTIVO')", usuario, usuario+"@sanidad.invalid");
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP + interval '1 hour')", sesion, usuario);
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id,estado) VALUES (?,'Prueba aislada',?,'ACTIVA')", ganaderia, usuario);
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol,estado) VALUES (?,?,?,'PROPIETARIO','ACTIVA')", UUID.randomUUID(), ganaderia, usuario);
        jdbc.update("INSERT INTO ganado.animal(id,ganaderia_id,sexo,creado_por,creado_en) VALUES (?,?,'HEMBRA',?,CURRENT_TIMESTAMP)", animal, ganaderia, usuario);
        jwt=tokens.emitir(usuario,sesion,Instant.now(),Instant.now().plusSeconds(600)).valor();
    }
    String ruta() { return "/api/v1/ganaderias/"+ganaderia+"/animales/"+animal+"/vacunaciones"; }
    String rutaHistorial() { return "/api/v1/ganaderias/"+ganaderia+"/animales/"+animal+"/eventos-sanitarios"; }
    String cuerpo(UUID op, UUID evento, String nombre) throws Exception {
        return json.writeValueAsString(Map.of("operationId",op,"eventId",evento,"schemaVersion",1,
                "nombreVacuna",nombre,"fechaAplicacion","2026-01-01","responsable","Responsable sintético","proximaFecha","2026-02-01"));
    }
    org.springframework.test.web.servlet.ResultActions guardar(String body) throws Exception {
        return mvc.perform(post(ruta()).header("Authorization","Bearer "+jwt)
                .header("Origin","https://127.0.0.1:5173").with(csrf().asHeader()).contentType("application/json").content(body));
    }
    String cuerpoEvento(UUID op,UUID evento,String tipo,String descripcion) throws Exception{
        return json.writeValueAsString(Map.of("operationId",op,"eventId",evento,"schemaVersion",1,
                "tipo",tipo,"fecha","2026-01-02","descripcion",descripcion,
                "responsable","Responsable sintético","producto","Producto informado"));
    }
    org.springframework.test.web.servlet.ResultActions guardarEvento(String body)throws Exception{
        return mvc.perform(post(rutaHistorial()).header("Authorization","Bearer "+jwt)
                .header("Origin","https://127.0.0.1:5173").with(csrf().asHeader())
                .contentType("application/json").content(body));
    }
    String rutaAgenda(){return "/api/v1/ganaderias/"+ganaderia+"/agenda-sanitaria";}
    String cuerpoAgenda(UUID id,UUID animalId,String tipo,String fecha)throws Exception{return json.writeValueAsString(Map.of("actividadId",id,"animalId",animalId,"tipo",tipo,"fechaPrevista",fecha,"descripcion","Control futuro informado","observaciones","Dato sintético"));}
    org.springframework.test.web.servlet.ResultActions programar(String body)throws Exception{return mvc.perform(post(rutaAgenda()).header("Authorization","Bearer "+jwt).header("Origin","https://127.0.0.1:5173").with(csrf().asHeader()).contentType("application/json").content(body));}
    @Test void registraConsultaYReintentaSinDuplicar() throws Exception {
        var op=UUID.randomUUID(); var evento=UUID.randomUUID(); var body=cuerpo(op,evento,"Vacuna informada");
        guardar(body).andExpect(status().isCreated()).andExpect(jsonPath("$.resultado").value("APLICADO"));
        guardar(body).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("REPROCESADO"));
        guardar(cuerpo(op,evento,"Otro contenido")).andExpect(status().isConflict());
        mvc.perform(get(ruta()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nombreVacuna").value("Vacuna informada"));
    }
    @Test void validaYRechazaCsrfOrigenYAnonimo() throws Exception {
        var body=cuerpo(UUID.randomUUID(),UUID.randomUUID(),"Vacuna");
        guardar(cuerpo(UUID.randomUUID(),UUID.randomUUID()," ")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get(ruta())).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta()).header("Authorization","Bearer "+jwt).header("Origin","https://127.0.0.1:5173")
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(ruta()).header("Authorization","Bearer "+jwt).header("Origin","https://otro.invalid").with(csrf().asHeader())
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sanidad.evento_sanitario WHERE animal_id=?",Integer.class,animal)).isZero();
    }
    @Test void rechazaAnimalAjenoYMembresiaRevocada() throws Exception {
        UUID real=animal; animal=UUID.randomUUID();
        guardar(cuerpo(UUID.randomUUID(),UUID.randomUUID(),"Vacuna")).andExpect(status().isNotFound());
        animal=real;
        jdbc.update("UPDATE identidad.membresia SET estado='REVOCADA',revocado_en=CURRENT_TIMESTAMP WHERE usuario_id=?",usuario);
        guardar(cuerpo(UUID.randomUUID(),UUID.randomUUID(),"Vacuna")).andExpect(status().isForbidden());
        mvc.perform(get(ruta()).header("Authorization","Bearer "+jwt)).andExpect(status().isForbidden());
    }
    @Test void editorEscribeLectorConsultaYAjenoNoAccede() throws Exception {
        UUID miembro=UUID.randomUUID(); UUID sesionMiembro=UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash,estado) VALUES (?,?,'$argon2id$fixture','ACTIVO')",miembro,miembro+"@sanidad.invalid");
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP + interval '1 hour')",sesionMiembro,miembro);
        jwt=tokens.emitir(miembro,sesionMiembro,Instant.now(),Instant.now().plusSeconds(600)).valor();
        mvc.perform(get(ruta()).header("Authorization","Bearer "+jwt)).andExpect(status().isForbidden());
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol,estado) VALUES (?,?,?,'EDITOR','ACTIVA')",UUID.randomUUID(),ganaderia,miembro);
        guardar(cuerpo(UUID.randomUUID(),UUID.randomUUID(),"Vacuna")).andExpect(status().isCreated());
        jdbc.update("UPDATE identidad.membresia SET rol='LECTOR' WHERE usuario_id=?",miembro);
        guardar(cuerpo(UUID.randomUUID(),UUID.randomUUID(),"Vacuna")).andExpect(status().isForbidden());
        mvc.perform(get(ruta()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void historialRegistraTresTiposFiltraOrdenaYReprocesa()throws Exception{
        for(String tipo:new String[]{"DESPARASITACION","TRATAMIENTO_SANITARIO","REVISION_VETERINARIA"}){
            UUID op=UUID.randomUUID(),evento=UUID.randomUUID();String body=cuerpoEvento(op,evento,tipo,"Hecho "+tipo);
            guardarEvento(body).andExpect(status().isCreated());
            guardarEvento(body).andExpect(status().isOk()).andExpect(jsonPath("$.resultado").value("REPROCESADO"));
        }
        mvc.perform(get(rutaHistorial()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(rutaHistorial()+"?tipo=TRATAMIENTO_SANITARIO").header("Authorization","Bearer "+jwt))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].producto").value("Producto informado"));
    }
    @Test void historialValidaTipoCamposPermisoYAislamiento()throws Exception{
        guardarEvento(cuerpoEvento(UUID.randomUUID(),UUID.randomUUID(),"VACUNACION","No admitida aquí"))
                .andExpect(status().isUnprocessableEntity());
        guardarEvento(cuerpoEvento(UUID.randomUUID(),UUID.randomUUID(),"DESPARASITACION"," "))
                .andExpect(status().isUnprocessableEntity());
        jdbc.update("UPDATE identidad.membresia SET rol='LECTOR' WHERE usuario_id=?",usuario);
        guardarEvento(cuerpoEvento(UUID.randomUUID(),UUID.randomUUID(),"DESPARASITACION","Hecho"))
                .andExpect(status().isForbidden());
        mvc.perform(get(rutaHistorial()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
        animal=UUID.randomUUID();
        mvc.perform(get(rutaHistorial()).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
    }
    @Test void agendaProgramaListaCompletaSinCrearEventoClinico()throws Exception{
        UUID actividad=UUID.randomUUID();String body=cuerpoAgenda(actividad,animal,"VACUNACION","2099-12-01");
        programar(body).andExpect(status().isCreated()).andExpect(jsonPath("$.estadoVisible").value("PROXIMA"));
        programar(body).andExpect(status().isCreated()).andExpect(jsonPath("$.actividadId").value(actividad.toString()));
        mvc.perform(get(rutaAgenda()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post(rutaAgenda()+"/"+actividad+"/completar").header("Authorization","Bearer "+jwt).header("Origin","https://127.0.0.1:5173").with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoVisible").value("COMPLETADA"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sanidad.evento_sanitario WHERE animal_id=?",Integer.class,animal)).isZero();
    }
    @Test void agendaValidaRolesAislamientoYVencimientoVisible()throws Exception{
        programar(cuerpoAgenda(UUID.randomUUID(),animal,"VACUNACION","2020-01-01")).andExpect(status().isUnprocessableEntity());
        UUID vencida=UUID.randomUUID();jdbc.update("INSERT INTO sanidad.actividad_agenda(id,ganaderia_id,animal_id,tipo,fecha_prevista,descripcion,estado,version,creado_por,creado_en) VALUES(?,?,?,'REVISION_VETERINARIA','2020-01-01','Revisión pendiente','PENDIENTE',1,?,CURRENT_TIMESTAMP)",vencida,ganaderia,animal,usuario);
        mvc.perform(get(rutaAgenda()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[0].estadoVisible").value("VENCIDA"));
        jdbc.update("UPDATE identidad.membresia SET rol='LECTOR' WHERE usuario_id=?",usuario);
        programar(cuerpoAgenda(UUID.randomUUID(),animal,"TRATAMIENTO_SANITARIO","2099-12-01")).andExpect(status().isForbidden());
        mvc.perform(get(rutaAgenda()).header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
    }
}
