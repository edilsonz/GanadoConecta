package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.UUID;

@FunctionalInterface
public interface AutorizarLecturaGanaderia {
    void autorizar(UUID actorUsuarioId, UUID ganaderiaId);
}
