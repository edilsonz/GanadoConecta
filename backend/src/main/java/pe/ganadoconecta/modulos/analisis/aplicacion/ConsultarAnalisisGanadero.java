package pe.ganadoconecta.modulos.analisis.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Medicion;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Periodo;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Reproduccion;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Sanidad;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConsultarAgendaSanitaria;
import pe.ganadoconecta.modulos.sanidad.dominio.EstadoActividadSanitaria;
import pe.ganadoconecta.modulos.sanidad.dominio.TipoActividadSanitaria;

public final class ConsultarAnalisisGanadero {
    public record Analisis(Periodo periodo, Sanidad sanidad, Reproduccion reproduccion, List<Medicion> pesajes) {}
    public record Aviso(UUID actividadId, TipoActividadSanitaria tipo, String descripcion, LocalDate fechaPrevista,
            String estado) {}
    public record Alertas(LocalDate fechaCorte, List<Aviso> avisos) {}

    private final ConsultarReportesGanaderos reportes;
    private final ConsultarAgendaSanitaria agenda;
    private final Clock relojLima;

    public ConsultarAnalisisGanadero(ConsultarReportesGanaderos reportes, ConsultarAgendaSanitaria agenda, Clock reloj) {
        this.reportes = reportes;
        this.agenda = agenda;
        this.relojLima = reloj.withZone(ZoneId.of("America/Lima"));
    }

    public Analisis analizar(UUID actor, UUID ganaderia, UUID animal, Periodo periodo) {
        var reporte = reportes.ejecutar(actor, ganaderia, animal, periodo);
        var mediciones = reporte.peso().mediciones();
        if (mediciones.size() < 2) return new Analisis(periodo, reporte.sanidad(), reporte.reproduccion(), mediciones);
        var anterior = mediciones.get(mediciones.size() - 2);
        var ultimas = List.of(new Medicion(anterior.id(), anterior.fecha(), anterior.kilos(), null), mediciones.getLast());
        return new Analisis(periodo, reporte.sanidad(), reporte.reproduccion(), ultimas);
    }

    public Alertas alertas(UUID actor, UUID ganaderia) {
        LocalDate hoy = LocalDate.now(relojLima);
        var avisos = agenda.ejecutar(actor, ganaderia, null, null, EstadoActividadSanitaria.PENDIENTE).stream()
                .filter(actividad -> !actividad.fechaPrevista().isAfter(hoy))
                .map(actividad -> new Aviso(actividad.id(), actividad.tipo(), actividad.descripcion(),
                        actividad.fechaPrevista(), actividad.estadoVisible(hoy)))
                .toList();
        return new Alertas(hoy, avisos);
    }
}
