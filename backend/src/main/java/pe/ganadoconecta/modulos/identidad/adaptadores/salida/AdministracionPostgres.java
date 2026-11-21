package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.ConsultarAdministracion.*;

public final class AdministracionPostgres implements Repositorio {
    private final JdbcTemplate jdbc;

    public AdministracionPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public boolean permisoVigente(UUID actor, UUID sesion, Instant ahora) {
        return !jdbc.queryForList("""
                SELECT p.usuario_id
                FROM identidad.permiso_administracion p
                JOIN identidad.usuario u ON u.id=p.usuario_id
                JOIN identidad.sesion_dispositivo s ON s.usuario_id=u.id
                WHERE p.usuario_id=? AND s.id=? AND p.habilitado
                  AND u.estado='ACTIVO' AND s.revocado_en IS NULL AND s.vence_en>?
                FOR SHARE OF s,u,p
                """, UUID.class, actor, sesion, ahora.atOffset(ZoneOffset.UTC)).isEmpty();
    }

    @Override public List<Usuario> listar(Filtro filtro, int limite) {
        var sql = new StringBuilder("""
                SELECT id,email_normalizado,estado,creado_en FROM identidad.usuario WHERE true
                """);
        var parametros = new ArrayList<>();
        agregar(sql, parametros, " AND id=?", filtro.id());
        agregar(sql, parametros, " AND email_normalizado=?", filtro.correo());
        agregar(sql, parametros, " AND estado=?", filtro.estado() == null ? null : filtro.estado().name());
        agregar(sql, parametros, " AND id>?", filtro.cursor());
        sql.append(" ORDER BY id LIMIT ?");
        parametros.add(limite);
        return jdbc.query(sql.toString(), (r,n) -> new Usuario(r.getObject("id", UUID.class),
                r.getString("email_normalizado"), Estado.valueOf(r.getString("estado")),
                r.getObject("creado_en", OffsetDateTime.class).toInstant()), parametros.toArray());
    }

    private static void agregar(StringBuilder sql, List<Object> valores, String clausula, Object valor) {
        if (valor != null) {
            sql.append(clausula);
            valores.add(valor);
        }
    }

    @Override public Estadisticas estadisticas() {
        return jdbc.queryForObject("""
                SELECT u.total, u.activas, g.total, g.activas
                FROM (SELECT count(*) total,count(*) FILTER (WHERE estado='ACTIVO') activas
                      FROM identidad.usuario) u
                CROSS JOIN (SELECT count(*) total,count(*) FILTER (WHERE estado='ACTIVA') activas
                            FROM identidad.ganaderia) g
                """, (r,n) -> new Estadisticas(r.getLong(1), r.getLong(2), r.getLong(3), r.getLong(4)));
    }

    @Override public void auditar(UUID actor, Operacion operacion, Instant ahora) {
        jdbc.update("""
                INSERT INTO identidad.auditoria_administracion(actor_id,operacion,consultado_en)
                VALUES (?,?,?)
                """, actor, operacion.name(), ahora.atOffset(ZoneOffset.UTC));
    }
}
