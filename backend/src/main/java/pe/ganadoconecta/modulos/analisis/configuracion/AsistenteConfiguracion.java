package pe.ganadoconecta.modulos.analisis.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pe.ganadoconecta.modulos.analisis.adaptadores.salida.MotorAsistenteLocal;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAnalisisGanadero;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente.Motor;
import pe.ganadoconecta.modulos.analisis.aplicacion.FuentesAsistente;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultarFichaAnimal;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioSesiones;
import pe.ganadoconecta.modulos.reproduccion.aplicacion.ListarEventosReproductivos;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ConsultarAgendaSanitaria;
import pe.ganadoconecta.modulos.sanidad.aplicacion.ListarHistorialSanitario;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
class AsistenteConfiguracion {
    @Bean Motor motorAsistente(ObjectMapper json, @Value("${GANADOCONECTA_IA_ACTIVA:false}") boolean activa,
            @Value("${GANADOCONECTA_IA_PUERTO:18081}") int puerto,
            @Value("${GANADOCONECTA_IA_SECRETO:}") String secreto) {
        if (!activa) return (pregunta, hechos) -> { throw new ConsultarAsistente.Indisponible(); };
        return new MotorAsistenteLocal(puerto, secreto, json);
    }

    @Bean ConsultarAsistente consultarAsistente(Motor motor, RepositorioSesiones sesiones, Clock reloj,
            AutorizarLecturaGanaderia permisos, ConsultarFichaAnimal ficha, ConsultarAnalisisGanadero analisis,
            ListarHistorialSanitario sanidad, ListarEventosReproductivos reproduccion, ConsultarAgendaSanitaria agenda) {
        var fuentes = new FuentesAsistente(permisos, ficha, analisis, sanidad, reproduccion, agenda);
        return new ConsultarAsistente(fuentes, motor, sesiones, reloj);
    }
}
