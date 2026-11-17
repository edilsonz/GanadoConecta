package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.util.Objects;

/** La firma no basta: cada uso consulta el estado vigente de sesión y cuenta. */
public final class ValidarTokenAcceso {
    private final TokensAcceso tokens;
    private final RepositorioSesiones sesiones;
    private final Clock reloj;

    public ValidarTokenAcceso(TokensAcceso tokens, RepositorioSesiones sesiones, Clock reloj) {
        this.tokens = Objects.requireNonNull(tokens);
        this.sesiones = Objects.requireNonNull(sesiones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public TokensAcceso.IdentidadToken ejecutar(String token) {
        TokensAcceso.IdentidadToken identidad = tokens.verificar(token);
        if (!sesiones.estaVigente(
                identidad.sesionId(), identidad.usuarioId(), reloj.instant())) {
            throw new SesionNoValidaException();
        }
        return identidad;
    }
}
