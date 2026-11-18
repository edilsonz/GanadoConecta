package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Instant;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaAccesible;

/**
 * Revalida y bloquea sesión, cuenta, ganadería y membresía dentro de la unidad
 * de trabajo que protege una escritura.
 */
public interface AutorizarAccesoGanaderiaTransaccional {
    GanaderiaAccesible autorizar(UUID actorUsuarioId, UUID actorSesionId,
            UUID ganaderiaId, PermisoGanaderia permiso, Instant ahora);
}
