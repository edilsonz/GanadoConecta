package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.ganadoconecta.modulos.identidad.aplicacion.SesionNoValidaException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokenAccesoInvalidoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;
import pe.ganadoconecta.modulos.identidad.aplicacion.ValidarTokenAcceso;

/** Autentica Bearer JWT y vuelve a comprobar sesión/cuenta en PostgreSQL. */
@Component
@Profile("postgres")
public final class AutenticacionJwtFiltro extends OncePerRequestFilter {
    private static final String PREFIJO = "Bearer ";

    private final ValidarTokenAcceso validar;
    private final ObjectMapper objectMapper;

    public AutenticacionJwtFiltro(ValidarTokenAcceso validar, ObjectMapper objectMapper) {
        this.validar = validar;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!cabecera.startsWith(PREFIJO) || cabecera.length() == PREFIJO.length()) {
            rechazar(response);
            return;
        }
        try {
            TokensAcceso.IdentidadToken token = validar.ejecutar(
                    cabecera.substring(PREFIJO.length()));
            IdentidadAutenticada principal = new IdentidadAutenticada(
                    token.usuarioId(), token.sesionId());
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, java.util.List.of());
            autenticacion.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            filterChain.doFilter(request, response);
        } catch (TokenAccesoInvalidoException | SesionNoValidaException exception) {
            SecurityContextHolder.clearContext();
            rechazar(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "SESION_NO_VALIDA", "La sesión no es válida.");
        } catch (PersistenciaIdentidadException exception) {
            SecurityContextHolder.clearContext();
            rechazar(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AUTENTICACION_NO_DISPONIBLE",
                    "No fue posible comprobar la sesión.");
        }
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        rechazar(response, HttpServletResponse.SC_UNAUTHORIZED,
                "SESION_NO_VALIDA", "La sesión no es válida.");
    }

    private void rechazar(HttpServletResponse response, int estado,
            String codigo, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "codigo", codigo,
                "mensajeSeguro", mensaje,
                "correlacionId", UUID.randomUUID().toString()));
    }
}
