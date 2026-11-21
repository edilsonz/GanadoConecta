package pe.ganadoconecta.modulos.reportes.adaptadores.entrada.http;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Periodo;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Reporte;

@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/reportes")
@Profile("postgres")
class ReportesControlador {
    private final ConsultarReportesGanaderos consulta;

    ReportesControlador(ConsultarReportesGanaderos consulta) { this.consulta = consulta; }

    @GetMapping
    ResponseEntity<Reporte> reporte(@PathVariable UUID ganaderiaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID animalId,
            @RequestParam MultiValueMap<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!Set.of("desde", "hasta", "animalId").containsAll(parametros.keySet())
                || parametros.values().stream().anyMatch(valores -> valores.size() != 1)) {
            throw new IllegalArgumentException();
        }
        return respuesta(200, consulta.ejecutar(identidad.usuarioId(), ganaderiaId, animalId, new Periodo(desde, hasta)));
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<Void> denegado() { return respuesta(403, null); }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Void> invalido() { return respuesta(400, null); }

    @ExceptionHandler({DataAccessException.class, PersistenciaIdentidadException.class})
    ResponseEntity<Void> servicio() { return respuesta(503, null); }

    private static <T> ResponseEntity<T> respuesta(int estado, T datos) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(datos);
    }
}
