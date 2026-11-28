import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'

const serie = process.argv[2] ?? 'cierre-instrumentado'
if (!/^[a-z0-9-]+$/.test(serie)) throw new Error('Nombre de serie inválido.')
const temporal = '.cache/rendimiento-sprint2'
const carpeta = 'docs/07-sprint-2/rendimiento/cierre'
const niveles = [10, 50, 100, 250, 500, 1000, 1500, 2000]
const leer = nombre => {
  const bytes = readFileSync(`${temporal}/${nombre}`)
  const texto = bytes.toString('utf8').replace(/^\uFEFF/, '')
  return nombre.endsWith('-parada.txt') && texto.includes('\uFFFD')
    ? new TextDecoder('windows-1252').decode(bytes) : texto
}
const guardar = (nombre, texto) => writeFileSync(`${carpeta}/${nombre}`, texto.replaceAll('\r\n', '\n'))
const redondear = n => Number.isFinite(n) ? Number(n.toFixed(2)) : null

function csv(texto) {
  const separar = linea => Array.from(linea.matchAll(/(?:^|,)(?:"((?:[^"]|"")*)"|([^,]*))/g),
    ([, comillas, literal]) => comillas?.replaceAll('""', '"') ?? literal)
  const [columnas, ...filas] = texto.trim().split(/\r?\n/).map(separar)
  if (filas.some(fila => fila.length !== columnas.length)) throw new Error('CSV incompleto.')
  return filas.map(fila => Object.fromEntries(columnas.map((nombre, i) => [nombre, fila[i]])))
}

function pico(filas, clave) {
  const valores = filas.map(f => parseFloat(f[clave])).filter(Number.isFinite)
  return valores.length ? Math.max(...valores) : null
}

mkdirSync(carpeta, { recursive: true })
const jmx = `${serie}-jmx.csv`
const muestrasJmx = existsSync(`${temporal}/${jmx}`) && leer(jmx).trim() ? csv(leer(jmx)) : []
if (muestrasJmx.length) guardar(jmx, leer(jmx))

const resumen = {}
for (const caso of ['calentamiento', 'fria', 'poblada', 'quinientos', 'progresiva']) {
  const prefijo = `${serie}-${caso}`
  const archivo = `${prefijo}.json`
  if (!existsSync(`${temporal}/${archivo}`)) continue
  const bruto = JSON.parse(leer(archivo))
  guardar(archivo, JSON.stringify(bruto, null, 2) + '\n')
  const recursos = `${prefijo}-recursos.csv`
  const muestras = csv(leer(recursos))
  guardar(recursos, leer(recursos))
  const datosRedis = Object.fromEntries(['antes', 'despues'].flatMap(momento => {
    const archivoRedis = `${prefijo}-redis-${momento}.txt`
    if (!existsSync(`${temporal}/${archivoRedis}`)) return []
    const contenido = leer(archivoRedis)
    guardar(archivoRedis, contenido)
    const campo = nombre => Number(contenido.match(new RegExp(`^${nombre}:(\\d+)`, 'm'))?.[1])
    return [[momento, {
      aciertos: campo('keyspace_hits'), ausencias: campo('keyspace_misses'),
      getLlamadas: Number(contenido.match(/^cmdstat_get:calls=(\d+)/m)?.[1]),
      getMicrosegundos: Number(contenido.match(/^cmdstat_get:.*?,usec=(\d+)/m)?.[1]),
    }]]
  }))
  const valor = metrica => bruto.metrics[metrica]?.values ?? {}
  const p = (metrica, percentil = 'p(95)') => redondear(valor(metrica)[percentil])
  const fallos = Object.entries(bruto.metrics).flatMap(([metrica, m]) =>
    Object.entries(m.thresholds ?? {}).filter(([, umbral]) => umbral.ok === false).map(([u]) => `${metrica}: ${u}`))
  if (existsSync(`${temporal}/${prefijo}-parada.txt`)) fallos.push(leer(`${prefijo}-parada.txt`).trim())

  const inicio = Date.parse(muestras[0]?.utc) - Number(muestras[0]?.segundo) * 1000
  const dentro = (desde, hasta) => muestrasJmx.filter(m => {
    const segundo = (Date.parse(m.utc) - inicio) / 1000
    return segundo >= desde && segundo < hasta
  })
  const jmxEn = filas => {
    const base = { muestras: filas.length, hilosOcupados: pico(filas, 'hilos_ocupados'),
      hilosLimite: pico(filas, 'hilos_limite'), tcpTomcat: pico(filas, 'tcp_tomcat'),
      tcpLimite: pico(filas, 'tcp_limite'), colaConfigurada: pico(filas, 'cola_configurada'),
      hikariActivas: pico(filas, 'hikari_activas'), hikariTotal: pico(filas, 'hikari_total'),
      hikariEsperando: pico(filas, 'hikari_esperando'),
      heapMiB: redondear(pico(filas, 'heap_usado_bytes') / (1024 * 1024)) }
    if (filas.length < 2) return base
    const inicio = filas[0], fin = filas.at(-1)
    const n = Number(fin.peticiones_servidor) - Number(inicio.peticiones_servidor)
    return { ...base, solicitudes: n, mediaServidorMs: redondear((fin.procesamiento_ms - inicio.procesamiento_ms) / n),
      maximoServidorAcumuladoMs: Number(fin.maximo_ms),
      gcMs: fin.gc_ms - inicio.gc_ms, gcCiclos: fin.gc_ciclos - inicio.gc_ciclos }
  }
  const duracion = bruto.state.testRunDurationMs / 1000
  resumen[caso] = {
    vusActivos: valor('vus').max ?? null, segundos: redondear(duracion),
    solicitudes: valor('http_reqs').count ?? null, reqsPorSegundo: redondear(valor('http_reqs').rate),
    p50Ms: p('http_req_duration', 'med'), p95Ms: p('http_req_duration'), p99Ms: p('http_req_duration', 'p(99)'),
    p95AnimalesMs: p('http_req_duration{operacion:animales}'), p95CatalogoMs: p('http_req_duration{operacion:catalogo}'),
    erroresPct: redondear(100 * valor('http_req_failed').rate), comprobacionesPct: redondear(100 * valor('checks').rate),
    fallos, datosRedis, jmx: jmxEn(dentro(0, duracion)),
    recursos: {
      cpuJavaPct: pico(muestras, 'java_cpu_una_cpu_pct'), cpuK6Pct: pico(muestras, 'k6_cpu_una_cpu_pct'),
      javaRssMiB: pico(muestras, 'java_rss_mib'), k6RssMiB: pico(muestras, 'k6_rss_mib'),
      pgActivas: pico(muestras, 'pg_activas'), pgBloqueadas: pico(muestras, 'pg_espera_bloqueo'),
      pgEsperaIo: pico(muestras, 'pg_espera_io'), tcpEstablecidas: pico(muestras, 'tcp_establecidas'),
      tcpTimeWait: pico(muestras, 'tcp_time_wait'),
      memoriaDisponibleMiB: Math.min(...muestras.map(m => Number(m.host_disponible_mib))),
    },
    etapas: niveles.flatMap((vu, indice) => {
      const etiqueta = `{nivel:${vu}}`
      const pedidos = valor('http_reqs' + etiqueta).count
      if (!pedidos) return []
      const desde = caso === 'quinientos' ? 5 : indice * 21 + 5
      const segundos = Math.max(0, Math.min(caso === 'quinientos' ? 25 : 16, duracion - desde))
      return [{ vu, segundos: redondear(segundos), reqsPorSegundo: redondear(pedidos / segundos),
        p50Ms: p('http_req_duration' + etiqueta, 'med'), p95Ms: p('http_req_duration' + etiqueta),
        p99Ms: p('http_req_duration' + etiqueta, 'p(99)'), esperaMs: p('http_req_waiting' + etiqueta),
        conexionMs: p('http_req_connecting' + etiqueta), bloqueoMs: p('http_req_blocked' + etiqueta),
        recepcionMs: p('http_req_receiving' + etiqueta),
        erroresPct: redondear(100 * valor('http_req_failed' + etiqueta).rate), jmx: jmxEn(dentro(desde, desde + segundos)) }]
    }),
  }
}
guardar(`${serie}-resumen.json`, JSON.stringify(resumen, null, 2) + '\n')

