package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entrega al cliente del mismo origen el token CSRF no autenticador. */
@RestController
@RequestMapping("/api/v1/auth")
public class CsrfControlador {

    @GetMapping("/csrf")
    ResponseEntity<CsrfRespuesta> obtener(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(new CsrfRespuesta(csrfToken.getToken()));
    }

    record CsrfRespuesta(String token) {
        @Override
        public String toString() {
            return "CsrfRespuesta[token=PROTEGIDO]";
        }
    }
}
