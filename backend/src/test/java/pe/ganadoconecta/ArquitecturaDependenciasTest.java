package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ArquitecturaDependenciasTest {
    private static final Pattern DEPENDENCIA = Pattern.compile(
            "^ +([\\w.$]+)\\s+->\\s+([\\w.$]+)\\s+.+$");
    private static final Pattern NUCLEO = Pattern.compile(
            "^pe\\.ganadoconecta\\.modulos\\..+\\.(dominio|aplicacion)\\..+$");
    private static final Pattern INFRAESTRUCTURA = Pattern.compile(
            "(\\.(adaptadores|configuracion|infraestructura)\\.|^(org\\.springframework|com\\.fasterxml"
            + "|jakarta\\.(servlet|persistence)|javax\\.sql|java\\.(sql|net\\.http))\\.)");

    @Test
    void elNucleoCompiladoNoDependeDeFrameworksNiAdaptadores() throws Exception {
        var clases = Path.of(GanadoConectaApplication.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        String jdeps = System.getProperty("os.name").startsWith("Windows") ? "jdeps.exe" : "jdeps";
        Process proceso = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", jdeps).toString(),
                "--ignore-missing-deps", "-verbose:class", clases.toString()).redirectErrorStream(true).start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(proceso.waitFor()).withFailMessage(salida).isZero();
        var violaciones = new ArrayList<String>();
        int verificadas = 0;
        for (String linea : salida.split("\\R")) {
            var dependencia = DEPENDENCIA.matcher(linea);
            if (!dependencia.matches()) continue;
            var origen = NUCLEO.matcher(dependencia.group(1));
            if (!origen.matches()) continue;
            verificadas++;
            String destino = dependencia.group(2);
            if (INFRAESTRUCTURA.matcher(destino).find()
                    || ("dominio".equals(origen.group(1)) && destino.contains(".aplicacion."))) {
                violaciones.add(dependencia.group(1) + " -> " + destino);
            }
        }
        assertThat(verificadas).isPositive();
        assertThat(violaciones).isEmpty();
    }
}
