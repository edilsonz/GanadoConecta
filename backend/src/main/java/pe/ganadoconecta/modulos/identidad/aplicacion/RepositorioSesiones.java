package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

public interface RepositorioSesiones {
    void crear(NuevaSesion sesion);

    Rotacion rotar(String huellaActual, UUID sucesorId, String huellaSucesor, Instant ahora);

    boolean estaVigente(UUID sesionId, UUID usuarioId, Instant ahora);

    record NuevaSesion(UUID sesionId, UUID usuarioId, Instant creadoEn, Instant venceEn,
            UUID verificadorId, String huellaRenovacion) {
    }

    record Rotacion(Estado estado, UUID sesionId, UUID usuarioId, Instant venceEn) {
        public static Rotacion valida(UUID sesionId, UUID usuarioId, Instant venceEn) {
            return new Rotacion(Estado.VALIDA, sesionId, usuarioId, venceEn);
        }

        public static Rotacion invalida() {
            return new Rotacion(Estado.INVALIDA, null, null, null);
        }

        public static Rotacion reutilizada() {
            return new Rotacion(Estado.REUTILIZADA, null, null, null);
        }
    }

    enum Estado {
        VALIDA,
        INVALIDA,
        REUTILIZADA
    }
}
