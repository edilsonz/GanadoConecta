package pe.ganadoconecta.modulos.identidad.configuracion;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.CodificadorContrasenaArgon2;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioUsuariosPostgres;
import pe.ganadoconecta.modulos.identidad.aplicacion.CodificadorContrasena;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorIdUsuario;
import pe.ganadoconecta.modulos.identidad.aplicacion.RegistrarUsuario;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioUsuarios;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class IdentidadConfiguracion {
    @Bean
    RepositorioUsuarios repositorioUsuarios(JdbcTemplate jdbc) {
        return new RepositorioUsuariosPostgres(jdbc);
    }

    @Bean
    CodificadorContrasena codificadorContrasena() {
        return new CodificadorContrasenaArgon2();
    }

    @Bean
    GeneradorIdUsuario generadorIdUsuario() {
        return UUID::randomUUID;
    }

    @Bean
    RegistrarUsuario registrarUsuario(RepositorioUsuarios repositorio,
            CodificadorContrasena codificador, GeneradorIdUsuario generadorId) {
        return new RegistrarUsuario(repositorio, codificador, generadorId);
    }
}
