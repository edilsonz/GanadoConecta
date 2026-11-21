package pe.ganadoconecta.modulos.catalogo.adaptadores.salida;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;

public final class AnunciosPostgres implements Repositorio {
    private static final String PRIVADO = """
            SELECT a.*,
                ARRAY(SELECT f.id FROM catalogo.foto f WHERE f.anuncio_id=a.id ORDER BY f.creada_en,f.id) fotos
            FROM catalogo.anuncio a
            """;
    private final JdbcTemplate jdbc;
    public AnunciosPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void exigirAnimalActivo(UUID g, UUID animal) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ganado.animal WHERE ganaderia_id=? AND id=? AND estado='ACTIVO')
                """,Boolean.class,g,animal))) throw new Fallo(Error.AUSENTE);
    }

    @Override public List<Privado> privados(UUID g, UUID animal) {
        return jdbc.query(PRIVADO + "WHERE a.ganaderia_id=? AND a.animal_id=? ORDER BY a.actualizado_en DESC,a.id",
                (rs,n) -> privado(rs), g, animal);
    }

    @Override public Privado privado(UUID g, UUID animal, UUID id) {
        return jdbc.query(PRIVADO + "WHERE a.ganaderia_id=? AND a.animal_id=? AND a.id=? FOR UPDATE OF a",
                (rs,n) -> privado(rs), g, animal, id).stream().findFirst().orElse(null);
    }

    private static Privado privado(ResultSet r) throws SQLException {
        var datos = new Datos(r.getString("titulo"),r.getString("caracteristicas"),r.getString("ubicacion_general"),
                r.getBigDecimal("precio_pen"),r.getString("contacto"),r.getBoolean("genealogia_autorizada"));
        return new Privado(r.getObject("id",UUID.class),datos,Estado.valueOf(r.getString("estado")),
                r.getLong("version"),fotos(r));
    }

    @Override public void crear(Ambito a, UUID id, Datos d, Instant ahora) {
        int filas = jdbc.update("""
                INSERT INTO catalogo.anuncio(id,ganaderia_id,animal_id,titulo,caracteristicas,ubicacion_general,
                        precio_pen,contacto,genealogia_autorizada,estado,actualizado_en)
                VALUES (?,?,?,?,?,?,?,?,?,'BORRADOR',?) ON CONFLICT(id) DO NOTHING
                """,id,a.ganaderia(),a.animal(),d.titulo(),d.caracteristicas(),d.ubicacionGeneral(),
                d.precioPen(),d.contacto(),d.genealogiaAutorizada(),ahora.atOffset(ZoneOffset.UTC));
        if (filas == 0) throw new Fallo(Error.CONFLICTO);
    }

    @Override public void editar(UUID id, Datos d, Instant ahora) {
        jdbc.update("""
                UPDATE catalogo.anuncio SET titulo=?,caracteristicas=?,ubicacion_general=?,precio_pen=?,
                    contacto=?,genealogia_autorizada=?,version=version+1,actualizado_en=? WHERE id=?
                """, d.titulo(),d.caracteristicas(),d.ubicacionGeneral(),d.precioPen(),d.contacto(),
                d.genealogiaAutorizada(),ahora.atOffset(ZoneOffset.UTC),id);
    }

    @Override public void estado(UUID id, Estado nuevo, Instant ahora) {
        var fecha = ahora.atOffset(ZoneOffset.UTC);
        jdbc.update("""
                UPDATE catalogo.anuncio SET estado=?,version=version+1,actualizado_en=?,
                   publicado_en=CASE WHEN ?='PUBLICADO' THEN ? ELSE publicado_en END WHERE id=?
                """,nuevo.name(),fecha,nuevo.name(),fecha,id);
    }

    @Override public void agregarFoto(UUID anuncio, UUID foto, int bytes, Instant ahora) {
        var fecha = ahora.atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO catalogo.foto(id,anuncio_id,bytes,creada_en) VALUES(?,?,?,?)",foto,anuncio,bytes,fecha);
        jdbc.update("UPDATE catalogo.anuncio SET version=version+1,actualizado_en=? WHERE id=?",fecha,anuncio);
    }

    @Override public boolean fotoPublica(UUID anuncio, UUID foto) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM catalogo.foto f JOIN catalogo.anuncio a ON a.id=f.anuncio_id
                    JOIN ganado.animal x ON x.id=a.animal_id JOIN identidad.ganaderia g ON g.id=a.ganaderia_id
                    WHERE f.id=? AND a.id=? AND a.estado='PUBLICADO' AND x.estado='ACTIVO' AND g.estado='ACTIVA')
                """,Boolean.class,foto,anuncio));
    }

    @Override public List<Publico> buscar(Filtros f, int limite, int desde) {
        var sql = new StringBuilder("""
                SELECT a.id,a.titulo,a.caracteristicas,x.categoria,x.raza,x.sexo,a.ubicacion_general,a.precio_pen,a.contacto,
                    ARRAY(SELECT fp.id FROM catalogo.foto fp WHERE fp.anuncio_id=a.id ORDER BY fp.creada_en,fp.id) fotos,
                    madre.categoria madre_categoria,madre.raza madre_raza,
                    padre.categoria padre_categoria,padre.raza padre_raza
                FROM catalogo.anuncio a
                JOIN ganado.animal x ON x.id=a.animal_id AND x.ganaderia_id=a.ganaderia_id
                JOIN identidad.ganaderia g ON g.id=a.ganaderia_id
                LEFT JOIN reproduccion.parentesco m ON a.genealogia_autorizada AND m.ganaderia_id=a.ganaderia_id
                    AND m.animal_id=a.animal_id AND m.tipo='MADRE'
                LEFT JOIN ganado.animal madre ON madre.id=m.progenitor_animal_id AND madre.ganaderia_id=m.ganaderia_id
                LEFT JOIN reproduccion.parentesco p ON a.genealogia_autorizada AND p.ganaderia_id=a.ganaderia_id
                    AND p.animal_id=a.animal_id AND p.tipo='PADRE'
                LEFT JOIN ganado.animal padre ON padre.id=p.progenitor_animal_id AND padre.ganaderia_id=p.ganaderia_id
                WHERE a.estado='PUBLICADO' AND x.estado='ACTIVO' AND g.estado='ACTIVA'
                """);
        var parametros = new ArrayList<Object>();
        filtro(sql,parametros," AND x.categoria=?", f.categoria()==null?null:f.categoria().name());
        filtro(sql,parametros," AND x.sexo=?", f.sexo()==null?null:f.sexo().name());
        filtro(sql,parametros," AND lower(x.raza)=lower(?)", f.raza());
        filtro(sql,parametros," AND lower(a.ubicacion_general)=lower(?)", f.ubicacion());
        filtro(sql,parametros," AND a.precio_pen>=?", f.minimo());
        filtro(sql,parametros," AND a.precio_pen<=?", f.maximo());
        sql.append(" ORDER BY a.publicado_en DESC,a.id DESC LIMIT ? OFFSET ?");
        parametros.add(limite);
        parametros.add(desde);
        return jdbc.query(sql.toString(),(rs,n) -> {
            var genealogia = new ArrayList<Progenitor>();
            progenitor(rs,genealogia,"MADRE","madre");
            progenitor(rs,genealogia,"PADRE","padre");
            return new Publico(rs.getObject("id",UUID.class),rs.getString("titulo"),rs.getString("caracteristicas"),
                    rs.getString("categoria"),rs.getString("raza"),rs.getString("sexo"),rs.getString("ubicacion_general"),
                    rs.getBigDecimal("precio_pen"),rs.getString("contacto"),genealogia,fotos(rs));
        },parametros.toArray());
    }

    private static void filtro(StringBuilder sql, List<Object> parametros, String clausula, Object valor) {
        if (valor != null) { sql.append(clausula); parametros.add(valor); }
    }

    private static List<UUID> fotos(ResultSet r) throws SQLException {
        return Arrays.asList((UUID[])r.getArray("fotos").getArray());
    }

    private static void progenitor(ResultSet r,List<Progenitor> resultado,String relacion,String prefijo) throws SQLException {
        String categoria = r.getString(prefijo+"_categoria"), raza = r.getString(prefijo+"_raza");
        if (categoria != null || raza != null) resultado.add(new Progenitor(relacion,categoria,raza));
    }
}
