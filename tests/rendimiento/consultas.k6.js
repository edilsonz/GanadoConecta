import http from 'k6/http'
import { check, sleep } from 'k6'
import { SharedArray } from 'k6/data'
import exec from 'k6/execution'
import { base, catalogo, filtros } from './consultas-sinteticas.mjs'

const modo = __ENV.MODO || 'smoke'
if (!['smoke', 'calentar', 'cien', 'progresiva'].includes(modo)) {
  throw new Error('Modo no reconocido.')
}
const cuentas = new SharedArray('sesiones sintéticas', () =>
  JSON.parse(open('../../.cache/rendimiento-sprint2/sesiones.json')))
const niveles = [10, 50, 100, 250, 500, 1000, 1500, 2000]
const ascenso = 5
const sostenido = 16
const etapa = ascenso + sostenido
const criterios = {
  http_req_failed: [{ threshold: 'rate<0.02', abortOnFail: true, delayAbortEval: '10s' }],
  checks: [{ threshold: 'rate>0.98', abortOnFail: true, delayAbortEval: '10s' }],
}
if (modo === 'cien') {
  criterios['http_req_duration{operacion:animales}'] = ['p(95)<=2000']
  criterios['http_req_duration{operacion:catalogo}'] = ['p(95)<=2000']
}
if (modo === 'progresiva') {
  niveles.forEach((nivel, indice) => {
    const desde = `${indice * etapa + ascenso + 8}s`
    criterios[`http_req_duration{nivel:${nivel}}`] =
      [{ threshold: 'p(95)<4000', abortOnFail: true, delayAbortEval: desde }]
    criterios[`http_req_failed{nivel:${nivel}}`] =
      [{ threshold: 'rate<0.02', abortOnFail: true, delayAbortEval: desde }]
    criterios[`http_reqs{nivel:${nivel}}`] = ['count>=0']
  })
}
const configuraciones = {
  smoke: { executor: 'constant-vus', vus: 2, duration: '5s' },
  calentar: { executor: 'constant-vus', vus: 20, duration: '15s' },
  cien: { executor: 'constant-vus', vus: 100, duration: '25s' },
  progresiva: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '0s',
    stages: niveles.flatMap(target => [{ target, duration: `${ascenso}s` }, { target, duration: `${sostenido}s` }]),
  },
}
export const options = {
  scenarios: { consultas: { ...configuraciones[modo], gracefulStop: '2s' } },
  thresholds: criterios,
  summaryTrendStats: ['min', 'med', 'p(95)', 'p(99)', 'max'],
  discardResponseBodies: false,
  maxRedirects: 0,
}

export function setup() {
  const margen = modo === 'progresiva' ? 200000 : 45000
  if (!cuentas.length || cuentas.some(c => !c.accessToken || Date.parse(c.expiresAt) - Date.now() < margen)) {
    throw new Error('Renueva las sesiones sintéticas antes de medir.')
  }
  const respuesta = http.get(base + '/api/health', { timeout: '3s' })
  if (respuesta.status !== 200 || respuesta.json().status !== 'UP') {
    throw new Error('El backend aislado no está disponible.')
  }
  const cuenta = cuentas[0]
  const animales = http.get(base + `/api/v1/ganaderias/${cuenta.ganaderiaId}/animales?limit=20`, {
    headers: { Authorization: `Bearer ${cuenta.accessToken}` }, timeout: '3s',
  })
  if (animales.status !== 200 || animales.json().items.length !== 20) {
    throw new Error('La semilla de animales o la sesión sintética no es válida.')
  }
}

let cursor
let primerAnimal

export default function () {
  const turno = exec.vu.iterationInScenario
  const id = exec.vu.idInTest
  const cuenta = cuentas[(id - 1) % cuentas.length]
  const esCatalogo = (id + turno) % 2 === 0
  const operacion = esCatalogo ? 'catalogo' : 'animales'
  const segundos = (Date.now() - exec.scenario.startTime) / 1000
  const tramo = niveles[Math.floor(segundos / etapa)]
  const nivel = modo === 'progresiva' && segundos % etapa >= ascenso && tramo ? String(tramo) : 'transicion'
  const tags = { name: operacion, operacion, nivel }
  const [filtro, categoria, ubicacion] = filtros[(id + Math.floor(turno / 2)) % filtros.length]
  const pagina = 1 + ((id + Math.floor(turno / 2)) % 3)
  const url = esCatalogo ? catalogo(pagina, filtro)
    : `/api/v1/ganaderias/${cuenta.ganaderiaId}/animales?limit=20${cursor ? '&cursor=' + encodeURIComponent(cursor) : ''}`
  const r = http.get(base + url, {
    headers: esCatalogo ? {} : { Authorization: `Bearer ${cuenta.accessToken}` },
    timeout: '5s', tags,
  })
  let datos
  if (r.status === 200) {
    try { datos = r.json() } catch { /* Se cuenta como respuesta inválida. */ }
  }
  const items = datos?.items
  const valido = Array.isArray(items) && items.length === 20 && (esCatalogo
    ? datos.siguientePagina === pagina + 1 && new Set(items.map(a => a.id)).size === 20 && items.every(a => a.id
      && typeof a.titulo === 'string' && a.titulo.startsWith('Anuncio sintético ')
      && (!categoria || a.categoria === categoria)
      && (!ubicacion || a.ubicacionGeneral === ubicacion)
      && (!filtro.includes('minimo=') || (a.precioPen >= 1000 && a.precioPen <= 1600))
      && !('ganaderiaId' in a || 'animalId' in a || 'observaciones' in a || 'historial' in a))
    : new Set(items.map(a => a.animalId)).size === 20
      && items.every(a => a.ganaderiaId === cuenta.ganaderiaId)
      && (!cursor || items[0].animalId !== primerAnimal)
      && typeof datos.siguienteCursor === 'string')
  check(r, { 'HTTP 200': respuesta => respuesta.status === 200, 'respuesta íntegra': () => valido }, tags)
  if (!esCatalogo && valido) {
    primerAnimal = cursor ? undefined : items[0].animalId
    cursor = cursor ? undefined : datos.siguienteCursor
  }
  sleep(0.85 + (id % 13) / 20)
}

export function handleSummary(datos) {
  const salida = `k6 ${modo}: ${datos.metrics.iterations?.values.count ?? 0} iteraciones, ` +
    `${datos.metrics.vus_max?.values.max ?? 0} VU configurados\n`
  const archivo = __ENV.RESULTADO
  return archivo ? { stdout: salida, [archivo]: JSON.stringify(datos, null, 2) } : { stdout: salida }
}
