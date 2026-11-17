package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.ganadoconecta.modulos.identidad.aplicacion.CerrarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.IniciarSesion;
import pe.ganadoconecta.modulos.identidad.aplicacion.RenovarSesion;

/** Adaptador HTTP AU02–AU04. El token de renovación solo viaja en cookie. */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("postgres")
public class AutenticacionControlador {
    static final String COOKIE_RENOVACION = "__Host-GC-Refresh";

    private final IniciarSesion iniciar;
    private final RenovarSesion renovar;
    private final CerrarSesion cerrar;
    private final Clock reloj;

    public AutenticacionControlador(IniciarSesion iniciar, RenovarSesion renovar,
            CerrarSesion cerrar, Clock reloj) {
        this.iniciar = iniciar;
        this.renovar = renovar;
        this.cerrar = cerrar;
        this.reloj = reloj;
    }

    @PostMapping("/login")
    ResponseEntity<LoginRespuesta> login(@RequestBody LoginSolicitud solicitud,
            @CookieValue(value = COOKIE_RENOVACION, required = false) String cookieAnterior,
            HttpServletRequest request) {
        IniciarSesion.Resultado resultado = iniciar.ejecutar(new IniciarSesion.Comando(
                solicitud.correo(), solicitud.contrasena(), request.getRemoteAddr(), cookieAnterior));
        LoginRespuesta respuesta = new LoginRespuesta(
                resultado.usuarioId(), resultado.correo(), "ACTIVO",
                resultado.tokenAcceso().valor(), resultado.tokenAcceso().expiraEn());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(
                        resultado.tokenRenovacion(), resultado.sesionVenceEn()).toString())
                .body(respuesta);
    }

    @PostMapping("/renovar")
    ResponseEntity<TokenAccesoRespuesta> renovar(
            @CookieValue(value = COOKIE_RENOVACION, required = false) String tokenRenovacion) {
        RenovarSesion.Resultado resultado = renovar.ejecutar(tokenRenovacion);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(
                        resultado.tokenRenovacion(), resultado.sesionVenceEn()).toString())
                .body(new TokenAccesoRespuesta(
                        resultado.tokenAcceso().valor(), resultado.tokenAcceso().expiraEn()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(value = COOKIE_RENOVACION, required = false) String tokenRenovacion) {
        cerrar.ejecutar(tokenRenovacion);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, eliminarCookie().toString())
                .build();
    }

    @GetMapping("/me")
    ResponseEntity<SesionRespuesta> me(
            @AuthenticationPrincipal IdentidadAutenticada identidad) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new SesionRespuesta(identidad.usuarioId(), "ACTIVO"));
    }

    private ResponseCookie cookie(String valor, Instant sesionVenceEn) {
        long segundos = Math.max(0, Duration.between(
                reloj.instant(), sesionVenceEn).toSeconds());
        return ResponseCookie.from(COOKIE_RENOVACION, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(segundos))
                .build();
    }

    private ResponseCookie eliminarCookie() {
        return ResponseCookie.from(COOKIE_RENOVACION, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    record LoginSolicitud(String correo, String contrasena) {
        @Override
        public String toString() {
            return "LoginSolicitud[datos=PROTEGIDOS]";
        }
    }

    record LoginRespuesta(UUID usuarioId, String correo, String estado,
            String accessToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "LoginRespuesta[usuarioId=" + usuarioId + ", credenciales=PROTEGIDAS]";
        }
    }

    record TokenAccesoRespuesta(String accessToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "TokenAccesoRespuesta[credenciales=PROTEGIDAS]";
        }
    }

    record SesionRespuesta(UUID usuarioId, String estado) {
    }
}
