package pe.ganadoconecta;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Genera claves efímeras en target para contextos PostgreSQL; nunca se versionan. */
abstract class SoporteJwtPostgres {
    private static final Path PUBLICA;
    private static final Path PRIVADA;

    static {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair claves = generador.generateKeyPair();
            Path directorio = Files.createDirectories(Path.of("target", "jwt-test-efimero"));
            PUBLICA = directorio.resolve("publica.pem").toAbsolutePath();
            PRIVADA = directorio.resolve("privada.pem").toAbsolutePath();
            Files.writeString(PUBLICA, pem("PUBLIC KEY", claves.getPublic().getEncoded()),
                    StandardCharsets.US_ASCII);
            Files.writeString(PRIVADA, pem("PRIVATE KEY", claves.getPrivate().getEncoded()),
                    StandardCharsets.US_ASCII);
            PUBLICA.toFile().deleteOnExit();
            PRIVADA.toFile().deleteOnExit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void propiedadesJwt(DynamicPropertyRegistry propiedades) {
        propiedades.add("ganadoconecta.seguridad.jwt.clave-publica", PUBLICA::toString);
        propiedades.add("ganadoconecta.seguridad.jwt.clave-privada", PRIVADA::toString);
    }

    private static String pem(String tipo, byte[] contenido) {
        return "-----BEGIN " + tipo + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(contenido)
                + "\n-----END " + tipo + "-----\n";
    }
}
