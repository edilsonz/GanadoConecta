package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.UUID;

@FunctionalInterface
public interface GeneradorIdMembresia {
    UUID generar();
}
