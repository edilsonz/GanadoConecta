package pe.ganadoconecta.modulos.identidad.dominio;

/** Contraseña preservada exactamente; AU01 no autoriza trim ni reglas de composición. */
public record ContrasenaRegistro(String valor) {
    public static final int MINIMO_CARACTERES = 15;
    public static final int MAXIMO_CARACTERES = 128;

    public static ContrasenaRegistro desde(String contrasena) {
        if (contrasena == null) {
            throw new DatoRegistroInvalidoException(
                    "contrasena", "La contraseña es obligatoria.");
        }
        int longitud = contrasena.codePointCount(0, contrasena.length());
        if (longitud < MINIMO_CARACTERES || longitud > MAXIMO_CARACTERES) {
            throw new DatoRegistroInvalidoException("contrasena",
                    "La contraseña debe tener entre 15 y 128 caracteres.");
        }
        return new ContrasenaRegistro(contrasena);
    }
}
