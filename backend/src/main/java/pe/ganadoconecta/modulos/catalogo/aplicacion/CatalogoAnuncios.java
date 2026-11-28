package pe.ganadoconecta.modulos.catalogo.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.ganado.dominio.Categoria;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;

public final class CatalogoAnuncios {
    public enum Estado { BORRADOR, PUBLICADO, PAUSADO, RETIRADO, CERRADO_VENTA }
    public enum Accion { PUBLICAR, PAUSAR, RETIRAR }
    public enum Error { INVALIDO, AUSENTE, CONFLICTO, ALMACEN_NO_DISPONIBLE }
    public static final class Fallo extends RuntimeException {
        public final Error tipo;
        public Fallo(Error tipo) { this.tipo = tipo; }
    }
    public record Datos(String titulo, String caracteristicas, String ubicacionGeneral, BigDecimal precioPen,
            String contacto, boolean genealogiaAutorizada) {}
    public record Privado(UUID id, Datos datos, Estado estado, long version, List<UUID> fotos) {}
    public record Progenitor(String relacion, String categoria, String raza) {}
    public record Publico(UUID id, String titulo, String caracteristicas, String categoria, String raza,
            String sexo, String ubicacionGeneral, BigDecimal precioPen, String contacto,
            List<Progenitor> genealogia, List<UUID> fotos) {}
    public record Pagina(List<Publico> items, Integer siguientePagina) {}
    public record Filtros(Categoria categoria, Sexo sexo, String raza, String ubicacion,
            BigDecimal minimo, BigDecimal maximo, int pagina) {}
    public record Ambito(UUID ganaderia, UUID animal, UUID actor, UUID sesion) {}

    public interface Repositorio {
        void exigirAnimalActivo(UUID ganaderia, UUID animal);
        List<Privado> privados(UUID ganaderia, UUID animal);
        Privado privado(UUID ganaderia, UUID animal, UUID id);
        void crear(Ambito ambito, UUID id, Datos datos, Instant ahora);
        void editar(UUID id, Datos datos, Instant ahora);
        void estado(UUID id, Estado estado, Instant ahora);
        void agregarFoto(UUID anuncio, UUID foto, int bytes, Instant ahora);
        boolean fotoPublica(UUID anuncio, UUID foto);
        List<Publico> buscar(Filtros filtros, int limite, int desplazamiento);
    }
    public interface Fotos {
        void guardar(UUID id, byte[] jpeg);
        byte[] leer(UUID id);
    }
    @FunctionalInterface public interface PrepararFoto {
        byte[] aJpeg(byte[] archivo, String tipo);
    }
    @FunctionalInterface public interface CachePublica {
        Pagina obtener(Filtros filtros, Supplier<Pagina> origen);
    }

    private final UnidadDeTrabajo unidad;
    private final AutorizarAccesoGanaderiaTransaccional autorizar;
    private final Repositorio repo;
    private final Fotos fotos;
    private final PrepararFoto prepararFoto;
    private final Clock reloj;
    private final CachePublica cache;

    public CatalogoAnuncios(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional autorizar,
            Repositorio repo, Fotos fotos, PrepararFoto prepararFoto, Clock reloj, CachePublica cache) {
        this.unidad = unidad; this.autorizar = autorizar; this.repo = repo;
        this.fotos = fotos; this.prepararFoto = prepararFoto; this.reloj = reloj;
        this.cache = cache;
    }

    public List<Privado> privados(Ambito a) {
        return propietario(a, () -> repo.privados(a.ganaderia(), a.animal()));
    }

    public Privado crear(Ambito a, UUID id, Datos datos) {
        if (id == null || id.version() != 4 || id.variant() != 2) throw new Fallo(Error.INVALIDO);
        return propietario(a, () -> {
            repo.exigirAnimalActivo(a.ganaderia(), a.animal());
            var anterior = repo.privado(a.ganaderia(), a.animal(), id);
            if (anterior != null) {
                if (!anterior.datos().equals(datos)) throw new Fallo(Error.CONFLICTO);
                return anterior;
            }
            repo.crear(a, id, datos, reloj.instant());
            return exigirAnuncio(a, id);
        });
    }

    public Privado editar(Ambito a, UUID id, long version, Datos datos) {
        return propietario(a, () -> {
            repo.exigirAnimalActivo(a.ganaderia(), a.animal());
            var actual = exigirAnuncio(a, id);
            editable(actual, version);
            repo.editar(id, datos, reloj.instant());
            return exigirAnuncio(a, id);
        });
    }

