package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Carga claves RSA externas: pública X.509 y privada PKCS#8, ambas en PEM. */
public final class CargadorClavesJwt {
    private CargadorClavesJwt() {
    }

    public static Claves cargar(Path rutaPublica, Path rutaPrivada) {
        try {
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            RSAPublicKey publica = (RSAPublicKey) rsa.generatePublic(new X509EncodedKeySpec(
                    contenidoPem(rutaPublica, "PUBLIC KEY")));
            RSAPrivateKey privada = (RSAPrivateKey) rsa.generatePrivate(new PKCS8EncodedKeySpec(
                    contenidoPem(rutaPrivada, "PRIVATE KEY")));
            if (publica.getModulus().bitLength() < 2048
                    || privada.getModulus().bitLength() < 2048
                    || !publica.getModulus().equals(privada.getModulus())) {
                throw new IllegalStateException("Las claves JWT RSA no son válidas");
            }
            return new Claves(publica, privada);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("No fue posible cargar las claves JWT externas", exception);
        }
    }

    private static byte[] contenidoPem(Path ruta, String tipo) throws IOException {
        if (ruta == null || !Files.isRegularFile(ruta)) {
            throw new IllegalStateException("La ruta de clave JWT no es un archivo regular");
        }
        String contenido = Files.readString(ruta, StandardCharsets.US_ASCII);
        String inicio = "-----BEGIN " + tipo + "-----";
        String fin = "-----END " + tipo + "-----";
        if (!contenido.contains(inicio) || !contenido.contains(fin)) {
            throw new IllegalStateException("Formato PEM JWT no admitido");
        }
        String base64 = contenido.replace(inicio, "").replace(fin, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    public record Claves(RSAPublicKey publica, RSAPrivateKey privada) {
    }
}
