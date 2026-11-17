package pe.ganadoconecta.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.AutenticacionJwtFiltro;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.ProteccionRegistroFiltro;

/** Mantiene cerrada la API salvo salud y el incremento explícito de registro. */
@Configuration(proxyBeanMethods = false)
public class SeguridadConfiguracion {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
            ProteccionRegistroFiltro proteccionRegistro,
            ObjectProvider<AutenticacionJwtFiltro> autenticacionJwt,
            ObjectMapper objectMapper) throws Exception {
        CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();
        csrfRepository.setCookieName("__Host-GC-CSRF");
        csrfRepository.setHeaderName("X-CSRF-TOKEN");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict"));

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");
        AccessDeniedHandler deniedHandler = (request, response, exception) -> {
            boolean csrf = exception instanceof MissingCsrfTokenException
                    || exception instanceof InvalidCsrfTokenException;
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    csrf ? "CSRF_INVALIDO" : "ACCESO_DENEGADO",
                    csrf ? "La protección de la solicitud no es válida."
                            : "No tiene acceso a este recurso.",
                    objectMapper);
        };

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/renovar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ganaderias").authenticated()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(deniedHandler)
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "AUTENTICACION_REQUERIDA",
                                "Se requiere una sesión válida.",
                                objectMapper)))
                .addFilterBefore(proteccionRegistro, CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        autenticacionJwt.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        return http.build();
    }

    @Bean
    ProteccionRegistroFiltro proteccionRegistroFiltro(ObjectMapper objectMapper,
            @Value("${ganadoconecta.seguridad.origen-permitido}") String origenPermitido,
            @Value("${ganadoconecta.seguridad.registro.max-intentos}") int maxIntentos,
            @Value("${ganadoconecta.seguridad.registro.ventana}") java.time.Duration ventana,
            @Value("${ganadoconecta.seguridad.registro.max-origenes}") int maxOrigenes) {
        return new ProteccionRegistroFiltro(
                origenPermitido, maxIntentos, ventana, maxOrigenes, objectMapper);
    }

    private static void writeError(HttpServletResponse response, int status, String code,
            String message, ObjectMapper objectMapper) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "codigo", code,
                "mensajeSeguro", message,
                "correlacionId", UUID.randomUUID().toString()));
    }
}
