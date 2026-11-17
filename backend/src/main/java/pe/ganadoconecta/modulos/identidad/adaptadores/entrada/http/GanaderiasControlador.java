package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.identidad.aplicacion.CrearGanaderia;

/** Adaptador HTTP del primer incremento AU05. */
@RestController
@RequestMapping("/api/v1/ganaderias")
@Profile("postgres")
public class GanaderiasControlador {
    private final CrearGanaderia crear;

    public GanaderiasControlador(CrearGanaderia crear) {
        this.crear = crear;
    }

    @PostMapping
    ResponseEntity<GanaderiaRespuesta> crear(@RequestBody GanaderiaSolicitud solicitud,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        CrearGanaderia.Resultado resultado = crear.ejecutar(new CrearGanaderia.Comando(
                solicitud.id(), solicitud.nombre(), identidad.usuarioId()));
        GanaderiaRespuesta respuesta = new GanaderiaRespuesta(
                resultado.id(), resultado.nombre(), resultado.estado(), resultado.rol());
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    record GanaderiaSolicitud(String id, String nombre) {
        @Override
        public String toString() {
            return "GanaderiaSolicitud[datos=PROTEGIDOS]";
        }
    }

    record GanaderiaRespuesta(UUID id, String nombre, String estado, String rol) {
    }
}
