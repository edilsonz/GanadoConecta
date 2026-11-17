package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Instant;

public interface LimiteIntentosLogin {
    void verificarPermitido(String claveCorreo, String origen, Instant ahora);

    void registrarFallo(String claveCorreo, String origen, Instant ahora);
}
