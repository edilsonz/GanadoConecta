package pe.ganadoconecta.modulos.ganado.adaptadores.entrada.http;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.ResultadoRegistroAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.SolicitudRegistroAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.Categoria;
import pe.ganadoconecta.modulos.ganado.dominio.DatoAnimalInvalidoException;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.ganado.dominio.UbicacionIdentificacionTextil;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;

/** T08: adaptador HTTP único para el alta individual de animales. */
@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/animales")
@Profile("postgres")
public class AnimalesControlador {
    private final RegistrarAnimal registrar;

    public AnimalesControlador(RegistrarAnimal registrar) {
        this.registrar = registrar;
    }

    @PostMapping
    ResponseEntity<RegistroAnimalRespuesta> registrar(
            @PathVariable UUID ganaderiaId,
            @RequestBody RegistroAnimalSolicitud solicitud,
            @RequestParam Map<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        if (!parametros.isEmpty()) {
            throw new SolicitudAnimalHttpInvalidaException();
        }
        ResultadoRegistroAnimal resultado = registrar.ejecutar(
                solicitud.aDominio(ganaderiaId),
                identidad.usuarioId(), identidad.sesionId());
        RegistroAnimalRespuesta respuesta = new RegistroAnimalRespuesta(
                resultado.operacionId().valor(), resultado.animalId().valor(),
                resultado.estado().name(), resultado.version(), resultado.confirmadoEn(),
                resultado.areteCoincidente()
                        ? List.of("ARETE_COINCIDENTE") : List.of());
        HttpStatus estado = resultado.estado()
                == ResultadoRegistroAnimal.EstadoResultado.APLICADO
                ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado)
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    record RegistroAnimalSolicitud(
            UUID operationId,
            UUID animalId,
            String tipo,
            Integer schemaVersion,
            Long baseVersion,
            String sexo,
            String nombre,
            String areteFrente,
            String areteReverso,
            String coloresIdentificacionTextil,
            String ubicacionIdentificacionTextil,
            String raza,
            LocalDate fechaNacimiento,
            Boolean nacimientoEstimado,
            String categoria,
            String observaciones) {

        SolicitudRegistroAnimal aDominio(UUID ganaderiaId) {
            if (operationId == null) {
                throw new DatoAnimalInvalidoException("operationId");
            }
            if (animalId == null) {
                throw new DatoAnimalInvalidoException("animalId");
            }
            if (schemaVersion == null) {
                throw new DatoAnimalInvalidoException("schemaVersion");
            }
            if (baseVersion == null) {
                throw new DatoAnimalInvalidoException("baseVersion");
            }
            return new SolicitudRegistroAnimal(
                    new GanaderiaId(ganaderiaId),
                    new AnimalId(animalId),
                    new OperacionId(operationId),
                    tipo, schemaVersion, baseVersion,
                    new DatosAnimal(sexo(sexo), nombre, areteFrente, areteReverso,
                            coloresIdentificacionTextil,
                            ubicacionIdentificacionTextil(ubicacionIdentificacionTextil),
                            raza, fechaNacimiento, nacimientoEstimado,
                            categoria(categoria), observaciones));
        }

        private static Sexo sexo(String valor) {
            if (valor == null || valor.isBlank()) {
                throw new DatoAnimalInvalidoException("sexo");
            }
            try {
                return Sexo.valueOf(valor);
            } catch (IllegalArgumentException exception) {
                throw new DatoAnimalInvalidoException("sexo");
            }
        }

        private static Categoria categoria(String valor) {
            if (valor == null || valor.isBlank()) {
                return null;
            }
            try {
                return Categoria.valueOf(valor);
            } catch (IllegalArgumentException exception) {
                throw new DatoAnimalInvalidoException("categoria");
            }
        }

        private static UbicacionIdentificacionTextil ubicacionIdentificacionTextil(
                String valor) {
            if (valor == null || valor.isBlank()) {
                return null;
            }
            try {
                return UbicacionIdentificacionTextil.valueOf(valor);
            } catch (IllegalArgumentException exception) {
                throw new DatoAnimalInvalidoException(
                        "ubicacionIdentificacionTextil");
            }
        }

        @Override
        public String toString() {
            return "RegistroAnimalSolicitud[datos=PROTEGIDOS]";
        }
    }

    record RegistroAnimalRespuesta(
            UUID operationId,
            UUID animalId,
            String resultado,
            long versionCreada,
            Instant confirmadoEn,
            List<String> advertencias) {
    }
}

final class SolicitudAnimalHttpInvalidaException extends RuntimeException {
}
