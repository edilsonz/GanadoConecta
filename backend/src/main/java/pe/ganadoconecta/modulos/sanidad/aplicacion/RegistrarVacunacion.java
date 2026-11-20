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
import pe.ganadoconecta.modulos.sanidad.dominio.Vacunacion;

public final class RegistrarVacunacion {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final AutorizarAccesoGanaderiaTransaccional autorizador;
    private final RepositorioVacunaciones repositorio;
    private final Clock reloj;

    public RegistrarVacunacion(UnidadDeTrabajo unidadDeTrabajo,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioVacunaciones repositorio, Clock reloj) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.autorizador = Objects.requireNonNull(autorizador);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public ResultadoVacunacion ejecutar(SolicitudVacunacion solicitud,
            UUID actorUsuarioId, UUID actorSesionId) {
        Objects.requireNonNull(solicitud);
        if (solicitud.operationId() == null || solicitud.operationId().version() != 4) {
            throw new DatoSanitarioInvalidoException("operationId");
        }
        if (solicitud.schemaVersion() != 1) {
            throw new DatoSanitarioInvalidoException("schemaVersion");
        }
        var ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        var vacunacion = new Vacunacion(solicitud.eventId(), solicitud.ganaderiaId(),
                solicitud.animalId(), solicitud.nombreVacuna(), solicitud.fechaAplicacion(),
                solicitud.responsable(), solicitud.lote(), solicitud.observaciones(),
                solicitud.proximaFecha(), actorUsuarioId, ahora);
        vacunacion.validarFechaNoFutura(ahora.atZone(LIMA).toLocalDate());
        byte[] huella = huella(solicitud, actorUsuarioId);
        return unidadDeTrabajo.ejecutar(() -> {
            autorizador.autorizar(actorUsuarioId, actorSesionId,
                    solicitud.ganaderiaId(), PermisoGanaderia.ESCRIBIR, ahora);
            return repositorio.registrar(vacunacion, solicitud.operationId(), huella,
                    solicitud.schemaVersion());
        });
    }

    private static byte[] huella(SolicitudVacunacion s, UUID actor) {
        String canonico = String.join("\u001f", actor.toString(), s.ganaderiaId().toString(),
                s.animalId().toString(), s.eventId().toString(),
                s.nombreVacuna() == null ? "" : s.nombreVacuna().strip(),
                String.valueOf(s.fechaAplicacion()),
                s.responsable() == null ? "" : s.responsable().strip(),
                s.lote() == null ? "" : s.lote().strip(),
                s.observaciones() == null ? "" : s.observaciones().strip(),
                String.valueOf(s.proximaFecha()), String.valueOf(s.schemaVersion()));
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(canonico.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
