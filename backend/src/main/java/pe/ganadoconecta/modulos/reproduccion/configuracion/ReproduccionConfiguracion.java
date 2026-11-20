package pe.ganadoconecta.modulos.reproduccion.configuracion;
import java.time.Clock;import org.springframework.context.annotation.*;import org.springframework.jdbc.core.JdbcTemplate;import pe.ganadoconecta.modulos.ganado.aplicacion.*;import pe.ganadoconecta.modulos.identidad.aplicacion.AutorizarAccesoGanaderiaTransaccional;import pe.ganadoconecta.modulos.reproduccion.adaptadores.salida.RepositorioReproduccionPostgres;import pe.ganadoconecta.modulos.reproduccion.aplicacion.*;
@Configuration(proxyBeanMethods=false) @Profile("postgres") public class ReproduccionConfiguracion{
 @Bean RepositorioReproduccion repositorioReproduccion(JdbcTemplate j){return new RepositorioReproduccionPostgres(j);}
 @Bean RegistrarEventoReproductivo registrarEventoReproductivo(UnidadDeTrabajo u,AutorizarAccesoGanaderiaTransaccional a,RepositorioReproduccion r,Clock c){return new RegistrarEventoReproductivo(u,a,r,c);}
 @Bean ListarEventosReproductivos listarEventosReproductivos(AutorizarLecturaGanaderia a,RepositorioReproduccion r){return new ListarEventosReproductivos(a,r);}
}
