package pe.ganadoconecta.modulos.ganado.adaptadores.entrada.http;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import org.springframework.transaction.TransactionException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConflictoRegistroAnimalException;
import pe.ganadoconecta.modulos.ganado.aplicacion.ContratoOperacionInvalidoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.PersistenciaAnimalException;
import pe.ganadoconecta.modulos.ganado.dominio.DatoAnimalInvalidoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;

@RestControllerAdvice(assignableTypes = AnimalesControlador.class)
@Profile("postgres")
public class ErroresAnimalesControlador {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ErroresAnimalesControlador.class);

    @ExceptionHandler(DatoAnimalInvalidoException.class)
    ResponseEntity<RespuestaErrorAnimal> datoInvalido(
            DatoAnimalInvalidoException exception) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INVALIDOS",
                "Revise los datos enviados.",
                Map.of(exception.campo(), "El campo no es válido."));
    }

    @ExceptionHandler(ContratoOperacionInvalidoException.class)
    ResponseEntity<RespuestaErrorAnimal> contratoInvalido(
            ContratoOperacionInvalidoException exception) {
        if ("schemaVersion".equals(exception.campo())) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ESQUEMA_NO_SOPORTADO", "La versión del esquema no es compatible.",
                    Map.of("schemaVersion", "La versión no está soportada."));
        }
        if ("baseVersion".equals(exception.campo())) {
            return respuesta(HttpStatus.CONFLICT, "VERSION_INCOMPATIBLE",
                    "La versión base no es compatible.", Map.of());
        }
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INVALIDOS",
                "Revise los datos enviados.",
                Map.of(exception.campo(), "El campo no es válido."));
    }

    @ExceptionHandler(ConflictoRegistroAnimalException.class)
    ResponseEntity<RespuestaErrorAnimal> conflicto() {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO",
                "La operación entra en conflicto con un registro existente.", Map.of());
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<RespuestaErrorAnimal> accesoDenegado(
            AccesoGanaderiaDenegadoException exception) {
        String codigo = switch (exception.motivo()) {
            case MEMBRESIA_REVOCADA -> "MEMBRESIA_REVOCADA";
            case ROL_INSUFICIENTE -> "ROL_INSUFICIENTE";
            case ACCESO_DENEGADO -> "ACCESO_GANADERIA_DENEGADO";
        };
        return respuesta(HttpStatus.FORBIDDEN, codigo,
                "No tiene permiso para registrar animales en esta ganadería.", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<RespuestaErrorAnimal> identificadorRutaInvalido() {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un identificador válido.", Map.of());
    }

    @ExceptionHandler(SolicitudAnimalHttpInvalidaException.class)
    ResponseEntity<RespuestaErrorAnimal> parametrosNoAdmitidos() {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene parámetros no admitidos.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaErrorAnimal> cuerpoInvalido(
            HttpMessageNotReadableException exception) {
        UnrecognizedPropertyException desconocido = buscarCausa(
                exception, UnrecognizedPropertyException.class);
        if (desconocido != null) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "CAMPO_NO_ADMITIDO",
                    "La solicitud contiene campos no admitidos.",
                    Map.of(desconocido.getPropertyName(), "Este campo no está permitido."));
        }
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un formato JSON válido.", Map.of());
    }

    @ExceptionHandler({PersistenciaAnimalException.class,
            PersistenciaIdentidadException.class, TransactionException.class})
    ResponseEntity<RespuestaErrorAnimal> persistenciaNoDisponible() {
        String correlacionId = UUID.randomUUID().toString();
        LOGGER.error("Fallo de persistencia al registrar animal; correlacionId={}",
                correlacionId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaErrorAnimal("REGISTRO_ANIMAL_NO_DISPONIBLE",
                        "No fue posible completar el registro del animal.",
                        Map.of(), correlacionId));
    }

    private ResponseEntity<RespuestaErrorAnimal> respuesta(HttpStatus estado,
            String codigo, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado)
                .cacheControl(CacheControl.noStore())
                .body(new RespuestaErrorAnimal(codigo, mensaje, errores,
                        UUID.randomUUID().toString()));
    }

    private static <T extends Throwable> T buscarCausa(Throwable error, Class<T> tipo) {
        Throwable actual = error;
        while (actual != null) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
            actual = actual.getCause();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record RespuestaErrorAnimal(
            String codigo,
            String mensajeSeguro,
            Map<String, String> erroresPorCampo,
            String correlacionId) {
    }
}
