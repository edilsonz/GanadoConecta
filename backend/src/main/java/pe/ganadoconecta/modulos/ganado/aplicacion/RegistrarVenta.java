package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;

public final class RegistrarVenta {
    public record Recibo(UUID operationId, UUID animalId, Instant registradaEn, int anunciosCerrados, boolean reprocesada) {}
    public interface Repositorio {
        Recibo registrar(UUID id, UUID ganaderia, UUID animal, UUID actor, Instant ahora);
    }
    public static final class Conflicto extends RuntimeException {}
    private final UnidadDeTrabajo unidad;
    private final AutorizarAccesoGanaderiaTransaccional autorizar;
    private final Repositorio repositorio;
    private final Clock reloj;

    public RegistrarVenta(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional autorizar,
            Repositorio repositorio, Clock reloj) {
        this.unidad=unidad; this.autorizar=autorizar; this.repositorio=repositorio; this.reloj=reloj;
    }
    public Recibo ejecutar(UUID id, UUID ganaderia, UUID animal, UUID actor, UUID sesion) {
        if (id == null || id.version()!=4 || id.variant()!=2) throw new IllegalArgumentException("Operación inválida");
        return unidad.ejecutar(() -> {
            var ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
            autorizar.autorizar(actor,sesion,ganaderia,PermisoGanaderia.ADMINISTRAR,ahora);
            return repositorio.registrar(id,ganaderia,animal,actor,ahora);
        });
    }
}
