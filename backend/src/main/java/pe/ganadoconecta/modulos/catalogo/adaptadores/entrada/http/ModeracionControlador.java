package pe.ganadoconecta.modulos.catalogo.adaptadores.entrada.http;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Fallo;
import pe.ganadoconecta.modulos.catalogo.aplicacion.ModeracionCatalogo;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;

@RestController
@RequestMapping("/api/v1")
@Profile("postgres")
public class ModeracionControlador {
    private final ModeracionCatalogo moderacion;
    public ModeracionControlador(ModeracionCatalogo moderacion) { this.moderacion=moderacion; }

    @GetMapping("/catalogo/reportes-disponibles")
    ResponseEntity<Map<String,Boolean>> disponible() {
        return respuesta(200,Map.of("recepcionExperimental",moderacion.recepcionExperimental()));
    }

    @PostMapping("/catalogo/{id}/reportes")
    ResponseEntity<Map<String,UUID>> reportar(@PathVariable UUID id,@RequestBody Motivo motivo,
            @AuthenticationPrincipal IdentidadAutenticada i) {
        return respuesta(201,Map.of("reporteId",moderacion.reportar(id,i.usuarioId(),i.sesionId(),motivo.motivo())));
    }

    @GetMapping("/moderacion/reportes")
    ResponseEntity<List<ModeracionCatalogo.Reporte>> bandeja(@AuthenticationPrincipal IdentidadAutenticada i) {
        return respuesta(200,moderacion.bandeja(i.usuarioId(),i.sesionId()));
    }

    @PostMapping("/moderacion/reportes/{id}/revision")
    ResponseEntity<Void> revisar(@PathVariable UUID id,@RequestBody Revision revision,
            @AuthenticationPrincipal IdentidadAutenticada i) {
        moderacion.revisar(id,i.usuarioId(),i.sesionId(),revision.decision());
        return respuesta(204,null);
    }

    @ExceptionHandler(ModeracionCatalogo.AccesoDenegado.class)
    ResponseEntity<Void> denegado() { return respuesta(403,null); }
    @ExceptionHandler(Fallo.class)
    ResponseEntity<Void> fallo(Fallo f) { return respuesta(switch(f.tipo) {
        case INVALIDO -> 422; case AUSENTE -> 404; case CONFLICTO -> 409; case ALMACEN_NO_DISPONIBLE -> 503;
    },null); }
    private static <T> ResponseEntity<T> respuesta(int codigo,T datos) {
        return ResponseEntity.status(codigo).cacheControl(CacheControl.noStore()).body(datos);
    }
    record Motivo(String motivo) { @Override public String toString() { return "Motivo[PROTEGIDO]"; } }
    record Revision(ModeracionCatalogo.Decision decision) {}
}
