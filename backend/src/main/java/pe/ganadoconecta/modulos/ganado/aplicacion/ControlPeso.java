package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;

public final class ControlPeso {
    public record Medicion(UUID id, BigDecimal kilos, LocalDate fecha, String observaciones) {}
    public record Registro(Medicion medicion, UUID ganaderia, UUID animal, UUID actor, Instant ahora) {}
    public enum Resultado { APLICADO, REPROCESADO, CONFLICTO }
    public interface Repositorio {
        List<Medicion> listar(UUID ganaderia, UUID animal);
        Resultado registrar(Registro registro);
    }
    public static final class DatoInvalido extends RuntimeException {}

    private final UnidadDeTrabajo unidad;
    private final AutorizarAccesoGanaderiaTransaccional escribir;
    private final AutorizarLecturaGanaderia leer;
    private final Repositorio repositorio;
    private final Clock reloj;

    public ControlPeso(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional escribir,
            AutorizarLecturaGanaderia leer, Repositorio repositorio, Clock reloj) {
        this.unidad = unidad;
        this.escribir = escribir;
        this.leer = leer;
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public List<Medicion> listar(UUID ganaderia, UUID animal, UUID actor) {
        leer.autorizar(actor, ganaderia);
        return repositorio.listar(ganaderia, animal);
    }

    public Resultado registrar(UUID ganaderia, UUID animal, UUID actor, UUID sesion, Medicion dato) {
        LocalDate hoy = LocalDate.now(reloj.withZone(ZoneId.of("America/Lima")));
        BigDecimal kilos = dato.kilos();
        if (dato.id() == null || dato.id().version() != 4 || dato.id().variant() != 2
                || kilos == null || kilos.signum() <= 0 || kilos.stripTrailingZeros().scale() > 3
                || kilos.compareTo(new BigDecimal("1000000000")) >= 0
                || dato.fecha() == null || dato.fecha().isAfter(hoy)) throw new DatoInvalido();
        String notas = dato.observaciones() == null ? "" : dato.observaciones().strip();
        if (notas.codePointCount(0, notas.length()) > 2000 || notas.codePoints()
                .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r')) throw new DatoInvalido();
        Medicion medicion = new Medicion(dato.id(), kilos, dato.fecha(), notas.isEmpty() ? null : notas);
        return unidad.ejecutar(() -> {
            escribir.autorizar(actor, sesion, ganaderia, PermisoGanaderia.ESCRIBIR, reloj.instant());
            return repositorio.registrar(new Registro(medicion, ganaderia, animal, actor, reloj.instant()));
        });
    }
}
