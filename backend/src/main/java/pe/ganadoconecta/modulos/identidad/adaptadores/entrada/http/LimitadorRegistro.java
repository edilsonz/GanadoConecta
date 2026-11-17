package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Límite local y acotado; una solución distribuida requiere decisión posterior. */
public final class LimitadorRegistro {
    private final int maxIntentos;
    private final Duration duracion;
    private final int maxOrigenes;
    private final Clock reloj;
    private final Map<String, Ventana> ventanas = new HashMap<>();

    public LimitadorRegistro(int maxIntentos, Duration duracion, int maxOrigenes, Clock reloj) {
        if (maxIntentos < 1 || duracion.isZero() || duracion.isNegative() || maxOrigenes < 1) {
            throw new IllegalArgumentException("Configuración inválida del límite de registro");
        }
        this.maxIntentos = maxIntentos;
        this.duracion = duracion;
        this.maxOrigenes = maxOrigenes;
        this.reloj = reloj;
    }

    public synchronized Resultado intentar(String origen) {
        Instant ahora = reloj.instant();
        Ventana actual = ventanas.get(origen);
        if (actual != null && !ahora.isBefore(actual.inicio().plus(duracion))) {
            ventanas.remove(origen);
            actual = null;
        }
        if (actual == null) {
            limpiarVencidas(ahora);
            if (ventanas.size() >= maxOrigenes) {
                return new Resultado(false, duracion.toSeconds());
            }
            ventanas.put(origen, new Ventana(ahora, 1));
            return new Resultado(true, 0);
        }

        int siguiente = actual.intentos() + 1;
        ventanas.put(origen, new Ventana(actual.inicio(), siguiente));
        long restante = Math.max(1,
                Duration.between(ahora, actual.inicio().plus(duracion)).toSeconds());
        return new Resultado(siguiente <= maxIntentos, siguiente <= maxIntentos ? 0 : restante);
    }

    private void limpiarVencidas(Instant ahora) {
        ventanas.entrySet().removeIf(entry ->
                !ahora.isBefore(entry.getValue().inicio().plus(duracion)));
    }

    private record Ventana(Instant inicio, int intentos) {
    }

    public record Resultado(boolean permitido, long reintentarEnSegundos) {
    }
}
