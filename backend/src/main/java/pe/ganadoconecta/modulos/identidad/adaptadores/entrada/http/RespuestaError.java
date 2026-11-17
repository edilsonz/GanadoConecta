package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RespuestaError(
        String codigo,
        String mensajeSeguro,
        Map<String, String> erroresPorCampo,
        String correlacionId) {
}
