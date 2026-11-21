package pe.ganadoconecta.modulos.reportes.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.reportes.adaptadores.salida.ReportesPostgres;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
class ReportesConfiguracion {
    @Bean ConsultarReportesGanaderos consultarReportesGanaderos(AutorizarLecturaGanaderia autorizar,
            JdbcTemplate jdbc, ObjectMapper json) {
        return new ConsultarReportesGanaderos(autorizar, new ReportesPostgres(jdbc, json));
    }
}
