package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorCredencialesRenovacion.CredencialRenovacion;

/** Rotación AU03. El token sucesor solo se entrega después del commit del repositorio. */
public final class RenovarSesion {
    private final RepositorioSesiones sesiones;
    private final GeneradorCredencialesRenovacion renovaciones;
    private final TokensAcceso tokens;
    private final Supplier<UUID> ids;
    private final Clock reloj;

    public RenovarSesion(RepositorioSesiones sesiones,
            GeneradorCredencialesRenovacion renovaciones, TokensAcceso tokens,
            Supplier<UUID> ids, Clock reloj) {
        this.sesiones = Objects.requireNonNull(sesiones);
        this.renovaciones = Objects.requireNonNull(renovaciones);
        this.tokens = Objects.requireNonNull(tokens);
        this.ids = Objects.requireNonNull(ids);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Resultado ejecutar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            throw new SesionNoValidaException();
        }
        Instant ahora = reloj.instant();
        CredencialRenovacion sucesor = renovaciones.generar();
        RepositorioSesiones.Rotacion rotacion = sesiones.rotar(
                renovaciones.calcularHuella(tokenRenovacion), ids.get(),
                sucesor.huellaSha256(), ahora);
        if (rotacion.estado() != RepositorioSesiones.Estado.VALIDA) {
            throw new SesionNoValidaException();
        }

        Instant expiraEn = ahora.plus(IniciarSesion.DURACION_TOKEN_ACCESO);
        if (expiraEn.isAfter(rotacion.venceEn())) {
            expiraEn = rotacion.venceEn();
        }
        TokensAcceso.TokenEmitido acceso = tokens.emitir(
                rotacion.usuarioId(), rotacion.sesionId(), ahora, expiraEn);
        return new Resultado(rotacion.usuarioId(), rotacion.sesionId(), acceso,
                sucesor.valor(), rotacion.venceEn());
    }

    public record Resultado(UUID usuarioId, UUID sesionId,
            TokensAcceso.TokenEmitido tokenAcceso, String tokenRenovacion,
            Instant sesionVenceEn) {
        @Override
        public String toString() {
            return "ResultadoRenovacion[usuarioId=" + usuarioId + ", sesionId=" + sesionId
                    + ", credenciales=PROTEGIDAS]";
        }
    }
}
