package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import pe.ganadoconecta.modulos.identidad.aplicacion.CodificadorContrasena;

/** Argon2id con parámetros fijados para este incremento y salt aleatorio de 16 bytes. */
public final class CodificadorContrasenaArgon2 implements CodificadorContrasena {
    public static final int SALT_BYTES = 16;
    public static final int HASH_BYTES = 32;
    public static final int PARALELISMO = 1;
    public static final int MEMORIA_KIB = 65_536;
    public static final int ITERACIONES = 3;

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
            SALT_BYTES, HASH_BYTES, PARALELISMO, MEMORIA_KIB, ITERACIONES);

    @Override
    public String codificar(String contrasena) {
        return encoder.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return encoder.matches(contrasena, hash);
    }
}
