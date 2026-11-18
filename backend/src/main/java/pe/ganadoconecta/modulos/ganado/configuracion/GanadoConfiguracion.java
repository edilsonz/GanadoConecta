package pe.ganadoconecta.modulos.ganado.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.ganado.adaptadores.salida.RepositorioAnimalesPostgres;
import pe.ganadoconecta.modulos.ganado.adaptadores.salida.UnidadDeTrabajoPostgres;
import pe.ganadoconecta.modulos.ganado.aplicacion.CalculadorHuellaOperacion;
import pe.ganadoconecta.modulos.ganado.aplicacion.CanonizadorAltaAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.RepositorioAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class GanadoConfiguracion {
    @Bean
    RepositorioAnimales repositorioAnimales(JdbcTemplate jdbc) {
        return new RepositorioAnimalesPostgres(jdbc);
    }

    @Bean
    UnidadDeTrabajo unidadDeTrabajoGanado(TransactionTemplate transacciones) {
        return new UnidadDeTrabajoPostgres(transacciones);
    }

    @Bean
    CalculadorHuellaOperacion calculadorHuellaOperacion() {
        return new CanonizadorAltaAnimal();
    }

    @Bean
    RegistrarAnimal registrarAnimal(UnidadDeTrabajo unidadDeTrabajo,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioAnimales repositorio,
            CalculadorHuellaOperacion calculadorHuella,
            Clock reloj) {
        return new RegistrarAnimal(unidadDeTrabajo, autorizador, repositorio,
                calculadorHuella, reloj);
    }
}
