package pe.ganadoconecta.modulos.analisis.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultarFichaAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Periodo;
import pe.ganadoconecta.modulos.reproduccion.aplicacion.ListarEventosReproductivos;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConsultarAgendaSanitaria;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarHistorialSanitario;
import pe.ganadoconecta.modulos.sanidad.dominio.EstadoActividadSanitaria;
import static pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.*;

public final class FuentesAsistente implements Datos {
    private static final int LIMITE = 8;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private final AutorizarLecturaGanaderia permisos;
    private final ConsultarFichaAnimal ficha;
    private final ConsultarAnalisisGanadero analisis;
    private final ListarHistorialSanitario sanidad;
    private final ListarEventosReproductivos reproduccion;
    private final ConsultarAgendaSanitaria agenda;

    public FuentesAsistente(AutorizarLecturaGanaderia permisos, ConsultarFichaAnimal ficha,
            ConsultarAnalisisGanadero analisis, ListarHistorialSanitario sanidad,
            ListarEventosReproductivos reproduccion, ConsultarAgendaSanitaria agenda) {
        this.permisos = permisos;
        this.ficha = ficha;
        this.analisis = analisis;
        this.sanidad = sanidad;
        this.reproduccion = reproduccion;
        this.agenda = agenda;
    }

    @Override public void autorizar(UUID actor, UUID ganaderia, UUID animal) {
        if (animal == null) {
            permisos.autorizar(actor, ganaderia);
            return;
        }
        try {
            ficha.ejecutar(actor, new GanaderiaId(ganaderia), new AnimalId(animal));
        } catch (AnimalNoEncontradoException e) {
            throw new AccesoGanaderiaDenegadoException();
        }
    }

    @Override public List<Evidencia> recuperar(UUID actor, UUID g, UUID a, Tema tema, Periodo periodo) {
        return switch (tema) {
            case PESO -> pesos(actor, g, a, periodo);
            case SANIDAD -> sanidad.ejecutar(actor, g, a, null).stream()
                    .filter(e -> enPeriodo(e.fecha(), periodo)).limit(LIMITE)
                    .map(e -> evento(Tipo.SANIDAD, e.id(), e.fecha(), "Evento sanitario", etiqueta(e.tipo()))).toList();
            case REPRODUCCION -> reproduccion.ejecutar(actor, g, a).stream()
                    .filter(e -> enPeriodo(e.fecha(), periodo)).limit(LIMITE)
                    .map(e -> evento(Tipo.REPRODUCCION, e.id(), e.fecha(), "Evento reproductivo",
                            etiqueta(e.tipo()) + (e.estadoGestacion() == null ? "" : " (" + etiqueta(e.estadoGestacion()) + ")"))).toList();
            case AGENDA -> agenda.ejecutar(actor, g, a, null, EstadoActividadSanitaria.PENDIENTE).stream()
                    .filter(e -> enPeriodo(e.fechaPrevista(), periodo)).limit(LIMITE)
                    .map(e -> evento(Tipo.AGENDA, e.id(), e.fechaPrevista(), "Actividad pendiente", etiqueta(e.tipo()))).toList();
        };
    }

    private List<Evidencia> pesos(UUID actor, UUID g, UUID a, Periodo periodo) {
        var mediciones = analisis.analizar(actor, g, a, periodo).pesajes();
        if (mediciones.isEmpty()) return List.of();
        var reciente = mediciones.getLast();
        String detalleUltimo = kilos(reciente.kilos()) + " kg";
        if (mediciones.size() == 1) detalleUltimo += " (único pesaje del período)";
        var ultimo = evento(Tipo.ULTIMO_PESAJE, reciente.id(), reciente.fecha(), "Pesaje", detalleUltimo);
        if (mediciones.size() == 1) return List.of(ultimo);

        var anterior = mediciones.getFirst();
        var fuentes = List.of(new Fuente(anterior.id(), "Pesaje", anterior.fecha()), ultimo.fuentes().getFirst());
        var pareja = new Evidencia(Tipo.PAREJA_PESAJES, "Últimos dos pesajes registrados: "
                + kilos(anterior.kilos()) + " kg el " + FECHA.format(anterior.fecha()) + " y "
                + kilos(reciente.kilos()) + " kg el " + FECHA.format(reciente.fecha()) + ".", fuentes);
        BigDecimal delta = reciente.kilos().subtract(anterior.kilos());
        String signo = delta.signum() > 0 ? "+" : "";
        var variacion = new Evidencia(Tipo.VARIACION, "Variación registrada: " + signo + kilos(delta) + " kg del "
                + FECHA.format(anterior.fecha()) + " al " + FECHA.format(reciente.fecha()) + ".", fuentes);
        return List.of(ultimo, pareja, variacion);
    }

    private static Evidencia evento(Tipo tipo, UUID id, LocalDate fecha, String origen, String dato) {
        return new Evidencia(tipo, origen + " del " + FECHA.format(fecha) + ": " + dato + ".",
                List.of(new Fuente(id, origen, fecha)));
    }

    private static boolean enPeriodo(LocalDate fecha, Periodo periodo) {
        return !fecha.isBefore(periodo.desde()) && !fecha.isAfter(periodo.hasta());
    }

    private static String kilos(BigDecimal kilos) {
        return kilos.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String etiqueta(Enum<?> valor) {
        return switch (valor.name()) {
            case "VACUNACION" -> "vacunación";
            case "DESPARASITACION" -> "desparasitación";
            case "BANO_ANTIPARASITARIO" -> "baño antiparasitario";
            case "REVISION_VETERINARIA" -> "revisión veterinaria";
            case "INSEMINACION" -> "inseminación";
            case "EVALUACION_GESTACION" -> "evaluación de gestación";
            case "CAMPANA_VACUNACION" -> "campaña de vacunación";
            case "CAMPANA_DESPARASITACION" -> "campaña de desparasitación";
            case "CAMPANA_BANO" -> "campaña de baño";
            default -> valor.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
