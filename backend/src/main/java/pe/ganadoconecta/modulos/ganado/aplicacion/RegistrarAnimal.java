package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.EstadoAnimal;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;

public final class RegistrarAnimal {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    private final UnidadDeTrabajo unidadDeTrabajo;
    private final AutorizarAccesoGanaderiaTransaccional autorizador;
    private final RepositorioAnimales repositorio;
    private final CalculadorHuellaOperacion calculadorHuella;
    private final Clock reloj;

    public RegistrarAnimal(UnidadDeTrabajo unidadDeTrabajo,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioAnimales repositorio,
            CalculadorHuellaOperacion calculadorHuella,
            Clock reloj) {
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.autorizador = Objects.requireNonNull(autorizador);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.calculadorHuella = Objects.requireNonNull(calculadorHuella);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public ResultadoRegistroAnimal ejecutar(SolicitudRegistroAnimal solicitud,
            UUID actorUsuarioId, UUID actorSesionId) {
        Objects.requireNonNull(solicitud);
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(actorSesionId);
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        solicitud.datos().validarFechaNoFutura(ahora.atZone(ZONA_NEGOCIO).toLocalDate());
        byte[] huella = calculadorHuella.calcular(solicitud, actorUsuarioId);

        return unidadDeTrabajo.ejecutar(() -> {
            autorizador.autorizar(actorUsuarioId, actorSesionId,
                    solicitud.ganaderiaId().valor(), PermisoGanaderia.ESCRIBIR, ahora);
            Animal animal = new Animal(solicitud.animalId(), solicitud.ganaderiaId(),
                    solicitud.datos(), EstadoAnimal.ACTIVO, 1, actorUsuarioId, ahora);
            return repositorio.registrar(animal, solicitud.operacionId(),
                    actorUsuarioId, huella);
        });
    }
}
