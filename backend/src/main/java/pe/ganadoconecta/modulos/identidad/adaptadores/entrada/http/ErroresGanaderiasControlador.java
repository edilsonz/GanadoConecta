package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.GanaderiaNoDisponibleException;
import pe.ganadoconecta.modulos.identidad.aplicacion.MembresiaNoEncontradaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaGanaderiaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PropietarioNoRevocableException;
import pe.ganadoconecta.modulos.identidad.dominio.DatoGanaderiaInvalidoException;

@RestControllerAdvice(assignableTypes = GanaderiasControlador.class)
@Profile("postgres")
public class ErroresGanaderiasControlador {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ErroresGanaderiasControlador.class);

    @ExceptionHandler(DatoGanaderiaInvalidoException.class)
    ResponseEntity<RespuestaError> datoInvalido(DatoGanaderiaInvalidoException exception) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INVALIDOS",
                "Revise los datos enviados.", Map.of(exception.campo(), exception.getMessage()));
    }

    @ExceptionHandler(GanaderiaNoDisponibleException.class)
    ResponseEntity<RespuestaError> conflicto() {
        return respuesta(HttpStatus.CONFLICT, "GANADERIA_NO_DISPONIBLE",
                "No fue posible crear la ganadería.", Map.of());
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<RespuestaError> accesoDenegado() {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_GANADERIA_DENEGADO",
                "No tiene acceso a la ganadería solicitada.", Map.of());
    }

    @ExceptionHandler(MembresiaNoEncontradaException.class)
    ResponseEntity<RespuestaError> membresiaNoEncontrada() {
        return respuesta(HttpStatus.NOT_FOUND, "MEMBRESIA_NO_ENCONTRADA",
                "No se encontró la membresía solicitada.", Map.of());
    }

    @ExceptionHandler(PropietarioNoRevocableException.class)
    ResponseEntity<RespuestaError> propietarioNoRevocable() {
        return respuesta(HttpStatus.CONFLICT, "PROPIETARIO_NO_REVOCABLE",
                "El propietario no puede revocarse mediante esta operación.", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<RespuestaError> identificadorInvalido() {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un identificador válido.", Map.of());
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

    @ExceptionHandler(PersistenciaGanaderiaException.class)
    ResponseEntity<RespuestaError> persistenciaNoDisponible() {
        String correlacionId = UUID.randomUUID().toString();
        LOGGER.error("Fallo de persistencia de ganadería; correlacionId={}",
                correlacionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaError("GANADERIA_NO_DISPONIBLE",
                        "No fue posible procesar la solicitud de ganadería.",
                        Map.of(), correlacionId));
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatus status, String codigo,
            String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaError(
                        codigo, mensaje, errores, UUID.randomUUID().toString()));
    }
}
