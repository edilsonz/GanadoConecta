package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

@SpringBootTest(properties="ganadoconecta.seguridad.origen-permitido=https://127.0.0.1:5173")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named="ganadoconecta.postgres.integration-test",matches="true")
@Transactional
class CatalogoPostgresIntegrationTest extends SoporteJwtPostgres {
    private static final String PUBLICO="/api/v1/catalogo";
    @TempDir static Path fotos;
    @DynamicPropertySource static void fotos(DynamicPropertyRegistry registro) {
        registro.add("GANADOCONECTA_FOTOS_DIRECTORIO", () -> fotos.toString());
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokensAcceso tokens;
    UUID g = UUID.randomUUID(), animal = UUID.randomUUID(), usuario = UUID.randomUUID();
    String jwt;

    @BeforeEach void preparar() {
        var sesion = UUID.randomUUID();
        jdbc.update("INSERT INTO identidad.usuario(id,email_normalizado,password_hash) VALUES (?,?,'$argon2id$fixture')",
                usuario,usuario+"@catalogo.invalid");
        jdbc.update("INSERT INTO identidad.sesion_dispositivo(id,usuario_id,creado_en,vence_en) VALUES (?,?,now(),now()+interval '1 hour')",sesion,usuario);
        jdbc.update("INSERT INTO identidad.ganaderia(id,nombre,propietario_usuario_id) VALUES (?,'Ganadería sintética',?)",g,usuario);
        jdbc.update("INSERT INTO identidad.membresia(id,ganaderia_id,usuario_id,rol) VALUES (?,?,?,'PROPIETARIO')",UUID.randomUUID(),g,usuario);
        jdbc.update("""
                INSERT INTO ganado.animal(id,ganaderia_id,sexo,categoria,raza,arete_frente,observaciones,creado_por,creado_en)
                VALUES (?,?,'HEMBRA','VACA','Cruce','PRIVADO','NO PUBLICAR',?,now())
                """,animal,g,usuario);
        Instant ahora=Instant.now();
        jwt="Bearer "+tokens.emitir(usuario,sesion,ahora,ahora.plusSeconds(600)).valor();
    }

    @Test void autorizacionPublicacionProyeccionFiltrosYPausa() throws Exception {
        UUID id=UUID.randomUUID(), madre=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ganado.animal(id,ganaderia_id,sexo,categoria,raza,nombre,creado_por,creado_en)
                VALUES (?,?,'HEMBRA','VACA','Raza sintética','Nombre privado',?,now())
                """,madre,g,usuario);
        jdbc.update("""
                INSERT INTO reproduccion.parentesco(ganaderia_id,animal_id,tipo,progenitor_animal_id,origen,actualizado_por,actualizado_en)
                VALUES (?,?,'MADRE',?,'CORRECCION',?,now())
                """,g,animal,madre,usuario);
        enviar(post(ruta().replace(g.toString(),UUID.randomUUID().toString())),datos(id,0L,false))
                .andExpect(status().isForbidden());
        enviar(post(ruta()),datos(id,0L,false)).andExpect(status().isCreated());
        mvc.perform(get(PUBLICO)).andExpect(jsonPath("$.items.length()").value(0));
        estado(id,1,"PUBLICAR").andExpect(status().isOk());
        mvc.perform(get(PUBLICO).param("categoria","VACA").param("sexo","HEMBRA").param("raza","cruce")
                .param("ubicacion","lima").param("minimo","110").param("maximo","130"))
                .andExpectAll(status().isOk(),jsonPath("$.items.length()").value(1),
                        jsonPath("$.items[0].contacto").doesNotExist(),jsonPath("$.items[0].genealogia.length()").value(0),
                        jsonPath("$.items[0].animalId").doesNotExist(),jsonPath("$.items[0].ganaderiaId").doesNotExist());
        enviar(put(ruta()+"/"+id),datos(id,2L,true)).andExpect(status().isOk());
        var respuesta=mvc.perform(get(PUBLICO)).andExpectAll(status().isOk(),
                jsonPath("$.items[0].contacto").value("contacto@ejemplo.invalid"),
                jsonPath("$.items[0].genealogia[0].raza").value("Raza sintética")).andReturn();
        assertThat(respuesta.getResponse().getContentAsString()).doesNotContain("Nombre privado","NO PUBLICAR","PRIVADO");
        mvc.perform(get(PUBLICO).param("minimo","121")).andExpect(jsonPath("$.items.length()").value(0));
        estado(id,2,"PAUSAR").andExpect(status().isConflict());
        estado(id,3,"PAUSAR").andExpect(status().isOk());
        mvc.perform(get(PUBLICO)).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test void fotoSeReencodificaYQuedaInaccesibleAlRetirar() throws Exception {
        UUID id=UUID.randomUUID();
        enviar(post(ruta()),datos(id,0L,false)).andExpect(status().isCreated());
        foto(id,new byte[]{10,20,30}).andExpect(status().isUnprocessableEntity());
        var salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",salida);
        var carga=foto(id,salida.toByteArray()).andExpect(status().isCreated()).andReturn();
        String imagen=json.readTree(carga.getResponse().getContentAsByteArray()).required("fotos").get(0).asText();
        String publica=PUBLICO+"/"+id+"/fotos/"+imagen;
        mvc.perform(get(publica)).andExpect(status().isNotFound());
        estado(id,2,"PUBLICAR").andExpect(status().isOk());
        var respuesta=mvc.perform(get(publica)).andExpectAll(status().isOk(),content().contentType("image/jpeg"),
                header().string("Cache-Control","no-store")).andReturn();
        assertThat(respuesta.getResponse().getContentAsByteArray()).startsWith((byte)0xff,(byte)0xd8);
        estado(id,3,"RETIRAR").andExpect(status().isOk());
        mvc.perform(get(publica)).andExpect(status().isNotFound());
    }

    String ruta() { return "/api/v1/ganaderias/"+g+"/animales/"+animal+"/anuncios"; }
    ResultActions enviar(MockHttpServletRequestBuilder req,Object cuerpo) throws Exception {
        return mvc.perform(autorizar(req).contentType("application/json").content(json.writeValueAsBytes(cuerpo)));
    }
    MockHttpServletRequestBuilder autorizar(MockHttpServletRequestBuilder req) {
        return req.header("Authorization",jwt).header("Origin","https://127.0.0.1:5173").with(csrf().asHeader());
    }
    ResultActions estado(UUID id,long version,String accion) throws Exception {
        return enviar(post(ruta()+"/"+id+"/estado"),Map.of("version",version,"accion",accion));
    }
    ResultActions foto(UUID id,byte[] bytes) throws Exception {
        return mvc.perform(autorizar(post(ruta()+"/"+id+"/fotos")).param("version","1").contentType("image/png").content(bytes));
    }
    Map<String,Object> datos(UUID id,long version,boolean autorizar) {
        var datos=new java.util.LinkedHashMap<String,Object>(Map.of("id",id,"version",version,"titulo","Anuncio sintético",
                "caracteristicas","Información autorizada","ubicacionGeneral","Lima","precioPen",120,
                "autorizaContacto",autorizar,"genealogiaAutorizada",autorizar));
        if (autorizar) datos.put("contacto","contacto@ejemplo.invalid");
        return datos;
    }
}
