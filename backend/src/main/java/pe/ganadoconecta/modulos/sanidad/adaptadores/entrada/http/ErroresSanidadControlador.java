package pe.ganadoconecta.modulos.sanidad.adaptadores.entrada.http;

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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConflictoSanitarioException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.PersistenciaSanitariaException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ActividadAgendaNoEncontradaException;
import pe.ganadoconecta.modulos.sanidad.dominio.DatoSanitarioInvalidoException;

@RestControllerAdvice(assignableTypes = {VacunacionesControlador.class,
        HistorialSanitarioControlador.class, AgendaSanitariaControlador.class})
@Profile("postgres")
public class ErroresSanidadControlador {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErroresSanidadControlador.class);

    @ExceptionHandler(DatoSanitarioInvalidoException.class)
    ResponseEntity<ErrorSanitario> invalido(DatoSanitarioInvalidoException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "DATOS_INVALIDOS",
                "Revise los datos sanitarios enviados.",
                Map.of(e.campo(), "El campo no es válido."));
    }

    @ExceptionHandler(ConflictoSanitarioException.class)
    ResponseEntity<ErrorSanitario> conflicto() {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO",
                "La operación entra en conflicto con un registro existente.", Map.of());
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<ErrorSanitario> acceso() {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_GANADERIA_DENEGADO",
                "No tiene acceso al seguimiento sanitario solicitado.", Map.of());
    }

    @ExceptionHandler(AnimalNoEncontradoException.class)
    ResponseEntity<ErrorSanitario> animal() {
        return respuesta(HttpStatus.NOT_FOUND, "ANIMAL_NO_ENCONTRADO",
                "No se encontró el animal solicitado.", Map.of());
    }

    @ExceptionHandler(ActividadAgendaNoEncontradaException.class)
    ResponseEntity<ErrorSanitario> actividad() {
        return respuesta(HttpStatus.NOT_FOUND, "ACTIVIDAD_NO_ENCONTRADA",
                "No se encontró la actividad sanitaria solicitada.", Map.of());
    }

    @ExceptionHandler({SolicitudSanitariaInvalidaException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorSanitario> solicitud() {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud sanitaria no es válida.", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorSanitario> cuerpo(HttpMessageNotReadableException e) {
        Throwable causa = e;
        while (causa != null && !(causa instanceof UnrecognizedPropertyException)) {
            causa = causa.getCause();
        }
        if (causa instanceof UnrecognizedPropertyException desconocido) {
            return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "CAMPO_NO_ADMITIDO",
                    "La solicitud contiene campos no admitidos.",
                    Map.of(desconocido.getPropertyName(), "Este campo no está permitido."));
        }
        return solicitud();
    }

    @ExceptionHandler(PersistenciaSanitariaException.class)
    ResponseEntity<ErrorSanitario> persistencia() {
        String correlacion = UUID.randomUUID().toString();
        LOGGER.error("Fallo de persistencia sanitaria; correlacionId={}", correlacion);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(new ErrorSanitario("SANIDAD_NO_DISPONIBLE",
                        "No fue posible procesar la información sanitaria.", Map.of(), correlacion));
    }

    private ResponseEntity<ErrorSanitario> respuesta(HttpStatus estado, String codigo,
            String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore())
                .body(new ErrorSanitario(codigo, mensaje, errores,
                        UUID.randomUUID().toString()));
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record ErrorSanitario(String codigo, String mensajeSeguro,
            Map<String, String> erroresPorCampo, String correlacionId) { }
}
