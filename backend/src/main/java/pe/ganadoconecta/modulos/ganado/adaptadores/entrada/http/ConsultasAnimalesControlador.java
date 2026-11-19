package pe.ganadoconecta.modulos.ganado.adaptadores.entrada.http;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultaAnimalesInvalidaException;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultarFichaAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.CursorListadoAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.ListarAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.PaginaAnimales;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatoAnimalInvalidoException;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;

/** T09: listado y ficha privada de animales por ganadería autorizada. */
@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/animales")
@Profile("postgres")
public class ConsultasAnimalesControlador {
    private static final Set<String> PARAMETROS_LISTADO = Set.of("limit", "cursor");

    private final ListarAnimales listar;
    private final ConsultarFichaAnimal consultarFicha;

    public ConsultasAnimalesControlador(ListarAnimales listar,
            ConsultarFichaAnimal consultarFicha) {
        this.listar = listar;
        this.consultarFicha = consultarFicha;
    }

    @GetMapping
    ResponseEntity<PaginaAnimalesRespuesta> listar(
            @PathVariable UUID ganaderiaId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam MultiValueMap<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        validarParametros(parametros, PARAMETROS_LISTADO);
        PaginaAnimales pagina = listar.ejecutar(
                identidad.usuarioId(), new GanaderiaId(ganaderiaId), limit,
                CursorHttp.decodificar(cursor));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new PaginaAnimalesRespuesta(
                        pagina.items().stream().map(AnimalRespuesta::desde).toList(),
                        CursorHttp.codificar(pagina.siguienteCursor()),
                        pagina.consultadoEn()));
    }

    @GetMapping("/{animalId}")
    ResponseEntity<AnimalRespuesta> consultarFicha(
            @PathVariable UUID ganaderiaId,
            @PathVariable UUID animalId,
            @RequestParam MultiValueMap<String, String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        validarParametros(parametros, Set.of());
        Animal animal = consultarFicha.ejecutar(
                identidad.usuarioId(), new GanaderiaId(ganaderiaId),
                new AnimalId(animalId));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AnimalRespuesta.desde(animal));
    }

    private static void validarParametros(MultiValueMap<String, String> parametros,
            Set<String> admitidos) {
        if (!admitidos.containsAll(parametros.keySet())
                || parametros.values().stream().anyMatch(valores -> valores.size() != 1)) {
            throw new ConsultaAnimalesInvalidaException();
        }
    }

    record PaginaAnimalesRespuesta(
            List<AnimalRespuesta> items,
            String siguienteCursor,
            Instant consultadoEn) {
    }

    record AnimalRespuesta(
            UUID animalId,
            UUID ganaderiaId,
            String sexo,
            String nombre,
            String areteFrente,
            String areteReverso,
            String raza,
            LocalDate fechaNacimiento,
            Boolean nacimientoEstimado,
            String categoria,
            String observaciones,
            String estado,
            long version,
            Instant creadoEn) {
        private static AnimalRespuesta desde(Animal animal) {
            return new AnimalRespuesta(
                    animal.id().valor(), animal.ganaderiaId().valor(),
                    animal.datos().sexo().name(), animal.datos().nombre(),
                    animal.datos().areteFrente(), animal.datos().areteReverso(),
                    animal.datos().raza(), animal.datos().fechaNacimiento(),
                    animal.datos().nacimientoEstimado(),
                    animal.datos().categoria() == null
                            ? null : animal.datos().categoria().name(),
                    animal.datos().observaciones(), animal.estado().name(),
                    animal.version(), animal.creadoEn());
        }
    }

    private static final class CursorHttp {
        private static final int LONGITUD_MAXIMA = 256;

        private static CursorListadoAnimales decodificar(String valor) {
            if (valor == null) {
                return null;
            }
            if (valor.isBlank() || valor.length() > LONGITUD_MAXIMA) {
                throw new ConsultaAnimalesInvalidaException();
            }
            try {
                String contenido = new String(
                        Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8);
                String[] partes = contenido.split("\\|", -1);
                if (partes.length != 2) {
                    throw new ConsultaAnimalesInvalidaException();
                }
                return new CursorListadoAnimales(
                        Instant.parse(partes[0]), new AnimalId(UUID.fromString(partes[1])));
            } catch (IllegalArgumentException | DateTimeException
                    | DatoAnimalInvalidoException exception) {
                throw new ConsultaAnimalesInvalidaException();
            }
        }

        private static String codificar(CursorListadoAnimales cursor) {
            if (cursor == null) {
                return null;
            }
            String contenido = cursor.creadoEn() + "|" + cursor.animalId().valor();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    contenido.getBytes(StandardCharsets.UTF_8));
        }
    }
}
