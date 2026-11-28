import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'

const temporal = '.cache/rendimiento-sprint2'
const destino = 'docs/07-sprint-2/rendimiento'
const bruto = {}
for (const variante of ['base', 'opt']) {
  bruto[variante] = Array.from({ length: 3 }, (_, indice) => {
    const archivo = `${temporal}/sql-${variante}-${indice + 1}.txt`
    const salida = readFileSync(archivo, 'utf8').replace(/^\uFEFF/, '')
    const planes = Object.fromEntries(Array.from(
      salida.matchAll(/^(animales_\w+|catalogo_\w+|reportes_\w+)\r?\n([\s\S]*?\n\])/gm),
      ([, consulta, datos]) => [consulta, JSON.parse(datos)[0]],
    ))
    if (Object.keys(planes).length !== 6) throw new Error(`Faltan planes en ${archivo}`)
    return planes
  })
}
const mediana = serie => [...serie].sort((a, b) => a - b)[Math.floor(serie.length / 2)]
const resumen = Object.fromEntries(Object.keys(bruto.base[0]).map(consulta => [
  consulta,
  Object.fromEntries(['base', 'opt'].map(variante => {
    const planes = bruto[variante].map(prueba => prueba[consulta])
    return [variante, {
      planMs: mediana(planes.map(plan => plan['Planning Time'])),
      ejecucionMs: mediana(planes.map(plan => plan['Execution Time'])),
      buffersHit: mediana(planes.map(plan => plan.Plan['Shared Hit Blocks'])),
      buffersRead: mediana(planes.map(plan => plan.Plan['Shared Read Blocks'])),
    }]
  })),
]))
mkdirSync(destino, { recursive: true })
writeFileSync(`${destino}/postgres-planes.json`, JSON.stringify(bruto) + '\n')
writeFileSync(`${destino}/postgres-resumen.json`, JSON.stringify(resumen, null, 2) + '\n')

const filas = Object.entries(resumen).filter(([consulta]) => consulta.startsWith('catalogo'))
const maximo = Math.max(...filas.flatMap(([, datos]) => [datos.base.ejecucionMs, datos.opt.ejecucionMs]))
const barras = filas.map(([consulta, datos], indice) => {
  const y = 99 + indice * 72
  const etiquetas = { catalogo_primera: 'Primera página', catalogo_filtros: 'Página 3 con filtros', catalogo_profunda: 'Página 250' }
  const barra = (ms, dy, color) => {
    const ancho = Math.max(2, ms / maximo * 390)
    const etiqueta = ms.toLocaleString('es-PE', { maximumFractionDigits: 2 })
    return `<rect x="190" y="${y + dy}" width="${ancho}" height="15" fill="${color}"/><text x="${197 + ancho}" y="${y + dy + 12}">${etiqueta} ms</text>`
  }
  return `<text x="22" y="${y + 18}">${etiquetas[consulta]}</text>` +
    barra(datos.base.ejecucionMs, 0, '#8c6546') + barra(datos.opt.ejecucionMs, 20, '#26714c')
}).join('')
const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 700 325" role="img" aria-labelledby="titulo detalle">
<title id="titulo">Tiempo de ejecución SQL del catálogo</title>
<desc id="detalle">Mediana de tres ejecuciones por consulta y variante. Las cifras están en milisegundos.</desc>
<rect width="700" height="325" fill="#f8f7f1"/>
<g font-family="system-ui, sans-serif" font-size="13" fill="#163326">
<text x="22" y="33" font-size="21" font-weight="700">Catálogo PostgreSQL · tiempos de ejecución</text>
<rect x="22" y="54" width="12" height="12" fill="#8c6546"/><text x="40" y="64">Línea base</text>
<rect x="132" y="54" width="12" height="12" fill="#26714c"/><text x="150" y="64">Proyección tras paginar</text>
${barras}
</g></svg>\n`
writeFileSync(`${destino}/postgres-tiempos.svg`, svg)
