package pe.ganadoconecta.modulos.analisis.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.*;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.PersistenciaAnimalException;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.SesionNoValidaException;
import pe.ganadoconecta.modulos.reproduccion.aplicacion.PersistenciaReproductivaException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.PersistenciaSanitariaException;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/asistente")
class AsistenteControlador {
    private static final int MAX_SOLICITUD = 4096;
    private final ConsultarAsistente asistente;
    private final ObjectMapper json;

    AsistenteControlador(ConsultarAsistente asistente, ObjectMapper json) {
        this.asistente = asistente;
        this.json = json;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Respuesta> consultar(@PathVariable UUID ganaderiaId, HttpServletRequest peticion,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (peticion.getQueryString() != null) throw new IllegalArgumentException();
        Solicitud solicitud;
        try {
            byte[] cuerpo = peticion.getInputStream().readNBytes(MAX_SOLICITUD + 1);
            if (cuerpo.length > MAX_SOLICITUD) throw new IllegalArgumentException();
            solicitud = json.readerFor(Solicitud.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS).readValue(cuerpo);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
        return respuesta(200, asistente.ejecutar(identidad.usuarioId(), identidad.sesionId(), ganaderiaId, solicitud));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Void> invalida() { return respuesta(400, null); }

    @ExceptionHandler(SesionNoValidaException.class)
    ResponseEntity<Void> sesion() { return respuesta(401, null); }

    @ExceptionHandler({AccesoGanaderiaDenegadoException.class, AnimalNoEncontradoException.class})
    ResponseEntity<Void> denegada() { return respuesta(403, null); }

    @ExceptionHandler(Ocupado.class)
    ResponseEntity<Void> ocupado() { return respuesta(429, null); }

    @ExceptionHandler({Indisponible.class, DataAccessException.class, PersistenciaIdentidadException.class,
            PersistenciaAnimalException.class, PersistenciaReproductivaException.class, PersistenciaSanitariaException.class})
    ResponseEntity<Void> indisponible() { return respuesta(503, null); }

    private static <T> ResponseEntity<T> respuesta(int estado, T datos) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(datos);
    }
}
