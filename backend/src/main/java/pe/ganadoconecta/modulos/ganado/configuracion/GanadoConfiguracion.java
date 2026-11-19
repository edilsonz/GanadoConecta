package pe.ganadoconecta.modulos.ganado.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.ganado.adaptadores.salida.ConsultasAnimalesPostgres;
import pe.ganadoconecta.modulos.ganado.adaptadores.salida.RepositorioAnimalesPostgres;
import pe.ganadoconecta.modulos.ganado.adaptadores.salida.UnidadDeTrabajoPostgres;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.ganado.aplicacion.CalculadorHuellaOperacion;
import pe.ganadoconecta.modulos.ganado.aplicacion.CanonizadorAltaAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultarFichaAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultasAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.ListarAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarAnimal;
import pe.ganadoconecta.modulos.ganado.aplicacion.RepositorioAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;
import pe.ganadoconecta.modulos.identidad.aplicacion.PermisoGanaderia;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class GanadoConfiguracion {
    @Bean
    RepositorioAnimales repositorioAnimales(JdbcTemplate jdbc) {
        return new RepositorioAnimalesPostgres(jdbc);
    }

    @Bean
    ConsultasAnimales consultasAnimales(JdbcTemplate jdbc) {
        return new ConsultasAnimalesPostgres(jdbc);
    }

    @Bean
    AutorizarLecturaGanaderia autorizarLecturaGanaderia(
            AutorizarAccesoGanaderia autorizador) {
        return (actor, ganaderia) ->
                autorizador.autorizar(actor, ganaderia, PermisoGanaderia.LEER);
    }

    @Bean
    ListarAnimales listarAnimales(AutorizarLecturaGanaderia autorizador,
            ConsultasAnimales consultas, Clock reloj) {
        return new ListarAnimales(autorizador, consultas, reloj);
    }

    @Bean
    ConsultarFichaAnimal consultarFichaAnimal(AutorizarLecturaGanaderia autorizador,
            ConsultasAnimales consultas) {
        return new ConsultarFichaAnimal(autorizador, consultas);
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
