package pe.ganadoconecta.modulos.analisis.adaptadores.salida;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.*;

public final class MotorAsistenteLocal implements Motor {
    private record Entrada(String pregunta, List<Hecho> hechos) {
        @Override public String toString() { return "Entrada[datos protegidos]"; }
    }

    private final HttpClient cliente = HttpClient.newBuilder().proxy(ProxySelector.of(null))
            .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build();
    private final URI uri;
    private final String secreto;
    private final ObjectMapper json;

    public MotorAsistenteLocal(int puerto, String secreto, ObjectMapper json) {
        if (puerto < 1 || puerto > 65535 || !secreto.matches("[A-Za-z0-9_-]{32,128}")) {
            throw new IllegalArgumentException("Configuración interna del asistente inválida");
        }
        this.uri = URI.create("http://127.0.0.1:" + puerto + "/interno/v1/inferir");
        this.secreto = secreto;
        this.json = json;
    }

    @Override public Seleccion inferir(String pregunta, List<Hecho> hechos) {
        try {
            var solicitud = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-GC-IA-Interno", secreto)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(new Entrada(pregunta, hechos))))
                    .build();
            var respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.statusCode() == 429) throw new Ocupado();
            if (respuesta.statusCode() != 200 || respuesta.body().length > 8192) throw new Indisponible();
            return json.readerFor(Seleccion.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Indisponible();
        } catch (IOException e) {
            throw new Indisponible();
        }
    }
}
