import { randomBytes, randomUUID } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

const origen = 'http://127.0.0.1:18083'
const ruta = '.cache/rendimiento-sprint2'
const cantidad = Number(process.argv[2] ?? '8')
if (!Number.isInteger(cantidad) || cantidad < 1 || cantidad > 8) {
  throw new Error('La preparación admite de 1 a 8 cuentas sintéticas por corrida.')
}
await mkdir(ruta, { recursive: true })
const archivoClave = join(ruta, 'cuentas.txt')
await writeFile(archivoClave, randomBytes(24).toString('base64url'), { flag: 'wx' })
  .catch(error => { if (error.code !== 'EEXIST') throw error })
const contrasena = (await readFile(archivoClave, 'utf8')).trim()

async function peticion(rutaApi, opciones = {}) {
  return fetch(origen + rutaApi, { redirect: 'error', signal: AbortSignal.timeout(10000), ...opciones })
}

async function escribir(rutaApi, datos, token) {
  const csrf = await peticion('/api/v1/auth/csrf')
  if (!csrf.ok) throw new Error(`CSRF respondió ${csrf.status}.`)
  const cookie = csrf.headers.getSetCookie().find(valor => valor.startsWith('__Host-GC-CSRF='))
  if (!cookie) throw new Error('Falta la cookie CSRF.')
  return peticion(rutaApi, {
    method: 'POST',
    headers: {
      Origin: origen,
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': (await csrf.json()).token,
      Cookie: cookie.split(';', 1)[0],
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(datos),
  })
}

const cuentas = []
for (let indice = 1; indice <= cantidad; indice++) {
  const correo = `rendimiento-${indice}@ganadoconecta.invalid`
  const credenciales = { correo, contrasena }
  let login = await escribir('/api/v1/auth/login', credenciales)
  if (login.status === 401) {
    const registro = await escribir('/api/v1/auth/registro', credenciales)
    if (registro.status !== 201) throw new Error(`Registro sintético respondió ${registro.status}.`)
    login = await escribir('/api/v1/auth/login', credenciales)
  }
  if (!login.ok) throw new Error(`Login sintético respondió ${login.status}.`)
  const { accessToken, expiresAt } = await login.json()
  const respuesta = await peticion('/api/v1/ganaderias', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!respuesta.ok) throw new Error(`Listado sintético respondió ${respuesta.status}.`)
  const listado = await respuesta.json()
  const nombre = `Rendimiento sintético ${indice}`
  let ganaderia = (Array.isArray(listado) ? listado : listado.items).find(item => item.nombre === nombre)
  if (!ganaderia) {
    const alta = await escribir('/api/v1/ganaderias', { id: randomUUID(), nombre }, accessToken)
    if (alta.status !== 201) throw new Error(`Alta de ganadería sintética respondió ${alta.status}.`)
    ganaderia = await alta.json()
  }
  cuentas.push({ ganaderiaId: ganaderia.id, accessToken, expiresAt })
}
await writeFile(join(ruta, 'sesiones.json'), JSON.stringify(cuentas))
console.log(`${cuentas.length} cuentas y ganaderías sintéticas preparadas.`)
