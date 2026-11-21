package pe.ganadoconecta.modulos.identidad.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.AdministracionPostgres;
import pe.ganadoconecta.modulos.identidad.aplicacion.ConsultarAdministracion;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
class AdministracionConfiguracion {
    @Bean ConsultarAdministracion consultarAdministracion(JdbcTemplate jdbc, UnidadDeTrabajo unidad, Clock reloj) {
        return new ConsultarAdministracion(new AdministracionPostgres(jdbc), unidad, reloj);
    }
}
