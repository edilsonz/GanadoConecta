package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;

public final class AccesoFamiliar {
    public enum Rol { EDITOR, LECTOR }
    public record Integrante(UUID usuarioId, String correo, String rol, String estado) {}
    public record Invitacion(UUID id, String correo, String rol, String estado, Instant venceEn) {}
    public record Resumen(List<Integrante> integrantes, List<Invitacion> invitaciones) {}
    public record Emitida(String codigo) {
        @Override public String toString() { return "Emitida[PROTEGIDA]"; }
    }
    public static final class NoDisponible extends RuntimeException {}
    public interface Repositorio {
        Resumen consultar(UUID ganaderia, Instant ahora);
        void emitir(UUID ganaderia, String correo, Rol rol, byte[] huella, Instant ahora, Instant vence);
        boolean cancelar(UUID ganaderia, UUID invitacion, Instant ahora);
        boolean cambiarRol(UUID ganaderia, UUID usuario, Rol rol, UUID actor, Instant ahora);
        UUID aceptar(UUID usuario, UUID sesion, byte[] huella, Clock reloj);
    }

    private final UnidadDeTrabajo unidad;
    private final AutorizarAccesoGanaderiaTransaccional autorizar;
    private final Repositorio repositorio;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public AccesoFamiliar(UnidadDeTrabajo unidad, AutorizarAccesoGanaderiaTransaccional autorizar,
            Repositorio repositorio, Clock reloj) {
        this.unidad = unidad;
        this.autorizar = autorizar;
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public Resumen consultar(UUID actor, UUID sesion, UUID ganaderia) {
        return administrar(actor, sesion, ganaderia, ahora -> repositorio.consultar(ganaderia, ahora));
    }

    public Emitida emitir(UUID actor, UUID sesion, UUID ganaderia, String correo, Rol rol) {
        String destinatario = CorreoElectronico.desde(correo).valor();
        exigirRol(rol);
        return administrar(actor, sesion, ganaderia, ahora -> {
            byte[] bytes = new byte[32];
            aleatorio.nextBytes(bytes);
            String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            repositorio.emitir(ganaderia, destinatario, rol, huella(codigo), ahora, ahora.plus(48, ChronoUnit.HOURS));
            return new Emitida(codigo);
        });
    }

    public void cancelar(UUID actor, UUID sesion, UUID ganaderia, UUID invitacion) {
        administrar(actor, sesion, ganaderia, ahora -> {
            if (!repositorio.cancelar(ganaderia, invitacion, ahora)) throw new NoDisponible();
            return true;
        });
    }

    public void cambiarRol(UUID actor, UUID sesion, UUID ganaderia, UUID usuario, Rol rol) {
        exigirRol(rol);
        administrar(actor, sesion, ganaderia, ahora -> {
            if (!repositorio.cambiarRol(ganaderia, usuario, rol, actor, ahora)) throw new NoDisponible();
            return true;
        });
    }

    public UUID aceptar(UUID usuario, UUID sesion, String codigo) {
        if (codigo == null || !codigo.matches("[A-Za-z0-9_-]{43}")) throw new NoDisponible();
        return unidad.ejecutar(() -> {
            UUID ganaderia = repositorio.aceptar(usuario, sesion, huella(codigo), reloj);
            if (ganaderia == null) throw new NoDisponible();
            return ganaderia;
        });
    }

    private <T> T administrar(UUID actor, UUID sesion, UUID ganaderia, Function<Instant,T> trabajo) {
        return unidad.ejecutar(() -> {
            autorizar.autorizar(actor, sesion, ganaderia, PermisoGanaderia.ADMINISTRAR, reloj.instant());
            return trabajo.apply(reloj.instant());
        });
    }

    private static void exigirRol(Rol rol) {
        if (rol == null) throw new IllegalArgumentException("Rol requerido");
    }

    private static byte[] huella(String codigo) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
