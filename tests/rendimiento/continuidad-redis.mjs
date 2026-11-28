import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { readFile, writeFile } from 'node:fs/promises'
import { base } from './consultas-sinteticas.mjs'

const temporal = '.cache/rendimiento-sprint2'
const [cuenta] = JSON.parse(await readFile(`${temporal}/sesiones.json`, 'utf8'))
assert.ok(Date.parse(cuenta.expiresAt) > Date.now())

async function consultar(ruta, estado, { method = 'GET', headers = {}, body } = {}) {
  const respuesta = await fetch(base + ruta, { method, body, redirect: 'error',
    signal: AbortSignal.timeout(5000), headers: { Authorization: `Bearer ${cuenta.accessToken}`, ...headers } })
  assert.equal(respuesta.status, estado)
  return respuesta
}

const ganaderias = await (await consultar('/api/v1/ganaderias', 200)).json()
const finca = (Array.isArray(ganaderias) ? ganaderias : ganaderias.items).find(x => x.id === cuenta.ganaderiaId)
assert.match(finca?.nombre ?? '', /^Rendimiento sintético \d+$/)
const ruta = `/api/v1/ganaderias/${cuenta.ganaderiaId}/animales`
const lista = await (await consultar(ruta, 200)).json()
const animal = lista.items.find(x => x.nombre?.startsWith('Ejemplar sintético '))
assert.ok(animal)
const historial = await (await consultar(`${ruta}/${animal.animalId}/eventos-sanitarios`, 200)).json()
assert.ok(historial.length >= 4 && historial.every(x => x.tipo === 'REVISION_VETERINARIA'))

const csrf = await consultar('/api/v1/auth/csrf', 200)
const cookie = csrf.headers.getSetCookie().find(x => x.startsWith('__Host-GC-CSRF='))
assert.ok(cookie)
const animalId = randomUUID()
await consultar(ruta, 201, { method: 'POST',
  headers: { Origin: base, 'Content-Type': 'application/json', Cookie: cookie.split(';', 1)[0],
    'X-CSRF-TOKEN': (await csrf.json()).token },
  body: JSON.stringify({ animalId, operationId: randomUUID(), tipo: 'CREAR_ANIMAL',
    schemaVersion: 2, baseVersion: 0, sexo: 'HEMBRA', nombre: 'Continuidad Redis sintética' }) })
assert.equal((await (await consultar(`${ruta}/${animalId}`, 200)).json()).animalId, animalId)
const catalogo = await (await consultar('/api/v1/catalogo', 200)).json()
assert.ok(catalogo.items.length === 20 && catalogo.items.every(x => x.titulo.startsWith('Anuncio sintético ')))

const resultado = { altaHttp: 201, confirmacionHttp: 200, historialHttp: 200, catalogoHttp: 200 }
await writeFile(`${temporal}/cierre-continuidad-redis.json`, JSON.stringify(resultado, null, 2) + '\n')
console.log(JSON.stringify(resultado))
