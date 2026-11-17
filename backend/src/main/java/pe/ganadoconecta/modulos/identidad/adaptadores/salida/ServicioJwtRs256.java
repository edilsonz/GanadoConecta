package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokenAccesoInvalidoException;
import pe.ganadoconecta.modulos.identidad.aplicacion.TokensAcceso;

/** JWT RS256 estricto. Las claves se reciben externamente y nunca se generan aquí. */
public final class ServicioJwtRs256 implements TokensAcceso {
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final String emisor;
    private final String audiencia;
    private final Clock reloj;

    public ServicioJwtRs256(RSAPublicKey publica, RSAPrivateKey privada,
            String emisor, String audiencia, Clock reloj) {
        Objects.requireNonNull(publica);
        Objects.requireNonNull(privada);
        this.emisor = textoObligatorio(emisor, "emisor");
        this.audiencia = textoObligatorio(audiencia, "audiencia");
        this.reloj = Objects.requireNonNull(reloj);

        RSAKey rsa = new RSAKey.Builder(publica).privateKey(privada).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsa)));
        this.decoder = NimbusJwtDecoder.withPublicKey(publica)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        JwtTimestampValidator tiempos = new JwtTimestampValidator(Duration.ZERO);
        tiempos.setClock(reloj);
        OAuth2TokenValidator<Jwt> validadorAudiencia = jwt ->
                jwt.getAudience().contains(this.audiencia)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token", "Audiencia no válida", null));
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                tiempos, new JwtIssuerValidator(this.emisor), validadorAudiencia));
    }

    @Override
    public TokenEmitido emitir(UUID usuarioId, UUID sesionId,
            Instant emitidoEn, Instant expiraEn) {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(sesionId);
        if (expiraEn == null || emitidoEn == null || !expiraEn.isAfter(emitidoEn)) {
            throw new IllegalArgumentException("La vigencia del JWT no es válida");
        }
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .audience(java.util.List.of(audiencia))
                .subject(usuarioId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .claim("sid", sesionId.toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String valor = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEn);
    }

    @Override
    public IdentidadToken verificar(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            Instant emitidoEn = Objects.requireNonNull(jwt.getIssuedAt());
            Instant expiraEn = Objects.requireNonNull(jwt.getExpiresAt());
            if (emitidoEn.isAfter(reloj.instant()) || !expiraEn.isAfter(emitidoEn)) {
                throw new TokenAccesoInvalidoException();
            }
            return new IdentidadToken(
                    UUID.fromString(jwt.getSubject()),
                    UUID.fromString(jwt.getClaimAsString("sid")),
                    UUID.fromString(jwt.getClaimAsString(JwtClaimNames.JTI)),
                    emitidoEn,
                    expiraEn);
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new TokenAccesoInvalidoException(exception);
        }
    }

    private static String textoObligatorio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombre + " es obligatorio");
        }
        return valor;
    }
}
