package pe.ganadoconecta.modulos.sanidad.adaptadores.entrada.http;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarVacunaciones;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RegistrarVacunacion;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ResultadoVacunacion;
import pe.ganadoconecta.modulos.sanidad.aplicacion.SolicitudVacunacion;
import pe.ganadoconecta.modulos.sanidad.dominio.Vacunacion;

@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}/vacunaciones")
@Profile("postgres")
public class VacunacionesControlador {
    private final RegistrarVacunacion registrar;
    private final ListarVacunaciones listar;

    public VacunacionesControlador(RegistrarVacunacion registrar,
            ListarVacunaciones listar) {
        this.registrar = registrar;
        this.listar = listar;
    }

    @PostMapping
    ResponseEntity<ResultadoRespuesta> registrar(@PathVariable UUID ganaderiaId,
            @PathVariable UUID animalId, @RequestBody RegistroSolicitud body,
            @RequestParam Map<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!parametros.isEmpty()) throw new SolicitudSanitariaInvalidaException();
        ResultadoVacunacion resultado = registrar.ejecutar(body.aDominio(ganaderiaId, animalId),
                identidad.usuarioId(), identidad.sesionId());
        return ResponseEntity.status(resultado.estado() == ResultadoVacunacion.Estado.APLICADO
                        ? HttpStatus.CREATED : HttpStatus.OK)
                .cacheControl(CacheControl.noStore())
                .body(new ResultadoRespuesta(resultado.operationId(), resultado.eventId(),
                        resultado.estado().name(), resultado.confirmadoEn()));
    }

    @GetMapping
    ResponseEntity<List<VacunacionRespuesta>> listar(@PathVariable UUID ganaderiaId,
            @PathVariable UUID animalId, @RequestParam Map<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!parametros.isEmpty()) throw new SolicitudSanitariaInvalidaException();
        List<VacunacionRespuesta> items = listar.ejecutar(identidad.usuarioId(),
                ganaderiaId, animalId).stream().map(VacunacionRespuesta::desde).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(items);
    }

    record RegistroSolicitud(UUID operationId, UUID eventId, Integer schemaVersion,
            String nombreVacuna, LocalDate fechaAplicacion, String responsable,
            String lote, String observaciones, LocalDate proximaFecha) {
        SolicitudVacunacion aDominio(UUID ganaderiaId, UUID animalId) {
            return new SolicitudVacunacion(ganaderiaId, animalId, eventId, operationId,
                    schemaVersion == null ? 0 : schemaVersion, nombreVacuna,
                    fechaAplicacion, responsable, lote, observaciones, proximaFecha);
        }
        @Override public String toString() { return "RegistroSolicitud[datos=PROTEGIDOS]"; }
    }

    record ResultadoRespuesta(UUID operationId, UUID eventId, String resultado,
            Instant confirmadoEn) { }

    record VacunacionRespuesta(UUID eventId, String nombreVacuna,
            LocalDate fechaAplicacion, String responsable, String lote,
            String observaciones, LocalDate proximaFecha, Instant creadoEn) {
        static VacunacionRespuesta desde(Vacunacion v) {
            return new VacunacionRespuesta(v.id(), v.nombreVacuna(), v.fechaAplicacion(),
                    v.responsable(), v.lote(), v.observaciones(), v.proximaFecha(),
                    v.creadoEn());
        }
    }
}

final class SolicitudSanitariaInvalidaException extends RuntimeException { }
