package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;

public final class ConsultarAdministracion {
    private static final int TAMANO = 20;

    public enum Estado { ACTIVO, INACTIVO }
    public enum Operacion { CONSULTAR_USUARIOS, CONSULTAR_ESTADISTICAS }
    public record Usuario(UUID id, String correo, Estado estado, Instant creadoEn) {}
    public record Pagina(List<Usuario> items, UUID siguienteCursor) {}
    public record Estadisticas(long cuentas, long cuentasActivas, long ganaderias, long ganaderiasActivas) {}
    public record Filtro(UUID id, String correo, Estado estado, UUID cursor) {
        public static Filtro de(String buscar, Estado estado, UUID cursor) {
            if (buscar == null || buscar.isBlank()) return new Filtro(null, null, estado, cursor);
            String valor = buscar.strip().toLowerCase(Locale.ROOT);
            if (valor.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
                return new Filtro(UUID.fromString(valor), null, estado, cursor);
            }
            if (valor.length() > 254 || !valor.matches("[^@\\s\\p{Cntrl}]+@[^@\\s\\p{Cntrl}]+")) {
                throw new IllegalArgumentException();
            }
            return new Filtro(null, valor, estado, cursor);
        }
    }

    public static final class AccesoDenegado extends RuntimeException {}
    public interface Repositorio {
        boolean permisoVigente(UUID actor, UUID sesion, Instant ahora);
        List<Usuario> listar(Filtro filtro, int limite);
        Estadisticas estadisticas();
        void auditar(UUID actor, Operacion operacion, Instant ahora);
    }

    private final Repositorio repo;
    private final UnidadDeTrabajo unidad;
    private final Clock reloj;

    public ConsultarAdministracion(Repositorio repo, UnidadDeTrabajo unidad, Clock reloj) {
        this.repo = repo;
        this.unidad = unidad;
        this.reloj = reloj;
    }

    public boolean acceso(UUID actor, UUID sesion) {
        return unidad.ejecutar(() -> { autorizar(actor, sesion); return true; });
    }

    public Pagina usuarios(UUID actor, UUID sesion, Filtro filtro) {
        return unidad.ejecutar(() -> {
            Instant ahora = autorizar(actor, sesion);
            var filas = repo.listar(filtro, TAMANO + 1);
            var items = List.copyOf(filas.subList(0, Math.min(filas.size(), TAMANO)));
            var siguiente = filas.size() > TAMANO ? items.getLast().id() : null;
            repo.auditar(actor, Operacion.CONSULTAR_USUARIOS, ahora);
            return new Pagina(items, siguiente);
        });
    }

    public Estadisticas estadisticas(UUID actor, UUID sesion) {
        return unidad.ejecutar(() -> {
            Instant ahora = autorizar(actor, sesion);
            var datos = repo.estadisticas();
            repo.auditar(actor, Operacion.CONSULTAR_ESTADISTICAS, ahora);
            return datos;
        });
    }

    private Instant autorizar(UUID actor, UUID sesion) {
        Instant ahora = reloj.instant();
        if (!repo.permisoVigente(actor, sesion, ahora)) throw new AccesoDenegado();
        return ahora;
    }
}
