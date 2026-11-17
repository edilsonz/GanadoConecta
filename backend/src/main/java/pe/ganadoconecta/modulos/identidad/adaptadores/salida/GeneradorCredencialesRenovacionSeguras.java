package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorCredencialesRenovacion;

/** Token opaco de 256 bits; solo su huella SHA-256 se entrega a persistencia. */
public final class GeneradorCredencialesRenovacionSeguras
        implements GeneradorCredencialesRenovacion {
    private static final int BYTES_TOKEN = 32;
    private final SecureRandom random;

    public GeneradorCredencialesRenovacionSeguras() {
        this(new SecureRandom());
    }

    GeneradorCredencialesRenovacionSeguras(SecureRandom random) {
        this.random = random;
    }

    @Override
    public CredencialRenovacion generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        java.util.Arrays.fill(bytes, (byte) 0);
        return new CredencialRenovacion(valor, calcularHuella(valor));
    }

    @Override
    public String calcularHuella(String valor) {
        try {
            byte[] bytes = valor.getBytes(StandardCharsets.US_ASCII);
            byte[] huella = MessageDigest.getInstance("SHA-256").digest(bytes);
            java.util.Arrays.fill(bytes, (byte) 0);
            String hexadecimal = HexFormat.of().formatHex(huella);
            java.util.Arrays.fill(huella, (byte) 0);
            return hexadecimal;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
