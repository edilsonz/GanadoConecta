package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.util.Objects;

/** Revoca de forma idempotente la sesión reconocida por cualquier token de su familia. */
public final class CerrarSesion {
    private final RepositorioSesiones sesiones;
    private final GeneradorCredencialesRenovacion renovaciones;
    private final Clock reloj;

    public CerrarSesion(RepositorioSesiones sesiones,
            GeneradorCredencialesRenovacion renovaciones, Clock reloj) {
        this.sesiones = Objects.requireNonNull(sesiones);
        this.renovaciones = Objects.requireNonNull(renovaciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public void ejecutar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            return;
        }
        sesiones.revocarPorHuella(
                renovaciones.calcularHuella(tokenRenovacion), reloj.instant());
    }
}
