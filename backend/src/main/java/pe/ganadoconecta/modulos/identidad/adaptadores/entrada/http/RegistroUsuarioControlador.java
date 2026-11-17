package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.identidad.aplicacion.RegistrarUsuario;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/auth")
public class RegistroUsuarioControlador {
    private final RegistrarUsuario registrarUsuario;

    public RegistroUsuarioControlador(RegistrarUsuario registrarUsuario) {
        this.registrarUsuario = registrarUsuario;
    }

    @PostMapping("/registro")
    ResponseEntity<RegistroUsuarioRespuesta> registrar(
            @RequestBody RegistroUsuarioSolicitud solicitud) {
        RegistrarUsuario.Resultado resultado = registrarUsuario.ejecutar(
                new RegistrarUsuario.Comando(solicitud.correo(), solicitud.contrasena()));
        RegistroUsuarioRespuesta respuesta = new RegistroUsuarioRespuesta(
                resultado.id(), resultado.correo(), resultado.estado());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    record RegistroUsuarioSolicitud(String correo, String contrasena) {
        @Override
        public String toString() {
            return "RegistroUsuarioSolicitud[datos=PROTEGIDOS]";
        }
    }

    record RegistroUsuarioRespuesta(UUID id, String correo, String estado) {
        @Override
        public String toString() {
            return "RegistroUsuarioRespuesta[id=" + id + ", datos=PROTEGIDOS]";
        }
    }
}
