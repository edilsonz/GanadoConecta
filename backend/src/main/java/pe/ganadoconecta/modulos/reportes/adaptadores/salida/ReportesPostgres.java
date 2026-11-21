package pe.ganadoconecta.modulos.reportes.adaptadores.salida;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import pe.ganadoconecta.modulos.reportes.aplicacion.ConsultarReportesGanaderos.*;

public final class ReportesPostgres implements Repositorio {
    private static final String CONSULTA = """
            WITH sanidad_por_tipo AS (
                SELECT tipo, count(*) AS total FROM sanidad.evento_sanitario
                WHERE ganaderia_id=:ganaderia AND fecha_evento BETWEEN :desde AND :hasta %s
                GROUP BY tipo
            ), reproduccion_por_tipo AS (
                SELECT count(*) FILTER (WHERE tipo='MONTA') montas,
                       count(*) FILTER (WHERE tipo='INSEMINACION') inseminaciones,
                       count(*) FILTER (WHERE tipo='EVALUACION_GESTACION') evaluaciones,
                       count(*) FILTER (WHERE estado_gestacion='CONFIRMADA') confirmadas,
                       count(*) FILTER (WHERE estado_gestacion='NO_CONFIRMADA') no_confirmadas,
                       count(*) FILTER (WHERE estado_gestacion='INDETERMINADA') indeterminadas
                FROM reproduccion.evento_reproductivo
                WHERE ganaderia_id=:ganaderia AND fecha_evento BETWEEN :desde AND :hasta %s
            ), partos AS (
                SELECT id,ganaderia_id FROM reproduccion.parto
                WHERE ganaderia_id=:ganaderia AND fecha_parto BETWEEN :desde AND :hasta %s
            ), diferencias_peso AS (
                SELECT id,fecha,kilos,registrado_en,
                       kilos - lag(kilos) OVER (ORDER BY fecha,registrado_en,id) AS variacion,
                       count(*) OVER () AS total
                FROM ganado.medicion_peso
                WHERE ganaderia_id=:ganaderia AND animal_id=CAST(:animal AS uuid)
                  AND fecha BETWEEN :desde AND :hasta
            ), pesos_recientes AS (
                SELECT * FROM diferencias_peso ORDER BY fecha DESC,registrado_en DESC,id DESC LIMIT 50
            ), serie AS (
                SELECT total, fecha, registrado_en, id,
                    jsonb_build_object('id',id,'fecha',fecha,'kilos',kilos,'variacion',variacion) punto
                FROM pesos_recientes
            )
            SELECT jsonb_build_object(
                'sanidad', jsonb_build_object(
                    'total', (SELECT coalesce(sum(total),0) FROM sanidad_por_tipo),
                    'porTipo', (SELECT coalesce(jsonb_agg(to_jsonb(s) ORDER BY tipo),'[]'::jsonb) FROM sanidad_por_tipo s)
                ),
                'reproduccion', (SELECT jsonb_build_object(
                    'montas',montas,'inseminaciones',inseminaciones,'evaluaciones',evaluaciones,
                    'confirmadas',confirmadas,'noConfirmadas',no_confirmadas,'indeterminadas',indeterminadas,
                    'partos',(SELECT count(*) FROM partos),
                    'crias',(SELECT count(*) FROM reproduccion.parto_cria c JOIN partos p
                             ON p.id=c.parto_id AND p.ganaderia_id=c.ganaderia_id)
                ) FROM reproduccion_por_tipo),
                'peso', jsonb_build_object(
                    'total',(SELECT coalesce(max(total),0) FROM serie),
                    'ultimo',(SELECT punto FROM serie ORDER BY fecha DESC,registrado_en DESC,id DESC LIMIT 1),
                    'mediciones',(SELECT coalesce(jsonb_agg(punto ORDER BY fecha,registrado_en,id),'[]'::jsonb) FROM serie)
                )
            ) %s
            """;
    private static final String SIN_ANIMAL = CONSULTA.formatted("", "", "", "");
    private static final String POR_ANIMAL = CONSULTA.formatted("AND animal_id=:animal", "AND animal_id=:animal",
            "AND madre_id=:animal", "WHERE EXISTS (SELECT 1 FROM ganado.animal WHERE id=:animal AND ganaderia_id=:ganaderia)");

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public ReportesPostgres(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
        this.json = json;
    }

    @Override public Optional<Reporte> consultar(UUID ganaderia, UUID animal, Periodo periodo) {
        var params = new MapSqlParameterSource().addValue("ganaderia", ganaderia).addValue("animal", animal)
                .addValue("desde", periodo.desde()).addValue("hasta", periodo.hasta());
        return jdbc.query(animal == null ? SIN_ANIMAL : POR_ANIMAL, params, (r,n) -> {
            try { return json.readValue(r.getString(1), Reporte.class); }
            catch (JsonProcessingException e) { throw new SQLException("Respuesta de reportes inválida", e); }
        }).stream().findFirst();
    }
}
