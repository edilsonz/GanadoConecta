package pe.ganadoconecta.modulos.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoGanaderiaDenegadoException;

public final class ConsultarReportesGanaderos {
    public record Periodo(LocalDate desde, LocalDate hasta) {
        public Periodo {
            if (desde == null || hasta == null || desde.isAfter(hasta)) throw new IllegalArgumentException();
        }
    }

    public record EventosPorTipo(String tipo, long total) {}
    public record Sanidad(long total, List<EventosPorTipo> porTipo) {}
    public record Reproduccion(long montas, long inseminaciones, long evaluaciones, long confirmadas,
            long noConfirmadas, long indeterminadas, long partos, long crias) {}
    public record Medicion(UUID id, LocalDate fecha, BigDecimal kilos, BigDecimal variacion) {}
    public record Peso(long total, Medicion ultimo, List<Medicion> mediciones) {}
    public record Reporte(Sanidad sanidad, Reproduccion reproduccion, Peso peso) {}

    public interface Repositorio {
        Optional<Reporte> consultar(UUID ganaderia, UUID animal, Periodo periodo);
    }

    private final AutorizarLecturaGanaderia autorizar;
    private final Repositorio repositorio;

    public ConsultarReportesGanaderos(AutorizarLecturaGanaderia autorizar, Repositorio repositorio) {
        this.autorizar = autorizar;
        this.repositorio = repositorio;
    }

    public Reporte ejecutar(UUID actor, UUID ganaderia, UUID animal, Periodo periodo) {
        autorizar.autorizar(actor, ganaderia);
        return repositorio.consultar(ganaderia, animal, periodo).orElseThrow(AccesoGanaderiaDenegadoException::new);
    }
}
