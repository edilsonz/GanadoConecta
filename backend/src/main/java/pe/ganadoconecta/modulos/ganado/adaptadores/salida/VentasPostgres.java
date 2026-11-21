package pe.ganadoconecta.modulos.ganado.adaptadores.salida;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.RegistrarVenta;

public final class VentasPostgres implements RegistrarVenta.Repositorio {
    private final JdbcTemplate jdbc;
    public VentasPostgres(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override public RegistrarVenta.Recibo registrar(UUID id,UUID ganaderia,UUID animal,UUID actor,Instant ahora) {
        var estados = jdbc.query("SELECT estado FROM ganado.animal WHERE ganaderia_id=? AND id=? FOR UPDATE",
                (rs,n)->rs.getString(1),ganaderia,animal);
        if (estados.isEmpty()) throw new AnimalNoEncontradoException();
        if ("VENDIDO".equals(estados.getFirst())) {
            return jdbc.query("""
                    SELECT registrada_en,anuncios_cerrados FROM ganado.venta
                    WHERE ganaderia_id=? AND animal_id=? AND operacion_id=? AND actor_id=?
                    """,(rs,n)->new RegistrarVenta.Recibo(id,animal,rs.getObject(1,OffsetDateTime.class).toInstant(),
                    rs.getInt(2),true),ganaderia,animal,id,actor).stream().findFirst()
                    .orElseThrow(RegistrarVenta.Conflicto::new);
        }
        var fecha=ahora.atOffset(ZoneOffset.UTC);
        if (jdbc.update("""
                INSERT INTO ganado.venta(operacion_id,ganaderia_id,animal_id,actor_id,registrada_en)
                VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING
                """,id,ganaderia,animal,actor,fecha) == 0) throw new RegistrarVenta.Conflicto();
        jdbc.update("UPDATE ganado.animal SET estado='VENDIDO' WHERE ganaderia_id=? AND id=?",ganaderia,animal);
        int cerrados = jdbc.update("""
                UPDATE catalogo.anuncio SET estado='CERRADO_VENTA',version=version+1,actualizado_en=?
                WHERE ganaderia_id=? AND animal_id=? AND estado IN ('PUBLICADO','PAUSADO')
                """,fecha,ganaderia,animal);
        jdbc.update("UPDATE ganado.venta SET anuncios_cerrados=? WHERE operacion_id=?",cerrados,id);
        return new RegistrarVenta.Recibo(id,animal,ahora,cerrados,false);
    }
}
