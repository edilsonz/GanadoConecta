package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;
import pe.ganadoconecta.modulos.sanidad.dominio.DatoSanitarioInvalidoException;
import pe.ganadoconecta.modulos.sanidad.dominio.EventoSanitario;
import pe.ganadoconecta.modulos.sanidad.dominio.TipoEventoSanitario;

public final class RegistrarEventoSanitario {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final UnidadDeTrabajo unidad;
    private final AutorizarAccesoGanaderiaTransaccional autorizador;
    private final RepositorioEventosSanitarios repositorio;
    private final Clock reloj;

    public RegistrarEventoSanitario(UnidadDeTrabajo unidad,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioEventosSanitarios repositorio, Clock reloj) {
        this.unidad = Objects.requireNonNull(unidad);
        this.autorizador = Objects.requireNonNull(autorizador);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public ResultadoEventoSanitario ejecutar(SolicitudEventoSanitario s,
            UUID actor, UUID sesion) {
        Objects.requireNonNull(s);
        if (s.operationId() == null || s.operationId().version() != 4)
            throw new DatoSanitarioInvalidoException("operationId");
        if (s.schemaVersion() != 1) throw new DatoSanitarioInvalidoException("schemaVersion");
        if (s.tipo() == TipoEventoSanitario.VACUNACION)
            throw new DatoSanitarioInvalidoException("tipo");
        var ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        var evento = new EventoSanitario(s.eventId(), s.ganaderiaId(), s.animalId(),
                s.tipo(), s.fecha(), null, s.descripcion(), s.responsable(), null,
                s.producto(), s.indicaciones(), s.observaciones(), null, actor, ahora);
        evento.validarRealizadoNoFuturo(ahora.atZone(LIMA).toLocalDate());
        byte[] huella = huella(evento, s.operationId(), s.schemaVersion());
        return unidad.ejecutar(() -> {
            autorizador.autorizar(actor, sesion, s.ganaderiaId(),
                    PermisoGanaderia.ESCRIBIR, ahora);
            return repositorio.registrar(evento, s.operationId(), huella, s.schemaVersion());
        });
    }

    private static byte[] huella(EventoSanitario e, UUID operacion, int schema) {
        String texto = String.join("\u001f", e.creadoPor().toString(),
                e.ganaderiaId().toString(), e.animalId().toString(), e.id().toString(),
                operacion.toString(), e.tipo().name(), e.fecha().toString(),
                valor(e.descripcion()), valor(e.responsable()), valor(e.producto()),
                valor(e.indicaciones()), valor(e.observaciones()), String.valueOf(schema));
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static String valor(String texto) { return texto == null ? "" : texto; }
}
