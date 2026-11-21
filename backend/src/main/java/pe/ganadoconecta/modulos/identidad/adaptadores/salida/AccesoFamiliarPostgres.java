package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoFamiliar;
import pe.ganadoconecta.modulos.identidad.aplicacion.AccesoFamiliar.Rol;

public final class AccesoFamiliarPostgres implements AccesoFamiliar.Repositorio {
    private final JdbcTemplate jdbc;

    public AccesoFamiliarPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public AccesoFamiliar.Resumen consultar(UUID ganaderia, Instant ahora) {
        var integrantes = jdbc.query("""
                SELECT u.id, u.email_normalizado, m.rol, m.estado
                FROM identidad.membresia m JOIN identidad.usuario u ON u.id=m.usuario_id
                WHERE m.ganaderia_id=? ORDER BY u.email_normalizado
                """, (rs,n) -> new AccesoFamiliar.Integrante(rs.getObject(1, UUID.class),
                rs.getString(2), rs.getString(3), rs.getString(4)), ganaderia);
        var invitaciones = jdbc.query("""
                SELECT id, correo, rol, CASE WHEN estado='PENDIENTE' AND vence_en<=?
                    THEN 'EXPIRADA' ELSE estado END, vence_en
                FROM identidad.invitacion WHERE ganaderia_id=? ORDER BY creada_en DESC, id
                """, (rs,n) -> new AccesoFamiliar.Invitacion(rs.getObject(1, UUID.class),
                rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, OffsetDateTime.class).toInstant()), fecha(ahora), ganaderia);
        return new AccesoFamiliar.Resumen(integrantes, invitaciones);
    }

    @Override public void emitir(UUID ganaderia, String correo, Rol rol, byte[] huella,
            Instant ahora, Instant vence) {
        jdbc.update("""
                UPDATE identidad.invitacion SET estado='CANCELADA'
                WHERE ganaderia_id=? AND correo=? AND estado='PENDIENTE'
                """, ganaderia, correo);
        jdbc.update("""
                INSERT INTO identidad.invitacion
                    (id,ganaderia_id,correo,rol,huella,creada_en,vence_en,estado)
                VALUES (?,?,?,?,?,?,?,'PENDIENTE')
                """, UUID.randomUUID(), ganaderia, correo, rol.name(), huella, fecha(ahora), fecha(vence));
    }

    @Override public boolean cancelar(UUID ganaderia, UUID invitacion, Instant ahora) {
        return jdbc.update("""
                UPDATE identidad.invitacion SET estado='CANCELADA'
                WHERE ganaderia_id=? AND id=? AND estado='PENDIENTE' AND vence_en>?
                """, ganaderia, invitacion, fecha(ahora)) == 1;
    }

    @Override public boolean cambiarRol(UUID ganaderia, UUID usuario, Rol rol, UUID actor, Instant ahora) {
        actorAuditoria(jdbc, actor);
        return jdbc.update("""
                UPDATE identidad.membresia SET rol=?, actualizado_en=?
                WHERE ganaderia_id=? AND usuario_id=? AND estado='ACTIVA' AND rol<>'PROPIETARIO'
                """, rol.name(), fecha(ahora), ganaderia, usuario) == 1;
    }

    @Override public UUID aceptar(UUID usuario, UUID sesion, byte[] huella, Clock reloj) {
        // Mismo orden que AU07: sesión/cuenta, ganadería y finalmente membresía.
        var cuentas = jdbc.query("""
                SELECT u.email_normalizado, s.vence_en
                FROM identidad.sesion_dispositivo s JOIN identidad.usuario u ON u.id=s.usuario_id
                WHERE s.id=? AND u.id=? AND s.revocado_en IS NULL AND u.estado='ACTIVO'
                FOR UPDATE OF s,u
                """, (rs,n) -> new Cuenta(rs.getString(1), rs.getObject(2, OffsetDateTime.class)),
                sesion, usuario);
        if (cuentas.isEmpty()) return null;
        Cuenta cuenta = cuentas.getFirst();
        var ganaderias = jdbc.query("""
                SELECT g.id FROM identidad.ganaderia g
                JOIN identidad.invitacion i ON i.ganaderia_id=g.id
                WHERE i.huella=? AND i.correo=? AND g.estado='ACTIVA' FOR UPDATE OF g
                """, (rs,n) -> rs.getObject(1, UUID.class), huella, cuenta.correo());
        if (ganaderias.isEmpty()) return null;
        UUID ganaderia = ganaderias.getFirst();
        Instant ahora = reloj.instant();
        if (!cuenta.vence().toInstant().isAfter(ahora)) return null;
        var invitaciones = jdbc.query("""
                UPDATE identidad.invitacion SET estado='ACEPTADA'
                WHERE ganaderia_id=? AND huella=? AND correo=? AND estado='PENDIENTE' AND vence_en>?
                RETURNING rol, creada_en
                """, (rs,n) -> new Vigente(rs.getString(1),rs.getObject(2,OffsetDateTime.class)),
                ganaderia, huella, cuenta.correo(), fecha(ahora));
        if (invitaciones.isEmpty()) return null;
        Vigente invitacion = invitaciones.getFirst();
        actorAuditoria(jdbc, usuario);
        int guardadas = jdbc.update("""
                INSERT INTO identidad.membresia AS m
                    (id,ganaderia_id,usuario_id,rol,estado,creado_en,actualizado_en)
                VALUES (?,?,?,?,'ACTIVA',?,?)
                ON CONFLICT (ganaderia_id,usuario_id) DO UPDATE SET rol=excluded.rol,
                    estado='ACTIVA',revocado_en=NULL,actualizado_en=excluded.actualizado_en
                WHERE m.estado='REVOCADA' AND m.rol<>'PROPIETARIO' AND m.revocado_en<?
                """, UUID.randomUUID(), ganaderia, usuario, invitacion.rol(),
                fecha(ahora), fecha(ahora), invitacion.creada());
        return guardadas == 1 ? ganaderia : null;
    }

    static void actorAuditoria(JdbcTemplate jdbc, UUID actor) {
        jdbc.queryForObject("SELECT set_config('ganadoconecta.actor_membresia', ?, true)",
                String.class, actor.toString());
    }

    private static OffsetDateTime fecha(Instant valor) {
        return valor.atOffset(ZoneOffset.UTC);
    }

    private record Cuenta(String correo, OffsetDateTime vence) {}
    private record Vigente(String rol, OffsetDateTime creada) {}
}