    public Privado cambiarEstado(Ambito a, UUID id, long version, Accion accion) {
        if (accion == null) throw new Fallo(Error.INVALIDO);
        return propietario(a, () -> {
            var actual = exigirAnuncio(a, id);
            editable(actual, version);
            Estado nuevo = switch (accion) {
                case PUBLICAR -> {
                    repo.exigirAnimalActivo(a.ganaderia(), a.animal());
                    if (actual.estado() != Estado.BORRADOR && actual.estado() != Estado.PAUSADO)
                        throw new Fallo(Error.CONFLICTO);
                    yield Estado.PUBLICADO;
                }
                case PAUSAR -> {
                    if (actual.estado() != Estado.PUBLICADO) throw new Fallo(Error.CONFLICTO);
                    yield Estado.PAUSADO;
                }
                case RETIRAR -> Estado.RETIRADO;
            };
            repo.estado(id, nuevo, reloj.instant());
            return exigirAnuncio(a, id);
        });
    }

    public Privado agregarFoto(Ambito a, UUID id, long version, byte[] archivo, String formato) {
        return propietario(a, () -> {
            repo.exigirAnimalActivo(a.ganaderia(), a.animal());
            var actual = exigirAnuncio(a, id);
            editable(actual, version);
            if (actual.fotos().size() >= 5) throw new Fallo(Error.INVALIDO);
            byte[] jpeg = prepararFoto.aJpeg(archivo, formato);
            UUID foto = UUID.randomUUID();
            repo.agregarFoto(id, foto, jpeg.length, reloj.instant());
            fotos.guardar(foto, jpeg);
            return exigirAnuncio(a, id);
        });
    }

    public byte[] fotoPublica(UUID anuncio, UUID foto) {
        if (!repo.fotoPublica(anuncio, foto)) throw new Fallo(Error.AUSENTE);
        return fotos.leer(foto);
    }

    public Pagina buscar(Filtros f) {
        if (f.pagina() < 1 || f.pagina() > 500 || (f.minimo() != null && f.maximo() != null
                && f.minimo().compareTo(f.maximo()) > 0)) throw new Fallo(Error.INVALIDO);
        return cache.obtener(f, () -> consultarPagina(f));
    }

    private Pagina consultarPagina(Filtros f) {
        var filas = repo.buscar(f, 21, (f.pagina() - 1) * 20);
        return new Pagina(filas.size() > 20 ? List.copyOf(filas.subList(0,20)) : filas,
                filas.size() > 20 && f.pagina() < 500 ? f.pagina()+1 : null);
    }

    public static Datos datos(String titulo, String caracteristicas, String ubicacion, BigDecimal precio,
            String contacto, boolean autorizaContacto, boolean genealogia) {
        String textoContacto = texto(contacto, 180, false);
        if (autorizaContacto != (textoContacto != null)) throw new Fallo(Error.INVALIDO);
        return new Datos(texto(titulo,120,true), texto(caracteristicas,1000,true),
                texto(ubicacion,100,true), precio(precio,true), textoContacto, genealogia);
    }

    public static BigDecimal precio(BigDecimal valor, boolean requerido) {
        if (!requerido && valor == null) return null;
        if (valor == null || valor.signum() < (requerido ? 1 : 0)
                || valor.compareTo(new BigDecimal("10000000000")) >= 0 || valor.stripTrailingZeros().scale() > 2) {
            throw new Fallo(Error.INVALIDO);
        }
        return valor.setScale(2);
    }

    public static String texto(String valor, int max, boolean obligatorio) {
        String normal = valor == null ? "" : valor.strip();
        if (normal.isEmpty() && !obligatorio) return null;
        if (normal.isEmpty() || normal.codePointCount(0,normal.length()) > max || normal.codePoints()
                .anyMatch(c -> Character.getType(c)==Character.FORMAT || Character.isISOControl(c) && c!='\n' && c!='\r')) {
            throw new Fallo(Error.INVALIDO);
        }
        return normal;
    }

    private <T> T propietario(Ambito a, Supplier<T> trabajo) {
        return unidad.ejecutar(() -> {
            autorizar.autorizar(a.actor(), a.sesion(), a.ganaderia(), PermisoGanaderia.ADMINISTRAR, reloj.instant());
            return trabajo.get();
        });
    }
    private Privado exigirAnuncio(Ambito a, UUID id) {
        var actual = repo.privado(a.ganaderia(), a.animal(), id);
        if (actual == null) throw new Fallo(Error.AUSENTE);
        return actual;
    }
    private static void editable(Privado actual, long version) {
        if (actual.estado() == Estado.RETIRADO || actual.estado() == Estado.CERRADO_VENTA
                || actual.version() != version) throw new Fallo(Error.CONFLICTO);
    }
}
