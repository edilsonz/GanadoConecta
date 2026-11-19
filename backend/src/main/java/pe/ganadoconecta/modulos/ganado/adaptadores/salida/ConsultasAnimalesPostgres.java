package pe.ganadoconecta.modulos.ganado.adaptadores.salida;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import pe.ganadoconecta.modulos.ganado.aplicacion.ConsultasAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.CursorListadoAnimales;
import pe.ganadoconecta.modulos.ganado.aplicacion.PersistenciaAnimalException;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.AnimalId;
import pe.ganadoconecta.modulos.ganado.dominio.Categoria;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.EstadoAnimal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;
import pe.ganadoconecta.modulos.ganado.dominio.Sexo;

public final class ConsultasAnimalesPostgres implements ConsultasAnimales {
    private static final String COLUMNAS = """
            id, ganaderia_id, sexo, nombre, arete_frente, arete_reverso,
            raza, fecha_nacimiento, nacimiento_estimado, categoria,
            observaciones, estado, version, creado_por, creado_en
            """;
    private static final RowMapper<Animal> MAPEO = (resultado, fila) -> {
        String categoria = resultado.getString("categoria");
        return new Animal(
                new AnimalId(resultado.getObject("id", UUID.class)),
                new GanaderiaId(resultado.getObject("ganaderia_id", UUID.class)),
                new DatosAnimal(
                        Sexo.valueOf(resultado.getString("sexo")),
                        resultado.getString("nombre"),
                        resultado.getString("arete_frente"),
                        resultado.getString("arete_reverso"),
                        resultado.getString("raza"),
                        resultado.getObject("fecha_nacimiento", LocalDate.class),
                        resultado.getObject("nacimiento_estimado", Boolean.class),
                        categoria == null ? null : Categoria.valueOf(categoria),
                        resultado.getString("observaciones")),
                EstadoAnimal.valueOf(resultado.getString("estado")),
                resultado.getLong("version"),
                resultado.getObject("creado_por", UUID.class),
                resultado.getObject("creado_en", OffsetDateTime.class).toInstant());
    };

    private final JdbcTemplate jdbc;

    public ConsultasAnimalesPostgres(JdbcTemplate jdbc) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc);
    }

    @Override
    public List<Animal> listar(GanaderiaId ganaderiaId, int cantidad,
            CursorListadoAnimales cursor) {
        try {
            if (cursor == null) {
                return jdbc.query("""
                        SELECT %s
                        FROM ganado.animal
                        WHERE ganaderia_id = ?
                        ORDER BY creado_en DESC, id DESC
                        LIMIT ?
                        """.formatted(COLUMNAS), MAPEO,
                        ganaderiaId.valor(), cantidad);
            }
            return jdbc.query("""
                    SELECT %s
                    FROM ganado.animal
                    WHERE ganaderia_id = ?
                      AND (creado_en, id) < (?, ?)
                    ORDER BY creado_en DESC, id DESC
                    LIMIT ?
                    """.formatted(COLUMNAS), MAPEO,
                    ganaderiaId.valor(),
                    OffsetDateTime.ofInstant(cursor.creadoEn(), ZoneOffset.UTC),
                    cursor.animalId().valor(), cantidad);
        } catch (DataAccessException exception) {
            throw new PersistenciaAnimalException(exception);
        }
    }

    @Override
    public Optional<Animal> buscar(GanaderiaId ganaderiaId, AnimalId animalId) {
        try {
            return jdbc.query("""
                    SELECT %s
                    FROM ganado.animal
                    WHERE ganaderia_id = ? AND id = ?
                    """.formatted(COLUMNAS), MAPEO,
                    ganaderiaId.valor(), animalId.valor()).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new PersistenciaAnimalException(exception);
        }
    }
}
