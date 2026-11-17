package pe.ganadoconecta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
@AutoConfigureMockMvc
class CsrfIntegrationTest {
    private static final String RUTA = "/api/v1/auth/csrf";

    @Autowired
    private MockMvc mvc;

    @Test
    void entregaTokenEnJsonYCookieSeguraSinCrearSesion() throws Exception {
        MvcResult resultado = mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        Cookie cookie = resultado.getResponse().getCookie("__Host-GC-CSRF");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    @Test
    void tokenNoConcedeAccesoAOtrasRutas() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACION_REQUERIDA"));
    }
}
