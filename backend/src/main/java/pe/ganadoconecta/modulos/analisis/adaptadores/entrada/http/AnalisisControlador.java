package pe.ganadoconecta.modulos.analisis.adaptadores.entrada.http;

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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAnalisisGanadero;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAnalisisGanadero.Alertas;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAnalisisGanadero.Analisis;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Periodo;
import pe.ganadoconecta.modulos.sanidad.aplicacion.PersistenciaSanitariaException;

@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}")
@Profile("postgres")
class AnalisisControlador {
    private final ConsultarAnalisisGanadero consulta;

    AnalisisControlador(ConsultarAnalisisGanadero consulta) { this.consulta = consulta; }

    @GetMapping("/analisis")
    ResponseEntity<Analisis> analizar(@PathVariable UUID ganaderiaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID animalId, @RequestParam MultiValueMap<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!Set.of("desde", "hasta", "animalId").containsAll(parametros.keySet())
                || parametros.values().stream().anyMatch(valores -> valores.size() != 1 || valores.getFirst().isBlank())) {
            throw new IllegalArgumentException();
        }
        return respuesta(200, consulta.analizar(identidad.usuarioId(), ganaderiaId, animalId, new Periodo(desde, hasta)));
    }

    @GetMapping("/alertas")
    ResponseEntity<Alertas> alertas(@PathVariable UUID ganaderiaId, @RequestParam MultiValueMap<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!parametros.isEmpty()) throw new IllegalArgumentException();
        return respuesta(200, consulta.alertas(identidad.usuarioId(), ganaderiaId));
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<Void> denegado() { return respuesta(403, null); }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Void> invalido() { return respuesta(400, null); }

    @ExceptionHandler({DataAccessException.class, PersistenciaIdentidadException.class, PersistenciaSanitariaException.class})
    ResponseEntity<Void> servicio() { return respuesta(503, null); }

    private static <T> ResponseEntity<T> respuesta(int estado, T datos) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(datos);
    }
}
