package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Instant;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;

public record ResultadoRegistroAnimal(
        AnimalId animalId,
        OperacionId operacionId,
        long version,
        Instant confirmadoEn,
        EstadoResultado estado,
        boolean areteCoincidente) {

    public enum EstadoResultado {
        APLICADO,
        REPROCESADO
    }
}
