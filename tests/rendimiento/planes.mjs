import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'

const modulos = 'backend/src/main/java/pe/ganadoconecta/modulos'
const revision = process.argv[2]
if (revision && !/^[0-9a-f]{7,40}$/.test(revision)) throw new Error('Indica un hash de Git válido.')
const sqlJava = (ruta, patron) => {
  const archivo = `${modulos}/${ruta}`
  const texto = revision ? execFileSync('git', ['show', revision + ':' + archivo], { encoding: 'utf8' })
    : readFileSync(archivo, 'utf8')
  const fragmento = texto.match(patron)?.[1]
  if (!fragmento) throw new Error(`SQL no encontrado en ${ruta}.`)
  return fragmento.trim()
}
const catalogo = sqlJava('catalogo/adaptadores/salida/AnunciosPostgres.java', /new StringBuilder\("""([\s\S]+?)"""\);/)
const proyeccion = catalogo.startsWith('WITH')
  ? sqlJava('catalogo/adaptadores/salida/AnunciosPostgres.java', /String PROYECCION_PAGINA = """([\s\S]+?)""";/)
  : ''
const reporte = sqlJava('reportes/adaptadores/salida/ReportesPostgres.java', /String CONSULTA = """([\s\S]+?)""";/)
const columnasAnimal = sqlJava('ganado/adaptadores/salida/ConsultasAnimalesPostgres.java', /String COLUMNAS = """([\s\S]+?)""";/)

function plan(nombre, sql) {
  console.log(`\\echo ${nombre}\nEXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)\n${sql};`)
}

console.log(`\
\\set ON_ERROR_STOP on
SELECT current_database() = 'ganadoconecta_rendimiento_v4' AS aislada \\gset
\\if :aislada
\\else
  \\quit 3
\\endif
\\pset tuples_only on
\\pset format unaligned
BEGIN READ ONLY;
SELECT id ganaderia FROM identidad.ganaderia WHERE nombre='Rendimiento sintético 1' \\gset
SELECT creado_en cursor_fecha,id cursor_id FROM ganado.animal WHERE ganaderia_id=:'ganaderia'
ORDER BY creado_en DESC,id DESC LIMIT 1 OFFSET 19 \\gset`)

plan('animales_primera', `SELECT ${columnasAnimal} FROM ganado.animal WHERE ganaderia_id=:'ganaderia'
ORDER BY creado_en DESC,id DESC LIMIT 21`)
plan('animales_cursor', `SELECT ${columnasAnimal} FROM ganado.animal WHERE ganaderia_id=:'ganaderia'
AND (creado_en,id)<(:'cursor_fecha',:'cursor_id') ORDER BY creado_en DESC,id DESC LIMIT 21`)
const pagina = (filtro, offset) => catalogo + filtro + ` ORDER BY a.publicado_en DESC,a.id DESC LIMIT 21 OFFSET ${offset}\n` + proyeccion
plan('catalogo_primera', pagina('', 0))
plan('catalogo_filtros', pagina(`
AND x.categoria='VACA' AND x.sexo='HEMBRA' AND lower(x.raza)=lower('Criolla')
AND lower(a.ubicacion_general)=lower('Lima') AND a.precio_pen BETWEEN 1000 AND 1600`, 40))
plan('catalogo_profunda', pagina('', 4980))
plan('reportes_mes', reporte.replaceAll('%s', '').replaceAll(':ganaderia', ":'ganaderia'")
  .replaceAll(':animal', 'NULL').replaceAll(':desde', "date '2025-02-01'").replaceAll(':hasta', "date '2025-02-28'"))
console.log('COMMIT;')