function grafico(archivo, series) {
  const x = vu => 67 + niveles.indexOf(vu) * 84
  const y = ms => 274 - ms * 0.04
  const guias = [0, 1000, 2000, 3000, 4000, 5000].map(ms =>
    `<path d="M62 ${y(ms)}H674" stroke="${ms === 4000 ? '#91422c' : '#cdd8d2'}" ` +
    `stroke-dasharray="${ms === 4000 ? '5 4' : '0'}"/><text x="12" y="${y(ms) + 4}">${ms}</text>`).join('\n')
  const lineas = series.map(([nombre, color, etapas], i) =>
    `<path d="${etapas.map((e, n) => `${n ? 'L' : 'M'}${x(e.vu)} ${y(e.p95Ms)}`).join(' ')}" fill="none" stroke="${color}" stroke-width="2.5"/>` +
    etapas.map(e => `<circle cx="${x(e.vu)}" cy="${y(e.p95Ms)}" r="3" fill="${color}"/>`).join('') +
    `<rect x="${85 + i * 195}" y="310" width="12" height="12" fill="${color}"/><text x="${105 + i * 195}" y="321">${nombre}</text>`).join('\n')
  guardar(archivo, `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 710 340" role="img" aria-labelledby="titulo">
<title id="titulo">Latencia p95 en la escalada de usuarios virtuales</title>
<rect width="710" height="340" fill="#f8f7f1"/><g font-family="system-ui, sans-serif" font-size="13" fill="#163326">
<text x="20" y="28" font-size="19" font-weight="700">Latencia p95 por tramo en milisegundos</text>
${guias}
${niveles.map(vu => `<text x="${x(vu)}" y="295" text-anchor="middle">${vu}</text>`).join('')}
${lineas}
</g></svg>\n`)
}

if (serie === 'cierre-instrumentado' && resumen.progresiva) {
  const previo = JSON.parse(readFileSync('docs/07-sprint-2/rendimiento/k6-resumen.json', 'utf8')).progresiva
  const primero = JSON.parse(readFileSync(`${carpeta}/cierre-resumen.json`, 'utf8')).progresiva
  grafico('comparacion-escalada.svg', [
    ['Incremento anterior', '#8b6546', previo.etapas.map(e => ({ vu: e.nivel, p95Ms: e.p95Ms }))],
    ['Primer cierre', '#397b96', primero.etapas],
    ['Con JMX local', '#286649', resumen.progresiva.etapas],
  ])
}

if (serie === 'cierre-pool5' && resumen.progresiva) {
  const base = JSON.parse(readFileSync(`${carpeta}/cierre-instrumentado-resumen.json`, 'utf8')).progresiva
  grafico('comparacion-pool.svg', [
    ['3 conexiones', '#8b6546', base.etapas],
    ['5 conexiones', '#286649', resumen.progresiva.etapas],
  ])
}
