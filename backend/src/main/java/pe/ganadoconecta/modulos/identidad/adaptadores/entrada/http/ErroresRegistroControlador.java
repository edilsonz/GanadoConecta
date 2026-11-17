package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.ganadoconecta.modulos.identidad.aplicacion.CorreoNoDisponibleException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaUsuarioException;
import pe.ganadoconecta.modulos.identidad.dominio.DatoRegistroInvalidoException;

@RestControllerAdvice(assignableTypes = RegistroUsuarioControlador.class)
@Profile("postgres")
public class ErroresRegistroControlador {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErroresRegistroControlador.class);

    @ExceptionHandler(DatoRegistroInvalidoException.class)
    ResponseEntity<RespuestaError> datoInvalido(DatoRegistroInvalidoException exception) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INVALIDOS",
                "Revise los datos enviados.", Map.of(exception.campo(), exception.getMessage()));
    }

    @ExceptionHandler(CorreoNoDisponibleException.class)
    ResponseEntity<RespuestaError> correoNoDisponible() {
        return respuesta(HttpStatus.CONFLICT, "REGISTRO_NO_DISPONIBLE",
                "No fue posible completar el registro.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaError> cuerpoInvalido(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof UnrecognizedPropertyException unknown) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "CAMPO_NO_ADMITIDO",
                    "La solicitud contiene campos no admitidos.",
                    Map.of(unknown.getPropertyName(), "Este campo no está permitido."));
        }
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un formato JSON válido.", Map.of());
    }

    @ExceptionHandler(PersistenciaUsuarioException.class)
    ResponseEntity<RespuestaError> persistenciaNoDisponible() {
        String correlacionId = UUID.randomUUID().toString();
        LOGGER.error("Fallo de persistencia al registrar usuario; correlacionId={}", correlacionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new RespuestaError("REGISTRO_NO_DISPONIBLE",
                        "No fue posible completar el registro.", Map.of(), correlacionId));
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatus status, String codigo,
            String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(status)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new RespuestaError(
                        codigo, mensaje, errores, UUID.randomUUID().toString()));
    }
}
