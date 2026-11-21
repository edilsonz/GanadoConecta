package pe.ganadoconecta.modulos.catalogo.adaptadores.entrada.http;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.catalogo.adaptadores.salida.FotoJpeg;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.ganado.dominio.Categoria;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;

@RestController
@RequestMapping("/api/v1")
@Profile("postgres")
public class CatalogoControlador {
    private static final String ANUNCIOS="/ganaderias/{g}/animales/{animal}/anuncios";
    private final CatalogoAnuncios catalogo;
    public CatalogoControlador(CatalogoAnuncios catalogo) { this.catalogo = catalogo; }

    @GetMapping("/catalogo")
    ResponseEntity<Pagina> buscar(@RequestParam(required=false) Categoria categoria,
            @RequestParam(required=false) Sexo sexo, @RequestParam(required=false) String raza,
            @RequestParam(required=false) String ubicacion, @RequestParam(required=false) BigDecimal minimo,
            @RequestParam(required=false) BigDecimal maximo, @RequestParam(defaultValue="1") int pagina) {
        return respuesta(200, catalogo.buscar(new Filtros(categoria, sexo, CatalogoAnuncios.texto(raza,100,false),
                CatalogoAnuncios.texto(ubicacion,100,false), CatalogoAnuncios.precio(minimo,false),
                CatalogoAnuncios.precio(maximo,false),pagina)));
    }

    @GetMapping("/catalogo/{anuncio}/fotos/{foto}")
    ResponseEntity<byte[]> foto(@PathVariable UUID anuncio,@PathVariable UUID foto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .body(catalogo.fotoPublica(anuncio,foto));
    }

    @GetMapping(ANUNCIOS)
    ResponseEntity<List<Privado>> privados(@PathVariable UUID g,@PathVariable UUID animal,
            @AuthenticationPrincipal IdentidadAutenticada i) {
        return respuesta(200,catalogo.privados(ambito(g,animal,i)));
    }

    @PostMapping(ANUNCIOS)
    ResponseEntity<Privado> crear(@PathVariable UUID g,@PathVariable UUID animal,
            @AuthenticationPrincipal IdentidadAutenticada i,@RequestBody Solicitud s) {
        return respuesta(201,catalogo.crear(ambito(g,animal,i),s.id(),s.datos()));
    }

    @PutMapping(ANUNCIOS+"/{id}")
    ResponseEntity<Privado> editar(@PathVariable UUID g,@PathVariable UUID animal,@PathVariable UUID id,
            @AuthenticationPrincipal IdentidadAutenticada i,@RequestBody Solicitud s) {
        return respuesta(200,catalogo.editar(ambito(g,animal,i),id,s.version(),s.datos()));
    }

    @PostMapping(ANUNCIOS+"/{id}/estado")
    ResponseEntity<Privado> estado(@PathVariable UUID g,@PathVariable UUID animal,@PathVariable UUID id,
            @AuthenticationPrincipal IdentidadAutenticada i,@RequestBody Cambio s) {
        return respuesta(200,catalogo.cambiarEstado(ambito(g,animal,i),id,s.version(),s.accion()));
    }

    @PostMapping(value=ANUNCIOS+"/{id}/fotos", consumes={"image/jpeg","image/png"})
    ResponseEntity<Privado> cargarFoto(@PathVariable UUID g,@PathVariable UUID animal,@PathVariable UUID id,
            @AuthenticationPrincipal IdentidadAutenticada i,@RequestParam long version,HttpServletRequest solicitud)
            throws IOException {
        if (solicitud.getContentLengthLong() > FotoJpeg.MAX_BYTES) return respuesta(413,null);
        byte[] cuerpo = solicitud.getInputStream().readNBytes(FotoJpeg.MAX_BYTES+1);
        if (cuerpo.length > FotoJpeg.MAX_BYTES) return respuesta(413,null);
        return respuesta(201,catalogo.agregarFoto(ambito(g,animal,i),id,version,cuerpo,solicitud.getContentType()));
    }

    @ExceptionHandler(Fallo.class)
    ResponseEntity<Void> fallo(Fallo fallo) {
        return respuesta(switch(fallo.tipo) {
            case INVALIDO -> 422;
            case AUSENTE -> 404;
            case CONFLICTO -> 409;
            case ALMACEN_NO_DISPONIBLE -> 503;
        }, null);
    }

    @ExceptionHandler(AccesoGanaderiaDenegadoException.class)
    ResponseEntity<Void> denegado() { return respuesta(403,null); }

    private static <T> ResponseEntity<T> respuesta(int estado,T cuerpo) {
        return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(cuerpo);
    }
    private static Ambito ambito(UUID g,UUID animal,IdentidadAutenticada i) {
        return new Ambito(g,animal,i.usuarioId(),i.sesionId());
    }

    record Solicitud(UUID id,long version,String titulo,String caracteristicas,String ubicacionGeneral,
            BigDecimal precioPen,String contacto,boolean autorizaContacto,boolean genealogiaAutorizada) {
        Datos datos() { return CatalogoAnuncios.datos(titulo,caracteristicas,ubicacionGeneral,precioPen,
                contacto,autorizaContacto,genealogiaAutorizada); }
        @Override public String toString() { return "Solicitud[datos=PROTEGIDOS]"; }
    }
    record Cambio(long version,Accion accion) {}
}
