\set ON_ERROR_STOP on
SELECT current_database() = 'ganadoconecta_rendimiento_v4' AS aislada \gset
\if :aislada
\else
  \echo 'La semilla requiere la base aislada ganadoconecta_rendimiento_v4.'
  \quit 3
\endif

BEGIN;
SELECT pg_advisory_xact_lock(410927);
CREATE FUNCTION pg_temp.uuid_semilla(valor text) RETURNS uuid
LANGUAGE sql IMMUTABLE AS $$
    SELECT overlay(overlay(md5(valor) placing '4' from 13 for 1) placing '8' from 17 for 1)::uuid
$$;
CREATE TEMP TABLE lotes AS
SELECT g.id ganaderia_id, g.propietario_usuario_id, u.email_normalizado
FROM identidad.ganaderia g JOIN identidad.usuario u ON u.id=g.propietario_usuario_id
WHERE u.email_normalizado LIKE 'rendimiento-%@ganadoconecta.invalid'
  AND g.nombre LIKE 'Rendimiento sintético %';
CREATE TEMP TABLE animales_semilla AS
SELECT pg_temp.uuid_semilla(l.ganaderia_id::text || ':' || n) id, l.*, n
FROM lotes l CROSS JOIN generate_series(1, :animales_por_ganaderia) n;
INSERT INTO ganado.animal(id,ganaderia_id,sexo,nombre,categoria,raza,creado_por,creado_en)
SELECT id, ganaderia_id, CASE WHEN n%2=0 THEN 'HEMBRA' ELSE 'MACHO' END,
    'Ejemplar sintético '||n, CASE WHEN n%2=0 THEN 'VACA' ELSE 'TORO' END,
    CASE WHEN n%4<2 THEN 'Criolla' ELSE 'Brown Swiss' END, propietario_usuario_id,
    timestamptz '2025-01-01 00:00:00+00' + n * interval '1 minute'
FROM animales_semilla ON CONFLICT (id) DO NOTHING;
INSERT INTO catalogo.anuncio(id,ganaderia_id,animal_id,titulo,caracteristicas,ubicacion_general,precio_pen,
    genealogia_autorizada,estado,actualizado_en,publicado_en)
SELECT pg_temp.uuid_semilla('anuncio:' || id), ganaderia_id, id, 'Anuncio sintético '||n, 'Datos sintéticos',
    CASE WHEN n%5<3 THEN 'Lima' ELSE 'Cusco' END, 900 + n%1000, false, 'PUBLICADO',
    timestamptz '2025-01-02 00:00:00+00' + n * interval '1 minute',
    timestamptz '2025-01-02 00:00:00+00' + n * interval '1 minute'
FROM animales_semilla WHERE n<=:anuncios_por_ganaderia ON CONFLICT (id) DO NOTHING;
INSERT INTO sanidad.evento_sanitario(id,ganaderia_id,animal_id,tipo,fecha_evento,descripcion,creado_por,creado_en)
SELECT pg_temp.uuid_semilla('evento:' || id || ':' || e), ganaderia_id, id, 'REVISION_VETERINARIA',
    date '2025-01-01' + (n + 17*e)%365, 'Evento exclusivamente sintético',
    propietario_usuario_id, timestamptz '2025-01-02 00:00:00+00'
FROM animales_semilla CROSS JOIN generate_series(1, :eventos_por_animal) e ON CONFLICT (id) DO NOTHING;
COMMIT;
ANALYZE;
SELECT (SELECT count(*) FROM identidad.ganaderia) ganaderias,
       (SELECT count(*) FROM ganado.animal) animales,
       (SELECT count(*) FROM catalogo.anuncio) anuncios,
       (SELECT count(*) FROM sanidad.evento_sanitario) eventos;
