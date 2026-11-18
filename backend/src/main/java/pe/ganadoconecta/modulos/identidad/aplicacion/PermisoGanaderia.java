package pe.ganadoconecta.modulos.identidad.aplicacion;

/** Permisos mínimos reutilizables para recursos privados de una ganadería. */
public enum PermisoGanaderia {
    LEER,
    ESCRIBIR,
    ADMINISTRAR;

    public boolean permite(String rol) {
        return switch (this) {
            case LEER -> "PROPIETARIO".equals(rol)
                    || "EDITOR".equals(rol)
                    || "LECTOR".equals(rol);
            case ESCRIBIR -> "PROPIETARIO".equals(rol) || "EDITOR".equals(rol);
            case ADMINISTRAR -> "PROPIETARIO".equals(rol);
        };
    }
}
