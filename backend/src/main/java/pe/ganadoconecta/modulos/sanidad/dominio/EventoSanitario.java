package pe.ganadoconecta.modulos.sanidad.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Hecho sanitario inmutable informado por una persona autorizada. */
public record EventoSanitario(UUID id, UUID ganaderiaId, UUID animalId,
        TipoEventoSanitario tipo, LocalDate fecha, String nombre,
        String descripcion, String responsable, String lote, String producto,
        String indicaciones, String observaciones, LocalDate proximaFecha,
        UUID creadoPor, Instant creadoEn) {
    public EventoSanitario {
        if (id == null || id.version() != 4) invalido("eventId");
        Objects.requireNonNull(ganaderiaId);
        Objects.requireNonNull(animalId);
        if (tipo == null) invalido("tipo");
        if (fecha == null) invalido("fecha");
        nombre = texto(nombre, "nombre", 150, tipo == TipoEventoSanitario.VACUNACION);
        descripcion = texto(descripcion, "descripcion", 1000,
                tipo != TipoEventoSanitario.VACUNACION);
        responsable = texto(responsable, "responsable", 150, false);
        lote = texto(lote, "lote", 100, false);
        producto = texto(producto, "producto", 150, false);
        indicaciones = texto(indicaciones, "indicaciones", 1000, false);
        observaciones = texto(observaciones, "observaciones", 2000, false);
        if (proximaFecha != null && proximaFecha.isBefore(fecha)) invalido("proximaFecha");
        Objects.requireNonNull(creadoPor);
        Objects.requireNonNull(creadoEn);
    }

    public void validarRealizadoNoFuturo(LocalDate hoy) {
        if (fecha.isAfter(hoy)) invalido("fecha");
    }

    private static String texto(String valor, String campo, int max, boolean requerido) {
        if (valor == null || valor.strip().isEmpty()) {
            if (requerido) invalido(campo);
            return null;
        }
        String limpio = valor.strip();
        if (limpio.codePointCount(0, limpio.length()) > max || limpio.chars()
                .anyMatch(c -> (c < 32 && c != '\n' && c != '\r' && c != '\t') || c == 127)) {
            invalido(campo);
        }
        return limpio;
    }

    private static void invalido(String campo) {
        throw new DatoSanitarioInvalidoException(campo);
    }
}
