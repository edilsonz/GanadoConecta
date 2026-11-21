package pe.ganadoconecta.modulos.reproduccion.adaptadores.entrada.http;
import java.util.*;import org.springframework.context.annotation.Profile;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;import pe.ganadoconecta.modulos.reproduccion.aplicacion.*;import pe.ganadoconecta.modulos.reproduccion.dominio.DatoReproductivoInvalidoException;
@RestControllerAdvice(assignableTypes={ReproduccionControlador.class,PartosControlador.class,GenealogiaControlador.class}) @Profile("postgres")
public class ErroresReproduccionControlador{
 record Error(String codigo,String mensajeSeguro,Map<String,String> erroresPorCampo){}
 @ExceptionHandler(DatoReproductivoInvalidoException.class)ResponseEntity<Error> invalido(DatoReproductivoInvalidoException e){return ResponseEntity.unprocessableEntity().body(new Error("DATOS_INVALIDOS","Revise los datos reproductivos.",Map.of(e.campo(),"El campo no es válido.")));}
 @ExceptionHandler(ConflictoReproductivoException.class)ResponseEntity<Error> conflicto(){return ResponseEntity.status(409).body(new Error("CONFLICTO","La operación entra en conflicto.",Map.of()));}
 @ExceptionHandler(AccesoGanaderiaDenegadoException.class)ResponseEntity<Error> acceso(){return ResponseEntity.status(403).body(new Error("ACCESO_DENEGADO","No tiene acceso al registro solicitado.",Map.of()));}
 @ExceptionHandler(AnimalNoEncontradoException.class)ResponseEntity<Error> animal(){return ResponseEntity.status(404).body(new Error("ANIMAL_NO_ENCONTRADO","No se encontró el animal solicitado.",Map.of()));}
 @ExceptionHandler(PersistenciaReproductivaException.class)ResponseEntity<Error> persistencia(){return ResponseEntity.status(503).body(new Error("REPRODUCCION_NO_DISPONIBLE","No fue posible procesar la información.",Map.of()));}
}
