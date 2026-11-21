package pe.ganadoconecta.modulos.ganado.adaptadores.salida;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.ganado.aplicacion.ControlPeso;
import pe.ganadoconecta.modulos.ganado.aplicacion.ControlPeso.Medicion;
import pe.ganadoconecta.modulos.ganado.aplicacion.ControlPeso.Resultado;

public final class PesosPostgres implements ControlPeso.Repositorio {
    private final JdbcTemplate jdbc;
    public PesosPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<Medicion> listar(UUID ganaderia, UUID animal) {
        exigirAnimal(ganaderia, animal);
        return jdbc.query("""
                SELECT id,kilos,fecha,observaciones FROM ganado.medicion_peso
                WHERE ganaderia_id=? AND animal_id=? ORDER BY fecha,registrado_en,id
                """, (rs,n) -> new Medicion(rs.getObject(1,UUID.class), rs.getBigDecimal(2),
                rs.getObject(3,LocalDate.class), rs.getString(4)), ganaderia, animal);
    }

    @Override public Resultado registrar(ControlPeso.Registro registro) {
        var m = registro.medicion();
        exigirAnimal(registro.ganaderia(), registro.animal());
        int insertadas = jdbc.update("""
                INSERT INTO ganado.medicion_peso(id,ganaderia_id,animal_id,kilos,fecha,observaciones,actor_id,registrado_en)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING
                """, m.id(), registro.ganaderia(), registro.animal(), m.kilos(), m.fecha(), m.observaciones(),
                registro.actor(), registro.ahora().atOffset(ZoneOffset.UTC));
        if (insertadas == 1) return Resultado.APLICADO;
        boolean coincide = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ganado.medicion_peso
                WHERE id=? AND ganaderia_id=? AND animal_id=? AND kilos=?
                    AND fecha=? AND observaciones IS NOT DISTINCT FROM ? AND actor_id=?)
                """, Boolean.class, m.id(), registro.ganaderia(), registro.animal(), m.kilos(),
                m.fecha(), m.observaciones(), registro.actor()));
        return coincide ? Resultado.REPROCESADO : Resultado.CONFLICTO;
    }

    private void exigirAnimal(UUID ganaderia, UUID animal) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ganado.animal WHERE ganaderia_id=? AND id=?)
                """, Boolean.class, ganaderia, animal))) throw new AnimalNoEncontradoException();
    }
}
