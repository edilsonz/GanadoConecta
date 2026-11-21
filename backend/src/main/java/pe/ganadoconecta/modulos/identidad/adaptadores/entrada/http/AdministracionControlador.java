package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.ganadoconecta.modulos.identidad.aplicacion.ConsultarAdministracion;
import pe.ganadoconecta.modulos.identidad.aplicacion.ConsultarAdministracion.*;

@RestController
@RequestMapping("/api/v1/administracion")
@Profile("postgres")
class AdministracionControlador {
    private final ConsultarAdministracion consulta;

    AdministracionControlador(ConsultarAdministracion consulta) { this.consulta = consulta; }

    @GetMapping("/acceso")
    ResponseEntity<Map<String, Boolean>> acceso(@AuthenticationPrincipal IdentidadAutenticada identidad) {
        return respuesta(200, Map.of("permitido", consulta.acceso(identidad.usuarioId(), identidad.sesionId())));
    }

    @GetMapping("/usuarios")
    ResponseEntity<Pagina> usuarios(@AuthenticationPrincipal IdentidadAutenticada identidad,
            @RequestParam(required = false) String buscar, @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) UUID cursor, @RequestParam MultiValueMap<String, String> parametros) {
        if (!Set.of("buscar", "estado", "cursor").containsAll(parametros.keySet())
                || parametros.values().stream().anyMatch(v -> v.size() != 1)) throw new IllegalArgumentException();
        return respuesta(200, consulta.usuarios(identidad.usuarioId(), identidad.sesionId(), Filtro.de(buscar, estado, cursor)));
    }

    @GetMapping("/estadisticas")
    ResponseEntity<Estadisticas> estadisticas(@AuthenticationPrincipal IdentidadAutenticada identidad) {
        return respuesta(200, consulta.estadisticas(identidad.usuarioId(), identidad.sesionId()));
    }

    @ExceptionHandler(AccesoDenegado.class)
    ResponseEntity<Void> denegado() { return respuesta(403, null); }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Void> invalido() { return respuesta(400, null); }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Void> servicio() { return respuesta(503, null); }

    private static <T> ResponseEntity<T> respuesta(int estado, T datos) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(datos);
    }
}
