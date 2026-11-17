package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodificadorContrasenaArgon2Test {
    @Test
    void generaSaltYHashArgon2idVerificableConParametrosFijados() {
        CodificadorContrasenaArgon2 codificador = new CodificadorContrasenaArgon2();
        String contrasena = "contraseña-sintética-de-prueba";

        long inicio = System.nanoTime();
        String primerHash = codificador.codificar(contrasena);
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        String segundoHash = codificador.codificar(contrasena);

        assertThat(primerHash)
                .startsWith("$argon2id$")
                .contains("m=65536,t=3,p=1")
                .doesNotContain(contrasena);
        assertThat(segundoHash).isNotEqualTo(primerHash);
        assertThat(codificador.coincide(contrasena, primerHash)).isTrue();
        assertThat(codificador.coincide("otra-contraseña-sintética", primerHash)).isFalse();
        System.out.printf("ARGON2ID_MEDICION encode_ms=%d memoria_kib=65536 iteraciones=3 paralelismo=1%n",
                milisegundos);
    }
}
