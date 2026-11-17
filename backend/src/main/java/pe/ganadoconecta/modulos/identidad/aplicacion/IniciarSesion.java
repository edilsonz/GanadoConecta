package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import pe.ganadoconecta.modulos.identidad.aplicacion.GeneradorCredencialesRenovacion.CredencialRenovacion;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.DatoRegistroInvalidoException;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioAutenticable;

/** Núcleo AU02: verifica credenciales y confirma sesión/renovación antes de emitir JWT. */
public final class IniciarSesion {
    public static final Duration DURACION_SESION = Duration.ofHours(24);
    public static final Duration DURACION_TOKEN_ACCESO = Duration.ofMinutes(10);

    private final RepositorioCredenciales credenciales;
    private final CodificadorContrasena codificador;
    private final RepositorioSesiones sesiones;
    private final GeneradorCredencialesRenovacion renovaciones;
    private final TokensAcceso tokens;
    private final LimiteIntentosLogin limite;
    private final Supplier<UUID> ids;
    private final Clock reloj;
    private final String hashSimulado;

    public IniciarSesion(RepositorioCredenciales credenciales, CodificadorContrasena codificador,
            RepositorioSesiones sesiones, GeneradorCredencialesRenovacion renovaciones,
            TokensAcceso tokens, LimiteIntentosLogin limite, Supplier<UUID> ids,
            Clock reloj, String hashSimulado) {
        this.credenciales = Objects.requireNonNull(credenciales);
        this.codificador = Objects.requireNonNull(codificador);
        this.sesiones = Objects.requireNonNull(sesiones);
        this.renovaciones = Objects.requireNonNull(renovaciones);
        this.tokens = Objects.requireNonNull(tokens);
        this.limite = Objects.requireNonNull(limite);
        this.ids = Objects.requireNonNull(ids);
        this.reloj = Objects.requireNonNull(reloj);
        this.hashSimulado = Objects.requireNonNull(hashSimulado);
    }

    public Resultado ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        Instant ahora = reloj.instant();
        String claveCorreo = claveSegura(comando.correo());
        String origen = comando.origen() == null ? "" : comando.origen();
        limite.verificarPermitido(claveCorreo, origen, ahora);

        Optional<UsuarioAutenticable> encontrado = buscar(comando.correo());
        String passwordHash = encontrado.map(UsuarioAutenticable::passwordHash)
                .orElse(hashSimulado);
        String contrasena = comando.contrasena() == null ? "" : comando.contrasena();
        boolean coincide = codificador.coincide(contrasena, passwordHash);
        UsuarioAutenticable usuario = encontrado.orElse(null);

        if (usuario == null || !usuario.activo() || !coincide) {
            limite.registrarFallo(claveCorreo, origen, ahora);
            throw new CredencialesInvalidasException();
        }

        if (comando.tokenRenovacionAnterior() != null
                && !comando.tokenRenovacionAnterior().isBlank()) {
            sesiones.revocarPorHuella(
                    renovaciones.calcularHuella(comando.tokenRenovacionAnterior()), ahora);
        }

        UUID sesionId = ids.get();
        UUID verificadorId = ids.get();
        Instant venceEn = ahora.plus(DURACION_SESION);
        CredencialRenovacion renovacion = renovaciones.generar();
        sesiones.crear(new RepositorioSesiones.NuevaSesion(
                sesionId, usuario.id(), ahora, venceEn, verificadorId,
                renovacion.huellaSha256()));
        TokensAcceso.TokenEmitido acceso = tokens.emitir(
                usuario.id(), sesionId, ahora, ahora.plus(DURACION_TOKEN_ACCESO));
        return new Resultado(usuario.id(), usuario.correo().valor(), sesionId,
                acceso, renovacion.valor(), venceEn);
    }

    private Optional<UsuarioAutenticable> buscar(String correoEntrada) {
        try {
            return credenciales.buscarPorCorreo(CorreoElectronico.desde(correoEntrada));
        } catch (DatoRegistroInvalidoException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private String claveSegura(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    public record Comando(String correo, String contrasena, String origen,
            String tokenRenovacionAnterior) {
        public Comando(String correo, String contrasena, String origen) {
            this(correo, contrasena, origen, null);
        }

        @Override
        public String toString() {
            return "ComandoLogin[datos=PROTEGIDOS]";
        }
    }

    public record Resultado(UUID usuarioId, String correo, UUID sesionId,
            TokensAcceso.TokenEmitido tokenAcceso, String tokenRenovacion,
            Instant sesionVenceEn) {
        @Override
        public String toString() {
            return "ResultadoLogin[usuarioId=" + usuarioId + ", sesionId=" + sesionId
                    + ", credenciales=PROTEGIDAS]";
        }
    }
}
