import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'

const temporal = '.cache/rendimiento-sprint2'
const destino = 'docs/07-sprint-2/rendimiento'
const pruebas = ['smoke-v4', 'calentamiento-sin-cache', 'calentamiento-cache', 'cien-sin-cache', 'cien-cache-fria',
  'cien-cache-poblada', 'cien-redis-caido', 'progresiva']
const leer = archivo => readFileSync(archivo, 'utf8').replace(/^\uFEFF/, '')
const valor = (datos, nombre) => datos.metrics[nombre]?.values ?? {}
const redondear = n => Number.isFinite(n) ? Number(n.toFixed(2)) : null
const medidas = (r, l) => ({
  solicitudes: r.count ?? null, solicitudesPorSegundo: redondear(r.rate),
  p50Ms: redondear(l.med), p95Ms: redondear(l['p(95)']), p99Ms: redondear(l['p(99)']),
})

function estadisticasRedis(nombre) {
  const antes = `${temporal}/${nombre}-redis-antes.txt`
  const despues = `${temporal}/${nombre}-redis-despues.txt`
  if (!existsSync(antes) || !existsSync(despues)) return null
  const campo = (ruta, clave) => Number(leer(ruta).match(new RegExp(`^${clave}:(\\d+)`, 'm'))?.[1])
  const aciertos = campo(despues, 'keyspace_hits') - campo(antes, 'keyspace_hits')
  const ausencias = campo(despues, 'keyspace_misses') - campo(antes, 'keyspace_misses')
  if (![aciertos, ausencias].every(Number.isFinite)) return null
  writeFileSync(`${destino}/redis-${nombre}-antes.txt`, leer(antes).replaceAll('\r\n', '\n'))
  writeFileSync(`${destino}/redis-${nombre}-despues.txt`, leer(despues).replaceAll('\r\n', '\n'))
  return { aciertos, ausencias, aciertosPct: redondear(100 * aciertos / (aciertos + ausencias)) }
}

function recursos(texto) {
  const separar = linea => Array.from(linea.matchAll(/(?:^|,)(?:"((?:[^"]|"")*)"|([^,]*))/g),
    ([, entreComillas, literal]) => entreComillas?.replaceAll('""', '"') ?? literal)
  const [titulo, ...filas] = texto.trim().split(/\r?\n/).map(separar)
  if (filas.some(fila => fila.length !== titulo.length)) throw new Error('Muestreo de recursos CSV inconsistente.')
  const valores = nombre => filas.map(fila => fila[titulo.indexOf(nombre)]).filter(Boolean)
  const extremo = (nombre, f = Math.max) => {
    const numeros = valores(nombre).map(parseFloat).filter(Number.isFinite)
    return numeros.length ? f(...numeros) : null
  }
  const usoMemoria = nombre => {
    const medida = valores(nombre).map(v => v.match(/\/ ([\d.]+)([KMG])iB/)?.slice(1))
      .filter(Boolean).map(([n, unidad]) => Number(n) * { K: 1 / 1024, M: 1, G: 1024 }[unidad])
    return medida.length ? redondear(Math.max(...medida)) : null
  }
  return { muestras: filas.length, maxVuMuestreados: extremo('vu_activos'),
    maxJavaCpuPct: extremo('java_cpu_una_cpu_pct'), maxJavaRssMiB: extremo('java_rss_mib'),
    maxK6CpuPct: extremo('k6_cpu_una_cpu_pct'), maxK6RssMiB: extremo('k6_rss_mib'),
    minMemoriaDisponibleMiB: extremo('host_disponible_mib', Math.min),
    maxPgConexiones: extremo('pg_conexiones'), maxPgActivas: extremo('pg_activas'),
    maxPgCpuPct: extremo('pg_cpu_y_memoria'), maxPgMemoriaMiB: usoMemoria('pg_cpu_y_memoria'),
    maxRedisCpuPct: extremo('redis_cpu_y_memoria'), maxRedisMemoriaMiB: usoMemoria('redis_cpu_y_memoria') }
}

