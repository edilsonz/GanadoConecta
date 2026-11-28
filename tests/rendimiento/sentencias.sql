\set ON_ERROR_STOP on
SELECT current_database() = 'ganadoconecta_rendimiento_v4' AS aislada \gset
\if :aislada
\else
  \quit 3
\endif
WITH mediciones AS (
    SELECT CASE
        WHEN query LIKE 'SELECT revision FROM catalogo.revision_publica%' THEN 'revision_publica'
        WHEN query LIKE 'WITH pagina AS MATERIALIZED%' THEN 'catalogo_proyeccion'
        WHEN query LIKE '%FROM ganado.animal%ORDER BY creado_en DESC, id DESC%' THEN 'animales_paginados'
        WHEN query LIKE 'SELECT%' AND query LIKE '%FROM identidad.sesion_dispositivo%' THEN 'consulta_sesion'
    END consulta, calls, total_exec_time, max_exec_time, rows, shared_blks_hit, shared_blks_read
    FROM pg_stat_statements
    WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
)
SELECT consulta, sum(calls) llamadas, round((sum(total_exec_time) / sum(calls))::numeric, 3) media_ms,
       round(max(max_exec_time)::numeric, 3) max_ms, sum(rows) filas,
       sum(shared_blks_hit) bloques_memoria, sum(shared_blks_read) bloques_leidos
FROM mediciones WHERE consulta IS NOT NULL GROUP BY consulta ORDER BY consulta;
