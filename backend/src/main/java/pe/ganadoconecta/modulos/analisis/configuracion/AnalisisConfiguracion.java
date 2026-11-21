package pe.ganadoconecta.modulos.analisis.configuracion;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAnalisisGanadero;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConsultarAgendaSanitaria;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
class AnalisisConfiguracion {
    @Bean ConsultarAnalisisGanadero consultarAnalisisGanadero(ConsultarReportesGanaderos reportes,
            ConsultarAgendaSanitaria agenda, Clock reloj) {
        return new ConsultarAnalisisGanadero(reportes, agenda, reloj);
    }
}
