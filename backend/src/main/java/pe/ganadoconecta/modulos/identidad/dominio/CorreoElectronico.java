package pe.ganadoconecta.modulos.identidad.dominio;

import java.util.Locale;

/** Correo normalizado según AU01; no implica que el correo haya sido verificado. */
public record CorreoElectronico(String valor) {
    private static final int MAXIMO_CARACTERES = 254;

    public static CorreoElectronico desde(String correo) {
        if (correo == null) {
            throw new DatoRegistroInvalidoException("correo", "El correo es obligatorio.");
        }

        String normalizado = correo.strip().toLowerCase(Locale.ROOT);
        int longitud = normalizado.codePointCount(0, normalizado.length());
        int primeraArroba = normalizado.indexOf('@');
        boolean formatoBasicoValido = primeraArroba > 0
                && primeraArroba == normalizado.lastIndexOf('@')
                && primeraArroba < normalizado.length() - 1;
        boolean contieneCaracterNoPermitido = normalizado.codePoints()
                .anyMatch(codePoint -> Character.isWhitespace(codePoint)
                        || Character.isSpaceChar(codePoint)
                        || Character.isISOControl(codePoint));

        if (longitud < 3 || longitud > MAXIMO_CARACTERES
                || !formatoBasicoValido || contieneCaracterNoPermitido) {
            throw new DatoRegistroInvalidoException(
                    "correo", "Ingrese un correo electrónico válido de hasta 254 caracteres.");
        }
        return new CorreoElectronico(normalizado);
    }
}
