package pe.ganadoconecta.modulos.ganado.adaptadores.entrada.http;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.ControlPeso;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/ganaderias/{ganaderia}/animales/{animal}/pesos")
public class PesosControlador {
    private final ControlPeso pesos;
    public PesosControlador(ControlPeso pesos) { this.pesos = pesos; }

    @GetMapping ResponseEntity<Historial> listar(@PathVariable UUID ganaderia,
            @PathVariable UUID animal, @AuthenticationPrincipal IdentidadAutenticada cuenta) {
        var historial = pesos.listar(ganaderia, animal, cuenta.usuarioId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Historial(historial, historial.isEmpty() ? null : historial.getLast()));
    }

    @PostMapping ResponseEntity<Map<String, Object>> registrar(@PathVariable UUID ganaderia,
            @PathVariable UUID animal, @AuthenticationPrincipal IdentidadAutenticada cuenta,
            @RequestBody ControlPeso.Medicion medicion) {
        var resultado = pesos.registrar(ganaderia, animal, cuenta.usuarioId(), cuenta.sesionId(), medicion);
        int estado = switch (resultado) {
            case APLICADO -> 201;
            case REPROCESADO -> 200;
            case CONFLICTO -> 409;
        };
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore())
                .body(Map.of("id", medicion.id(), "resultado", resultado.name()));
    }

    @ExceptionHandler(ControlPeso.DatoInvalido.class)
    ResponseEntity<Void> invalido() { return ResponseEntity.unprocessableEntity().build(); }
    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<Void> denegado() { return ResponseEntity.status(403).build(); }
    @ExceptionHandler(AnimalNoEncontradoException.class)
    ResponseEntity<Void> ausente() { return ResponseEntity.notFound().build(); }

    record Historial(List<ControlPeso.Medicion> mediciones, ControlPeso.Medicion ultimo) {}
}
