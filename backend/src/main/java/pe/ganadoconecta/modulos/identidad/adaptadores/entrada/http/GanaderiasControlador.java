package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.identidad.aplicacion.CrearGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.ConsultarAccesoGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.ListarGanaderias;
import pe.ganadoconecta.modulos.identidad.aplicacion.RevocarMembresia;

/** Adaptador HTTP de AU05–AU07. */
@RestController
@RequestMapping("/api/v1/ganaderias")
@Profile("postgres")
public class GanaderiasControlador {
    private final CrearGanaderia crear;
    private final ListarGanaderias listar;
    private final ConsultarAccesoGanaderia consultarAcceso;
    private final RevocarMembresia revocarMembresia;

    public GanaderiasControlador(CrearGanaderia crear, ListarGanaderias listar,
            ConsultarAccesoGanaderia consultarAcceso,
            RevocarMembresia revocarMembresia) {
        this.crear = crear;
        this.listar = listar;
        this.consultarAcceso = consultarAcceso;
        this.revocarMembresia = revocarMembresia;
    }

    @DeleteMapping("/{ganaderiaId}/membresias/{usuarioId}")
    ResponseEntity<Void> revocarMembresia(
            @PathVariable UUID ganaderiaId,
            @PathVariable UUID usuarioId,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        revocarMembresia.ejecutar(
                identidad.usuarioId(), identidad.sesionId(), ganaderiaId, usuarioId);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @GetMapping("/{ganaderiaId}/acceso")
    ResponseEntity<AccesoRespuesta> consultarAcceso(
            @PathVariable UUID ganaderiaId,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        var resultado = consultarAcceso.ejecutar(identidad.usuarioId(), ganaderiaId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AccesoRespuesta(resultado.ganaderiaId(), resultado.rol(),
                        resultado.estado(), resultado.validadoEn()));
    }

    @GetMapping
    ResponseEntity<List<GanaderiaRespuesta>> listar(
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        List<GanaderiaRespuesta> respuesta = listar.ejecutar(identidad.usuarioId()).stream()
                .map(ganaderia -> new GanaderiaRespuesta(
                        ganaderia.id(), ganaderia.nombre(),
                        ganaderia.estado(), ganaderia.rol()))
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
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

    record AccesoRespuesta(UUID ganaderiaId, String rol, String estado,
            Instant validadoEn) {
    }
}
