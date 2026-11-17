package pe.ganadoconecta.modulos.identidad.configuracion;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.CargadorClavesJwt;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.ServicioJwtRs256;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

/** Configura JWT solo con claves RSA externas; no existe clave predeterminada. */
@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class JwtConfiguracion {
    @Bean
    Clock relojUtc() {
        return Clock.systemUTC();
    }

    @Bean
    TokensAcceso tokensAcceso(
            @Value("${ganadoconecta.seguridad.jwt.clave-publica}") String rutaPublica,
            @Value("${ganadoconecta.seguridad.jwt.clave-privada}") String rutaPrivada,
            @Value("${ganadoconecta.seguridad.jwt.emisor}") String emisor,
            @Value("${ganadoconecta.seguridad.jwt.audiencia}") String audiencia,
            Clock reloj) {
        CargadorClavesJwt.Claves claves = CargadorClavesJwt.cargar(
                Path.of(rutaPublica), Path.of(rutaPrivada));
        return new ServicioJwtRs256(
                claves.publica(), claves.privada(), emisor, audiencia, reloj);
    }
}
