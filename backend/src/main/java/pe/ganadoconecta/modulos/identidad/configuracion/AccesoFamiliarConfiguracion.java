package pe.ganadoconecta.modulos.identidad.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.AccesoFamiliarPostgres;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoFamiliar;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class AccesoFamiliarConfiguracion {
    @Bean AccesoFamiliar accesoFamiliar(UnidadDeTrabajo unidad,
            AutorizarAccesoGanaderiaTransaccional autorizar, JdbcTemplate jdbc, Clock reloj) {
        return new AccesoFamiliar(unidad, autorizar, new AccesoFamiliarPostgres(jdbc), reloj);
    }
}
