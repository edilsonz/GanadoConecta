package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.ganadoconecta.modulos.identidad.aplicacion.CredencialesInvalidasException;
import pe.ganadoconecta.modulos.identidad.aplicacion.DemasiadosIntentosAutenticacionException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.SesionNoValidaException;

/** Errores AU02 uniformes, sin datos de cuenta, tokens ni detalles internos. */
@RestControllerAdvice(assignableTypes = AutenticacionControlador.class)
@Profile("postgres")
public class ErroresAutenticacionControlador {
    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<RespuestaError> credencialesInvalidas() {
        return respuesta(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                "Las credenciales no son válidas.");
    }

    @ExceptionHandler(SesionNoValidaException.class)
    ResponseEntity<RespuestaError> sesionNoValida() {
        return respuesta(HttpStatus.UNAUTHORIZED, "SESION_NO_VALIDA",
                "La sesión no es válida.");
    }

    @ExceptionHandler(DemasiadosIntentosAutenticacionException.class)
    ResponseEntity<RespuestaError> demasiadosIntentos() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "900")
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaError("DEMASIADOS_INTENTOS",
                        "Espere antes de volver a intentar.", Map.of(),
                        UUID.randomUUID().toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaError> cuerpoInvalido(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof UnrecognizedPropertyException) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "CAMPO_NO_ADMITIDO",
                    "La solicitud contiene campos no admitidos.");
        }
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un formato JSON válido.");
    }

    @ExceptionHandler(PersistenciaIdentidadException.class)
    ResponseEntity<RespuestaError> persistenciaNoDisponible() {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "AUTENTICACION_NO_DISPONIBLE",
                "No fue posible completar la operación.");
    }

    private ResponseEntity<RespuestaError> respuesta(
            HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado)
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaError(codigo, mensaje, Map.of(),
                        UUID.randomUUID().toString()));
    }
}
