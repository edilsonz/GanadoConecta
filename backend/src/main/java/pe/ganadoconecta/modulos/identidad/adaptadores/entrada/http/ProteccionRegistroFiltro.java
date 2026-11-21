package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** Origen y tamaño para escrituras públicas/privadas; limita el registro antes de Argon2id. */
public final class ProteccionRegistroFiltro extends OncePerRequestFilter {
    static final int MAXIMO_CUERPO_BYTES = 16 * 1024;
    private static final String RUTA = "/api/v1/auth/registro";
    private static final java.util.Set<String> RUTAS_PROTEGIDAS = java.util.Set.of(
            RUTA, "/api/v1/auth/login", "/api/v1/auth/renovar", "/api/v1/auth/logout",
            "/api/v1/ganaderias");

    private final String origenPermitido;
    private final LimitadorRegistro limitador;
    private final ObjectMapper objectMapper;

    public ProteccionRegistroFiltro(String origenPermitido, int maxIntentos,
            Duration ventana, int maxOrigenes, ObjectMapper objectMapper) {
        this(origenPermitido, new LimitadorRegistro(
                maxIntentos, ventana, maxOrigenes, Clock.systemUTC()), objectMapper);
    }

    ProteccionRegistroFiltro(String origenPermitido, LimitadorRegistro limitador,
            ObjectMapper objectMapper) {
        this.origenPermitido = quitarBarraFinal(origenPermitido);
        this.limitador = limitador;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean postProtegido = "POST".equals(request.getMethod())
                && RUTAS_PROTEGIDAS.contains(request.getRequestURI());
        boolean altaAnimalProtegida = "POST".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales$");
        boolean revocacionProtegida = "DELETE".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/membresias/[^/]+$");
        boolean vacunacionProtegida = "POST".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales/[^/]+/vacunaciones$");
        boolean eventoSanitarioProtegido = "POST".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales/[^/]+/eventos-sanitarios$");
        boolean eventoReproductivoProtegido = "POST".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales/[^/]+/eventos-reproductivos$");
        boolean partoProtegido = "POST".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales/[^/]+/partos$");
        boolean genealogiaProtegida = "PUT".equals(request.getMethod())
                && request.getRequestURI().matches(
                        "^/api/v1/ganaderias/[^/]+/animales/[^/]+/genealogia/(MADRE|PADRE)$");
        boolean agendaProtegida = "POST".equals(request.getMethod())
                && request.getRequestURI().matches("^/api/v1/ganaderias/[^/]+/agenda-sanitaria(?:/[^/]+/completar)?$");
        String ruta = request.getRequestURI();
        boolean familiaProtegida = switch (request.getMethod()) {
            case "POST" -> ruta.equals("/api/v1/invitaciones/aceptar")
                    || ruta.matches("^/api/v1/ganaderias/[^/]+/familia/invitaciones$");
            case "DELETE" -> ruta.matches("^/api/v1/ganaderias/[^/]+/familia/invitaciones/[^/]+$");
            case "PUT" -> ruta.matches("^/api/v1/ganaderias/[^/]+/familia/integrantes/[^/]+$");
            default -> false;
        };
        boolean pesoProtegido = "POST".equals(request.getMethod())
                && ruta.matches("^/api/v1/ganaderias/[^/]+/animales/[^/]+/pesos$");
        boolean anuncioProtegido = ("POST".equals(request.getMethod())
                    && ruta.matches("^/api/v1/ganaderias/[^/]+/animales/[^/]+/anuncios(?:/[^/]+/(?:estado|fotos))?$"))
                || ("PUT".equals(request.getMethod())
                    && ruta.matches("^/api/v1/ganaderias/[^/]+/animales/[^/]+/anuncios/[^/]+$"));
        return !anuncioProtegido && !pesoProtegido && !familiaProtegida && !postProtegido && !altaAnimalProtegida && !revocacionProtegida
                && !vacunacionProtegida && !eventoSanitarioProtegido
                && !eventoReproductivoProtegido && !partoProtegido
                && !genealogiaProtegida && !agendaProtegida;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!origenValido(request)) {
            escribirError(response, 403, "ORIGEN_NO_PERMITIDO",
                    "El origen de la solicitud no está permitido.", Map.of());
            return;
        }

        if (request.getRequestURI().matches("^/api/v1/ganaderias/[^/]+/animales/[^/]+/anuncios/[^/]+/fotos$")) {
            // El controlador aplica el límite binario después de autenticación y CSRF.
            filterChain.doFilter(request, response);
            return;
        }
        byte[] cuerpo = request.getInputStream().readNBytes(MAXIMO_CUERPO_BYTES + 1);
        if (cuerpo.length > MAXIMO_CUERPO_BYTES) {
            escribirError(response, 413, "CUERPO_DEMASIADO_GRANDE",
                    "La solicitud supera el tamaño permitido.", Map.of());
            java.util.Arrays.fill(cuerpo, (byte) 0);
            return;
        }

        if (RUTA.equals(request.getRequestURI())) {
            LimitadorRegistro.Resultado limite = limitador.intentar(request.getRemoteAddr());
            if (!limite.permitido()) {
                response.setHeader("Retry-After", Long.toString(limite.reintentarEnSegundos()));
                escribirError(response, 429, "DEMASIADOS_INTENTOS",
                        "Espere antes de volver a intentar el registro.", Map.of());
                java.util.Arrays.fill(cuerpo, (byte) 0);
                return;
            }
        }

        try {
            filterChain.doFilter(new SolicitudConCuerpo(request, cuerpo), response);
        } finally {
            java.util.Arrays.fill(cuerpo, (byte) 0);
        }
    }

    private boolean origenValido(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            return origenPermitido.equals(quitarBarraFinal(origin));
        }
        String referer = request.getHeader("Referer");
        return referer != null && (referer.equals(origenPermitido)
                || referer.startsWith(origenPermitido + "/"));
    }

    private void escribirError(HttpServletResponse response, int estado, String codigo,
            String mensaje, Map<String, String> errores) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), new RespuestaError(
                codigo, mensaje, errores, UUID.randomUUID().toString()));
    }

    private static String quitarBarraFinal(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El origen permitido es obligatorio");
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    private static final class SolicitudConCuerpo extends HttpServletRequestWrapper {
        private final byte[] cuerpo;

        private SolicitudConCuerpo(HttpServletRequest request, byte[] cuerpo) {
            super(request);
            this.cuerpo = cuerpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Lectura asíncrona no soportada");
                }

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return cuerpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return cuerpo.length;
        }
    }
}
