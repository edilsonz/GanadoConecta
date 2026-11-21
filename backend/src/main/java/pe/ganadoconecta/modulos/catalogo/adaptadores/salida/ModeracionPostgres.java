package pe.ganadoconecta.modulos.catalogo.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Fallo;
import pe.ganadoconecta.modulos.catalogo.aplicacion.ModeracionCatalogo.*;

public final class ModeracionPostgres implements Repositorio {
    private final JdbcTemplate jdbc;
    public ModeracionPostgres(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override public boolean cuentaVigente(UUID actor,UUID sesion,Instant ahora) {
        return !jdbc.queryForList("""
                SELECT s.id FROM identidad.sesion_dispositivo s JOIN identidad.usuario u ON u.id=s.usuario_id
                WHERE s.id=? AND u.id=? AND u.estado='ACTIVO' AND s.revocado_en IS NULL AND s.vence_en>?
                FOR UPDATE OF s,u
                """,UUID.class,sesion,actor,ahora.atOffset(ZoneOffset.UTC)).isEmpty();
    }

    @Override public boolean permisoVigente(UUID actor) {
        return !jdbc.queryForList("""
                SELECT usuario_id FROM identidad.permiso_moderacion WHERE usuario_id=? AND habilitado FOR UPDATE
                """,UUID.class,actor).isEmpty();
    }

    @Override public void reportar(UUID id,UUID anuncio,UUID actor,String motivo,Instant ahora) {
        var estados = jdbc.queryForList("""
                SELECT a.estado FROM catalogo.anuncio a
                JOIN ganado.animal x ON x.id=a.animal_id
                JOIN identidad.ganaderia g ON g.id=a.ganaderia_id
                WHERE a.id=? AND a.estado='PUBLICADO' AND x.estado='ACTIVO' AND g.estado='ACTIVA' FOR UPDATE OF a
                """,String.class,anuncio);
        if (estados.isEmpty()) throw new Fallo(Error.AUSENTE);
        jdbc.update("""
                INSERT INTO catalogo.reporte(id,anuncio_id,motivo,reportado_por,recibido_en) VALUES(?,?,?,?,?)
                """,id,anuncio,motivo,actor,ahora.atOffset(ZoneOffset.UTC));
    }

    @Override public List<Reporte> bandeja() {
        return jdbc.query("""
                SELECT r.id,r.anuncio_id,a.titulo,a.estado,r.motivo,r.estado,r.recibido_en,r.revisado_en
                FROM catalogo.reporte r JOIN catalogo.anuncio a ON a.id=r.anuncio_id
                ORDER BY (r.estado='PENDIENTE') DESC,r.recibido_en DESC,r.id DESC LIMIT 100
                """,(rs,n)->{
            var revisado=rs.getObject(8,OffsetDateTime.class);
            return new Reporte(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),
                    rs.getString(4),rs.getString(5),rs.getString(6),
                    rs.getObject(7,OffsetDateTime.class).toInstant(),revisado==null?null:revisado.toInstant());
        });
    }

    @Override public void revisar(UUID reporte,UUID actor,Decision decision,Instant ahora) {
        var revisiones=jdbc.query("""
                SELECT anuncio_id,estado,revisado_por FROM catalogo.reporte WHERE id=? FOR UPDATE
                """,(rs,n)->new Revision(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class)),reporte);
        if (revisiones.isEmpty()) throw new Fallo(Error.AUSENTE);
        var actual=revisiones.getFirst();
        if (!"PENDIENTE".equals(actual.estado())) {
            if (actual.estado().equals(decision.name()) && actor.equals(actual.actor())) return;
            throw new Fallo(Error.CONFLICTO);
        }
        var fecha=ahora.atOffset(ZoneOffset.UTC);
        if (decision==Decision.RETIRADO) {
            jdbc.update("""
                    UPDATE catalogo.anuncio SET estado='RETIRADO',version=version+1,actualizado_en=?
                    WHERE id=? AND estado NOT IN ('RETIRADO','CERRADO_VENTA')
                    """,fecha,actual.anuncio());
        }
        jdbc.update("""
                UPDATE catalogo.reporte SET estado=?,revisado_por=?,revisado_en=? WHERE id=?
                """,decision.name(),actor,fecha,reporte);
    }

    private record Revision(UUID anuncio,String estado,UUID actor) {}
}
