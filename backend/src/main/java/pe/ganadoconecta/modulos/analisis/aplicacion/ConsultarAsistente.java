package pe.ganadoconecta.modulos.analisis.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioSesiones;
import pe.ganadoconecta.modulos.identidad.aplicacion.SesionNoValidaException;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.Periodo;

public final class ConsultarAsistente {
    public enum Tema { PESO, SANIDAD, REPRODUCCION, AGENDA }
    public enum Tipo { ULTIMO_PESAJE, PAREJA_PESAJES, VARIACION, SANIDAD, REPRODUCCION, AGENDA }
    public enum Estado { RESPUESTA, INSUFICIENTE }
    public enum Estilo { REGISTROS, DIRECTO }
    public record Solicitud(String pregunta, Tema tema, LocalDate desde, LocalDate hasta, UUID animalId) {
        @Override public String toString() { return "Solicitud[datos protegidos]"; }
    }
    public record Fuente(UUID registroId, String tipo, LocalDate fecha) {}
    public record Evidencia(Tipo tipo, String texto, List<Fuente> fuentes) {}
    public record Hecho(String id, Tipo tipo, String texto) {}
    public record Seleccion(Estado estado, Estilo estilo, List<String> referencias) {}
    public record Respuesta(Estado estado, String texto, List<Fuente> fuentes) {}

    public interface Datos {
        void autorizar(UUID actor, UUID ganaderia, UUID animal);
        List<Evidencia> recuperar(UUID actor, UUID ganaderia, UUID animal, Tema tema, Periodo periodo);
    }

    public interface Motor {
        Seleccion inferir(String pregunta, List<Hecho> hechos);
    }

    public static final class Indisponible extends RuntimeException {}
    public static final class Ocupado extends RuntimeException {}

    private static final Respuesta INSUFICIENTE = new Respuesta(Estado.INSUFICIENTE,
            "No hay información suficiente para responder con los registros seleccionados.", List.of());
    private final Datos datos;
    private final Motor motor;
    private final RepositorioSesiones sesiones;
    private final Clock reloj;
    private final Semaphore concurrencia = new Semaphore(1);

    public ConsultarAsistente(Datos datos, Motor motor, RepositorioSesiones sesiones, Clock reloj) {
        this.datos = datos;
        this.motor = motor;
        this.sesiones = sesiones;
        this.reloj = reloj;
    }

    public Respuesta ejecutar(UUID actor, UUID sesion, UUID ganaderia, Solicitud solicitud) {
        String pregunta = validar(solicitud);
        UUID animal = solicitud.animalId();
        var periodo = new Periodo(solicitud.desde(), solicitud.hasta());
        verificarAcceso(actor, sesion, ganaderia, animal);
        if (!concurrencia.tryAcquire()) throw new Ocupado();
        try {
            var evidencias = datos.recuperar(actor, ganaderia, animal, solicitud.tema(), periodo);
            if (evidencias.isEmpty()) return INSUFICIENTE;
            var porReferencia = new LinkedHashMap<String, Evidencia>();
            for (Evidencia evidencia : evidencias) porReferencia.put("H" + (porReferencia.size() + 1), evidencia);
            var hechos = porReferencia.entrySet().stream()
                    .map(item -> new Hecho(item.getKey(), item.getValue().tipo(), item.getValue().texto())).toList();
            return fundamentar(motor.inferir(pregunta, hechos), porReferencia);
        } finally {
            concurrencia.release();
            verificarAcceso(actor, sesion, ganaderia, animal);
        }
    }

    private void verificarAcceso(UUID actor, UUID sesion, UUID ganaderia, UUID animal) {
        if (!sesiones.estaVigente(sesion, actor, reloj.instant())) throw new SesionNoValidaException();
        datos.autorizar(actor, ganaderia, animal);
    }

    private static String validar(Solicitud entrada) {
        if (entrada == null || entrada.tema() == null || entrada.pregunta() == null
                || (entrada.tema() != Tema.AGENDA && entrada.animalId() == null)
                || (entrada.animalId() != null && (entrada.animalId().version() != 4 || entrada.animalId().variant() != 2))) {
            throw new IllegalArgumentException();
        }
        String pregunta = entrada.pregunta().strip();
        if (pregunta.isBlank() || pregunta.codePointCount(0, pregunta.length()) > 400
                || pregunta.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException();
        return pregunta;
    }

    private static Respuesta fundamentar(Seleccion seleccion, LinkedHashMap<String, Evidencia> hechos) {
        if (seleccion == null || seleccion.estado() == null || seleccion.estilo() == null
                || seleccion.referencias() == null) throw new Indisponible();
        var ids = seleccion.referencias();
        if (ids.size() > hechos.size() || ids.stream().distinct().count() != ids.size()
                || ids.stream().anyMatch(id -> !hechos.containsKey(id))
                || ids.isEmpty() != (seleccion.estado() == Estado.INSUFICIENTE)) throw new Indisponible();
        if (ids.isEmpty()) return INSUFICIENTE;
        var elegidos = ids.stream().map(hechos::get).toList();
        var frases = elegidos.stream().map(Evidencia::texto).toList();
        var fuentes = elegidos.stream().flatMap(e -> e.fuentes().stream()).distinct().toList();
        String inicio = seleccion.estilo() == Estilo.REGISTROS ? "Según los registros confirmados: " : "";
        return new Respuesta(Estado.RESPUESTA, inicio + String.join(" ", frases), fuentes);
    }
}
