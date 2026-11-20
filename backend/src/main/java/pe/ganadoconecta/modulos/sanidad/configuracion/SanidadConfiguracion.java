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
import pe.ganadoconecta.modulos.sanidad.adaptadores.salida.RepositorioEventosSanitariosPostgres;
import pe.ganadoconecta.modulos.sanidad.adaptadores.salida.RepositorioAgendaSanitariaPostgres;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarHistorialSanitario;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RegistrarEventoSanitario;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RepositorioEventosSanitarios;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarVacunaciones;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RegistrarVacunacion;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RepositorioVacunaciones;
import pe.ganadoconecta.modulos.sanidad.aplicacion.RepositorioAgendaSanitaria;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ProgramarActividadSanitaria;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConsultarAgendaSanitaria;
import pe.ganadoconecta.modulos.sanidad.aplicacion.CompletarActividadSanitaria;

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

    @Bean RepositorioEventosSanitarios repositorioEventosSanitarios(JdbcTemplate jdbc){
        return new RepositorioEventosSanitariosPostgres(jdbc);
    }
    @Bean RegistrarEventoSanitario registrarEventoSanitario(UnidadDeTrabajo unidad,
            AutorizarAccesoGanaderiaTransaccional autorizador,
            RepositorioEventosSanitarios repositorio,Clock reloj){
        return new RegistrarEventoSanitario(unidad,autorizador,repositorio,reloj);
    }
    @Bean ListarHistorialSanitario listarHistorialSanitario(
            AutorizarLecturaGanaderia autorizador,RepositorioEventosSanitarios repositorio){
        return new ListarHistorialSanitario(autorizador,repositorio);
    }
    @Bean RepositorioAgendaSanitaria repositorioAgendaSanitaria(JdbcTemplate jdbc){return new RepositorioAgendaSanitariaPostgres(jdbc);}
    @Bean ProgramarActividadSanitaria programarActividadSanitaria(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioAgendaSanitaria r,Clock c){return new ProgramarActividadSanitaria(u,a,r,c);}
    @Bean ConsultarAgendaSanitaria consultarAgendaSanitaria(AutorizarLecturaGanaderia a,RepositorioAgendaSanitaria r){return new ConsultarAgendaSanitaria(a,r);}
    @Bean CompletarActividadSanitaria completarActividadSanitaria(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioAgendaSanitaria r,Clock c){return new CompletarActividadSanitaria(u,a,r,c);}
}
