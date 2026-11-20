package pe.ganadoconecta.modulos.sanidad.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Vacunacion(UUID id, UUID ganaderiaId, UUID animalId,
        String nombreVacuna, LocalDate fechaAplicacion, String responsable,
        String lote, String observaciones, LocalDate proximaFecha,
        UUID creadoPor, Instant creadoEn) {
    public Vacunacion {
        validarUuid(id, "eventId");
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(animalId);
        nombreVacuna = texto(nombreVacuna, "nombreVacuna", 150, true);
        if (fechaAplicacion == null) throw new DatoSanitarioInvalidoException("fechaAplicacion");
        responsable = texto(responsable, "responsable", 150, false);
        lote = texto(lote, "lote", 100, false);
        observaciones = texto(observaciones, "observaciones", 2000, false);
        if (proximaFecha != null && proximaFecha.isBefore(fechaAplicacion)) {
            throw new DatoSanitarioInvalidoException("proximaFecha");
        }
        Objects.requireNonNull(creadoPor);
        Objects.requireNonNull(creadoEn);
    }

    public void validarFechaNoFutura(LocalDate hoy) {
        if (fechaAplicacion.isAfter(hoy)) {
            throw new DatoSanitarioInvalidoException("fechaAplicacion");
        }
    }

    private static String texto(String valor, String campo, int maximo, boolean requerido) {
        if (valor == null || valor.strip().isEmpty()) {
            if (requerido) throw new DatoSanitarioInvalidoException(campo);
            return null;
        }
        String limpio = valor.strip();
        if (limpio.codePointCount(0, limpio.length()) > maximo || limpio.chars()
                .anyMatch(c -> (c < 32 && c != '\n' && c != '\r' && c != '\t') || c == 127)) {
            throw new DatoSanitarioInvalidoException(campo);
        }
        return limpio;
    }

    private static void validarUuid(UUID valor, String campo) {
        if (valor == null || valor.version() != 4) {
            throw new DatoSanitarioInvalidoException(campo);
        }
    }
}
