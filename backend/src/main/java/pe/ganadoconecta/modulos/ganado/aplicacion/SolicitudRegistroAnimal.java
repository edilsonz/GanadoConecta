package pe.ganadoconecta.modulos.ganado.aplicacion;

import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.OperacionId;

public record SolicitudRegistroAnimal(
        GanaderiaId ganaderiaId,
        AnimalId animalId,
        OperacionId operacionId,
        String tipo,
        int schemaVersion,
        long baseVersion,
        DatosAnimal datos) {

    public SolicitudRegistroAnimal {
        if (!"CREAR_ANIMAL".equals(tipo)) {
            throw new ContratoOperacionInvalidoException("tipo");
        }
        if (schemaVersion != 1) {
            throw new ContratoOperacionInvalidoException("schemaVersion");
        }
        if (baseVersion != 0) {
            throw new ContratoOperacionInvalidoException("baseVersion");
        }
        if (animalId != null && operacionId != null
                && animalId.valor().equals(operacionId.valor())) {
            throw new ContratoOperacionInvalidoException("operationId");
        }
        java.util.Objects.requireNonNull(ganaderiaId);
        java.util.Objects.requireNonNull(animalId);
        java.util.Objects.requireNonNull(operacionId);
        java.util.Objects.requireNonNull(datos);
    }
}
