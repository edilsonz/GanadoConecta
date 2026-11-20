package pe.ganadoconecta.modulos.sanidad.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.sanidad.adaptadores.salida.RepositorioVacunacionesPostgres;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarVacunaciones;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RegistrarVacunacion;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RepositorioVacunaciones;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class SanidadConfiguracion {
    @Bean
    RepositorioVacunaciones repositorioVacunaciones(JdbcTemplate jdbc) {
        return new RepositorioVacunacionesPostgres(jdbc);
    }

    @Bean
    RegistrarVacunacion registrarVacunacion(UnidadDeTrabajo unidadDeTrabajo,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioVacunaciones repositorio, Clock reloj) {
        return new RegistrarVacunacion(unidadDeTrabajo, autorizador, repositorio, reloj);
    }

    @Bean
    ListarVacunaciones listarVacunaciones(AutorizarLecturaGanaderia autorizador,
            RepositorioVacunaciones repositorio) {
        return new ListarVacunaciones(autorizador, repositorio);
    }
}
