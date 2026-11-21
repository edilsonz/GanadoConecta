package pe.ganadoconecta.modulos.sanidad.adaptadores.entrada.http;

import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.sanidad.aplicacion.*;
import pe.ganadoconecta.modulos.sanidad.dominio.*;

@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/agenda-sanitaria")
@Profile("postgres")
public class AgendaSanitariaControlador {
    private final ProgramarActividadSanitaria programar;
    private final ConsultarAgendaSanitaria consultar;
    private final CompletarActividadSanitaria completar;
    private final Clock reloj;

    public AgendaSanitariaControlador(ProgramarActividadSanitaria programar,
            ConsultarAgendaSanitaria consultar, CompletarActividadSanitaria completar, Clock reloj) {
        this.programar = programar;
        this.consultar = consultar;
        this.completar = completar;
        this.reloj = reloj;
    }

    @PostMapping ResponseEntity<Respuesta> crear(@PathVariable UUID ganaderiaId, @RequestBody Solicitud b,
            @RequestParam MultiValueMap<String,String> parametros, @AuthenticationPrincipal IdentidadAutenticada i) {
        sinParametros(parametros);
        var actividad = programar.ejecutar(i.usuarioId(), i.sesionId(), ganaderiaId, b.animalId(),
                b.actividadId(), b.eventoOrigenId(), convertir(TipoActividadSanitaria.class, b.tipo(), "tipo"),
                b.fechaPrevista(), b.descripcion(), b.observaciones(), b.participantes());
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(respuesta(actividad));
    }

    @GetMapping ResponseEntity<List<Respuesta>> listar(@PathVariable UUID ganaderiaId,
            @RequestParam(required=false) UUID animalId, @RequestParam(required=false) String tipo,
            @RequestParam(required=false) String estado, @RequestParam MultiValueMap<String,String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada i) {
        if (!Set.of("animalId","tipo","estado").containsAll(parametros.keySet())
                || parametros.values().stream().anyMatch(v -> v.size()!=1)) throw new SolicitudSanitariaInvalidaException();
        var actividades = consultar.ejecutar(i.usuarioId(), ganaderiaId, animalId,
                tipo == null ? null : convertir(TipoActividadSanitaria.class, tipo, "tipo"),
                estado == null ? null : convertir(EstadoActividadSanitaria.class, estado, "estado"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(actividades.stream().map(this::respuesta).toList());
    }

    @PostMapping("/{actividadId}/completar") ResponseEntity<Respuesta> completar(@PathVariable UUID ganaderiaId,
            @PathVariable UUID actividadId, @RequestBody(required=false) ConfirmacionActividad confirmacion,
            @RequestParam MultiValueMap<String,String> parametros, @AuthenticationPrincipal IdentidadAutenticada i) {
        sinParametros(parametros);
        var actividad = completar.ejecutar(i.usuarioId(), i.sesionId(), ganaderiaId, actividadId, confirmacion);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta(actividad));
    }

    private Respuesta respuesta(ActividadSanitaria a) {
        LocalDate hoy = LocalDate.now(reloj.withZone(ZoneId.of("America/Lima")));
        return new Respuesta(a.id(), a.animalId(), a.eventoOrigenId(), a.tipo().name(),
                a.fechaPrevista(), a.descripcion(), a.observaciones(), a.estado().name(),
                a.estadoVisible(hoy), a.version(), a.creadoEn(), a.completadoEn(), a.participantes(),
                a.atendidos(), a.fechaAtencion(), a.nombreVacuna());
    }

    private static void sinParametros(MultiValueMap<String,String> q) {
        if (!q.isEmpty()) throw new SolicitudSanitariaInvalidaException();
    }

    private static <T extends Enum<T>> T convertir(Class<T> tipo, String valor, String campo) {
        try { return Enum.valueOf(tipo, valor); }
        catch (IllegalArgumentException | NullPointerException e) { throw new DatoSanitarioInvalidoException(campo); }
    }

    record Solicitud(UUID actividadId, UUID animalId, UUID eventoOrigenId, String tipo,
            LocalDate fechaPrevista, String descripcion, String observaciones, List<UUID> participantes) {
        @Override public String toString() { return "Solicitud[datos=PROTEGIDOS]"; }
    }

    record Respuesta(UUID actividadId, UUID animalId, UUID eventoOrigenId, String tipo, LocalDate fechaPrevista,
            String descripcion, String observaciones, String estado, String estadoVisible, long version,
            Instant creadoEn, Instant completadoEn, List<UUID> participantes, List<UUID> atendidos,
            LocalDate fechaAtencion, String nombreVacuna) {}
}
