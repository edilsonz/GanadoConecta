package pe.ganadoconecta.modulos.identidad.configuracion;

import java.util.UUID;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.CodificadorContrasenaArgon2;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.GeneradorCredencialesRenovacionSeguras;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.LimiteIntentosLoginPostgres;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioCredencialesPostgres;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioGanaderiasPostgres;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioSesionesPostgres;
import pe.ganadoconecta.modulos.identidad.adaptadores.salida.RepositorioUsuariosPostgres;
import pe.ganadoconecta.modulos.identidad.aplicacion.CodificadorContrasena;
import pe.ganadoconecta.modulos.identidad.aplicacion.CerrarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.CrearGanaderia;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorCredencialesRenovacion;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorIdUsuario;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorIdMembresia;
import pe.ganadoconecta.modulos.identidad.aplicacion.LimiteIntentosLogin;
import pe.ganadoconecta.modulos.identidad.aplicacion.IniciarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.RenovarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.RegistrarUsuario;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioCredenciales;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioGanaderias;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioSesiones;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioUsuarios;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;
import pe.ganadoconecta.modulos.identidad.aplicacion.ValidarTokenAcceso;

@Configuration(proxyBeanMethods = false)
@Profile("postgres")
public class IdentidadConfiguracion {
    @Bean
    RepositorioUsuarios repositorioUsuarios(JdbcTemplate jdbc) {
        return new RepositorioUsuariosPostgres(jdbc);
    }

    @Bean
    RepositorioCredenciales repositorioCredenciales(JdbcTemplate jdbc) {
        return new RepositorioCredencialesPostgres(jdbc);
    }

    @Bean
    RepositorioGanaderias repositorioGanaderias(
            JdbcTemplate jdbc, TransactionTemplate transacciones) {
        return new RepositorioGanaderiasPostgres(jdbc, transacciones);
    }

    @Bean
    RepositorioSesiones repositorioSesiones(
            JdbcTemplate jdbc, TransactionTemplate transacciones) {
        return new RepositorioSesionesPostgres(jdbc, transacciones);
    }

    @Bean
    GeneradorCredencialesRenovacion generadorCredencialesRenovacion() {
        return new GeneradorCredencialesRenovacionSeguras();
    }

    @Bean
    LimiteIntentosLogin limiteIntentosLogin(JdbcTemplate jdbc,
            TransactionTemplate transacciones) {
        return new LimiteIntentosLoginPostgres(
                jdbc, transacciones, 10, java.time.Duration.ofMinutes(15));
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
    GeneradorIdMembresia generadorIdMembresia() {
        return UUID::randomUUID;
    }

    @Bean
    RegistrarUsuario registrarUsuario(RepositorioUsuarios repositorio,
            CodificadorContrasena codificador, GeneradorIdUsuario generadorId) {
        return new RegistrarUsuario(repositorio, codificador, generadorId);
    }

    @Bean
    CrearGanaderia crearGanaderia(RepositorioGanaderias repositorio,
            GeneradorIdMembresia generadorId) {
        return new CrearGanaderia(repositorio, generadorId);
    }

    @Bean
    IniciarSesion iniciarSesion(RepositorioCredenciales credenciales,
            CodificadorContrasena codificador, RepositorioSesiones sesiones,
            GeneradorCredencialesRenovacion renovaciones, TokensAcceso tokens,
            LimiteIntentosLogin limite, Clock reloj) {
        String hashSimulado = codificador.codificar(
                "verificacion-sintetica-de-coste-equivalente");
        return new IniciarSesion(credenciales, codificador, sesiones, renovaciones,
                tokens, limite, UUID::randomUUID, reloj, hashSimulado);
    }

    @Bean
    RenovarSesion renovarSesion(RepositorioSesiones sesiones,
            GeneradorCredencialesRenovacion renovaciones, TokensAcceso tokens,
            Clock reloj) {
        return new RenovarSesion(sesiones, renovaciones, tokens, UUID::randomUUID, reloj);
    }

    @Bean
    CerrarSesion cerrarSesion(RepositorioSesiones sesiones,
            GeneradorCredencialesRenovacion renovaciones, Clock reloj) {
        return new CerrarSesion(sesiones, renovaciones, reloj);
    }

    @Bean
    ValidarTokenAcceso validarTokenAcceso(
            TokensAcceso tokens, RepositorioSesiones sesiones, Clock reloj) {
        return new ValidarTokenAcceso(tokens, sesiones, reloj);
    }
}