mkdirSync(destino, { recursive: true })
const resumen = {}
for (const nombre of pruebas) {
  const resultado = `${temporal}/${nombre}.json`
  const muestreo = `${temporal}/${nombre}-recursos.csv`
  const parada = `${temporal}/${nombre}-parada.txt`
  if (!existsSync(resultado) && !existsSync(muestreo)) continue
  const bruto = existsSync(resultado) ? JSON.parse(leer(resultado)) : null
  const csv = existsSync(muestreo) ? leer(muestreo) : null
  if (bruto) writeFileSync(`${destino}/k6-${nombre}.json`, JSON.stringify(bruto, null, 2) + '\n')
  if (csv) writeFileSync(`${destino}/recursos-${nombre}.csv`, csv.replaceAll('\r\n', '\n'))
  const datos = { parada: existsSync(parada) ? leer(parada).trim() : null, recursos: csv ? recursos(csv) : null }
  if (bruto) {
    const r = valor(bruto, 'http_reqs')
    const umbralesFallidos = Object.entries(bruto.metrics).flatMap(([metrica, medicion]) =>
      Object.entries(medicion.thresholds ?? {}).filter(([, resultado]) => resultado.ok === false)
        .map(([umbral]) => `${metrica}: ${umbral}`))
    Object.assign(datos, {
      ...medidas(r, valor(bruto, 'http_req_duration')),
      maxVuK6: valor(bruto, 'vus').max ?? null,
      erroresPct: redondear(100 * valor(bruto, 'http_req_failed').rate),
      comprobacionesPct: redondear(100 * valor(bruto, 'checks').rate),
      umbralesFallidos,
      p95AnimalesMs: redondear(valor(bruto, 'http_req_duration{operacion:animales}')['p(95)']),
      p95CatalogoMs: redondear(valor(bruto, 'http_req_duration{operacion:catalogo}')['p(95)']),
      redis: estadisticasRedis(nombre),
      etapas: [10, 50, 100, 250, 500, 1000, 1500, 2000].flatMap((nivel, indice) => {
        const req = valor(bruto, `http_reqs{nivel:${nivel}}`)
        if (!req.count) return []
        const segundos = Math.min(16, Math.max(0, bruto.state.testRunDurationMs / 1000 - indice * 21 - 5))
        return [{ nivel, ...medidas(req, valor(bruto, `http_req_duration{nivel:${nivel}}`)),
          segundos: redondear(segundos), solicitudesPorSegundo: redondear(req.count / segundos),
          erroresPct: redondear(100 * valor(bruto, `http_req_failed{nivel:${nivel}}`).rate) }]
      }),
    })
  }
  resumen[nombre] = datos
}
writeFileSync(`${destino}/k6-resumen.json`, JSON.stringify(resumen, null, 2) + '\n')

function grafico(nombre, titulo, datos) {
  if (!datos.length) return
  const ancho = 690
  const alto = 65 + 52 * datos.length
  const maximo = Math.max(...datos.map(([, valor]) => valor))
  if (maximo <= 0) return
  const filas = datos.map(([etiqueta, ms], i) => {
    const y = 52 + i * 52
    const barra = Math.max(1, 365 * ms / maximo)
    return `<text x="20" y="${y + 17}">${etiqueta}</text>` +
      `<rect x="185" y="${y}" width="${barra}" height="23" fill="#26714c"/>` +
      `<text x="${194 + barra}" y="${y + 17}">${ms.toLocaleString('es-PE')} ms</text>`
  }).join('\n')
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${ancho} ${alto}" role="img" aria-labelledby="titulo">
<title id="titulo">${titulo}</title>
<rect width="${ancho}" height="${alto}" fill="#f8f7f1"/>
<g font-family="system-ui, sans-serif" font-size="14" fill="#163326">
<text x="20" y="32" font-size="20" font-weight="700">${titulo}</text>
${filas}
</g></svg>\n`
  writeFileSync(`${destino}/${nombre}.svg`, svg)
}
const escenarios = Object.entries({
  'cien-sin-cache': 'Sin caché', 'cien-cache-fria': 'Caché fría',
  'cien-cache-poblada': 'Caché poblada', 'cien-redis-caido': 'Redis caído',
})
grafico('k6-comparacion', 'Catálogo público · p95 con 100 VU',
  escenarios.flatMap(([nombre, etiqueta]) => {
    const datos = resumen[nombre]
    return datos?.p95CatalogoMs != null && !datos.parada ? [[etiqueta, datos.p95CatalogoMs]] : []
  }))
grafico('k6-escalada', 'Escalada · p95 en tramos sostenidos',
  (resumen.progresiva?.etapas ?? []).map(e => [e.nivel + ' VU', e.p95Ms]))
