package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoFamiliar;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.dominio.DatoRegistroInvalidoException;

@RestController
@RequestMapping("/api/v1")
@Profile("postgres")
public class AccesoFamiliarControlador {
    private final AccesoFamiliar acceso;

    public AccesoFamiliarControlador(AccesoFamiliar acceso) { this.acceso = acceso; }

    @GetMapping("/ganaderias/{ganaderia}/familia")
    ResponseEntity<AccesoFamiliar.Resumen> consultar(@PathVariable UUID ganaderia,
            @AuthenticationPrincipal IdentidadAutenticada usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(acceso.consultar(usuario.usuarioId(), usuario.sesionId(), ganaderia));
    }

    @PostMapping("/ganaderias/{ganaderia}/familia/invitaciones")
    ResponseEntity<AccesoFamiliar.Emitida> emitir(@PathVariable UUID ganaderia,
            @AuthenticationPrincipal IdentidadAutenticada usuario, @RequestBody Emision cuerpo) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(acceso.emitir(usuario.usuarioId(), usuario.sesionId(), ganaderia, cuerpo.correo(), cuerpo.rol()));
    }

    @DeleteMapping("/ganaderias/{ganaderia}/familia/invitaciones/{id}")
    ResponseEntity<Void> cancelar(@PathVariable UUID ganaderia, @PathVariable UUID id,
            @AuthenticationPrincipal IdentidadAutenticada usuario) {
        acceso.cancelar(usuario.usuarioId(), usuario.sesionId(), ganaderia, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ganaderias/{ganaderia}/familia/integrantes/{id}")
    ResponseEntity<Void> cambiarRol(@PathVariable UUID ganaderia, @PathVariable UUID id,
            @AuthenticationPrincipal IdentidadAutenticada usuario, @RequestBody Cambio cuerpo) {
        acceso.cambiarRol(usuario.usuarioId(), usuario.sesionId(), ganaderia, id, cuerpo.rol());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitaciones/aceptar")
    ResponseEntity<Map<String,UUID>> aceptar(@AuthenticationPrincipal IdentidadAutenticada usuario,
            @RequestBody Aceptacion cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("ganaderiaId", acceso.aceptar(usuario.usuarioId(), usuario.sesionId(), cuerpo.codigo())));
    }

    @ExceptionHandler(AccesoFamiliar.NoDisponible.class)
    ResponseEntity<Map<String,String>> noDisponible() {
        return ResponseEntity.status(409).cacheControl(CacheControl.noStore())
                .body(Map.of("codigo", "ACCESO_FAMILIAR_NO_DISPONIBLE"));
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<Void> denegado() {
        return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).build();
    }

    @ExceptionHandler({DatoRegistroInvalidoException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String,String>> invalido() {
        return ResponseEntity.unprocessableEntity().cacheControl(CacheControl.noStore())
                .body(Map.of("codigo", "DATOS_INVALIDOS"));
    }

    record Emision(String correo, AccesoFamiliar.Rol rol) {}
    record Cambio(AccesoFamiliar.Rol rol) {}
    record Aceptacion(String codigo) {
        @Override public String toString() { return "Aceptacion[PROTEGIDA]"; }
    }
}
