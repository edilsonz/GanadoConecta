package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.util.UUID;

public interface CalculadorHuellaOperacion {
    byte[] calcular(SolicitudRegistroAnimal solicitud, UUID actorUsuarioId);
}
