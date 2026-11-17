package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeneradorCredencialesRenovacionSegurasTest {
    @Test
    void generaValoresDeDoscientosCincuentaYSeisBitsConHuellasNoReversibles() {
        GeneradorCredencialesRenovacionSeguras generador =
                new GeneradorCredencialesRenovacionSeguras();

        var primero = generador.generar();
        var segundo = generador.generar();

        assertThat(java.util.Base64.getUrlDecoder().decode(primero.valor())).hasSize(32);
        assertThat(primero.valor()).isNotEqualTo(segundo.valor());
        assertThat(primero.huellaSha256()).hasSize(64)
                .isEqualTo(generador.calcularHuella(primero.valor()))
                .doesNotContain(primero.valor());
        assertThat(primero.toString()).doesNotContain(primero.valor(), primero.huellaSha256());
    }
}
