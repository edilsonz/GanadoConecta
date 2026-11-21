package pe.ganadoconecta.modulos.sanidad.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConfirmacionActividad(List<UUID> atendidos, LocalDate fechaAtencion, String nombreVacuna) {
    public ConfirmacionActividad {
        atendidos = ActividadSanitaria.seleccionar(atendidos, "atendidos");
        nombreVacuna = nombreVacuna == null || nombreVacuna.isBlank() ? null : nombreVacuna.strip();
    }
}
