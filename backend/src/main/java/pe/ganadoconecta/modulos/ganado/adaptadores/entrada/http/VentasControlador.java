package pe.ganadoconecta.modulos.ganado.adaptadores.entrada.http;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarVenta;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/ganaderias/{g}/animales/{animal}/venta")
public class VentasControlador {
    private final RegistrarVenta registrar;
    public VentasControlador(RegistrarVenta registrar) { this.registrar=registrar; }
    @PostMapping ResponseEntity<RegistrarVenta.Recibo> registrar(@PathVariable UUID g,@PathVariable UUID animal,
            @RequestBody Solicitud cuerpo,@AuthenticationPrincipal IdentidadAutenticada identidad) {
        var recibo=registrar.ejecutar(cuerpo.operationId(),g,animal,identidad.usuarioId(),identidad.sesionId());
        return ResponseEntity.status(recibo.reprocesada()?200:201).cacheControl(CacheControl.noStore()).body(recibo);
    }
    @ExceptionHandler(RegistrarVenta.Conflicto.class) ResponseEntity<Void> conflicto() { return error(409); }
    @ExceptionHandler(AnimalNoEncontradoException.class) ResponseEntity<Void> ausente() { return error(404); }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Void> invalido() { return error(422); }
    @ExceptionHandler(AccesoGanaderiaDenegadoException.class) ResponseEntity<Void> denegado() { return error(403); }
    private static ResponseEntity<Void> error(int codigo) {
        return ResponseEntity.status(codigo).cacheControl(CacheControl.noStore()).build();
    }
    record Solicitud(UUID operationId) {}
}
