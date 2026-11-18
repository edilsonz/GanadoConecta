import { codigoError, ErrorRegistro, esObjeto, leerJson, obtenerTokenCsrf } from './authApi'

export type RolGanaderia = 'PROPIETARIO' | 'EDITOR' | 'LECTOR'

export type Ganaderia = {
  id: string
  nombre: string
  estado: 'ACTIVA'
  rol: RolGanaderia
}

export type TipoErrorGanaderia =
  | 'validacion'
  | 'conflicto'
  | 'seguridad'
  | 'sesion'
  | 'conexion'
  | 'servicio'

export class ErrorGanaderia extends Error {
  constructor(public readonly tipo: TipoErrorGanaderia) {
    super(tipo)
    this.name = 'ErrorGanaderia'
  }
}

function esGanaderia(valor: unknown): valor is Ganaderia {
  return esObjeto(valor)
    && typeof valor.id === 'string'
    && typeof valor.nombre === 'string'
    && valor.estado === 'ACTIVA'
    && (valor.rol === 'PROPIETARIO' || valor.rol === 'EDITOR' || valor.rol === 'LECTOR')
}

function convertirError(error: unknown): never {
  if (error instanceof ErrorGanaderia) throw error
  if (error instanceof ErrorRegistro) {
    if (error.tipo === 'seguridad') throw new ErrorGanaderia('seguridad')
    if (error.tipo === 'conexion') throw new ErrorGanaderia('conexion')
    throw new ErrorGanaderia('servicio')
  }
  throw new ErrorGanaderia('conexion')
}

export async function listarGanaderias(accessToken: string): Promise<Ganaderia[]> {
  try {
    const response = await fetch('/api/v1/ganaderias', {
      method: 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    })
    const body = await leerJson(response)
    if (response.ok && Array.isArray(body) && body.every(esGanaderia)) return body
    if (response.status === 401) throw new ErrorGanaderia('sesion')
    if (response.status === 403) throw new ErrorGanaderia('seguridad')
    throw new ErrorGanaderia('servicio')
  } catch (error) {
    convertirError(error)
  }
}

async function enviarCreacion(
  id: string,
  nombre: string,
  accessToken: string,
  tokenCsrf: string,
) {
  return fetch('/api/v1/ganaderias', {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': tokenCsrf,
    },
    body: JSON.stringify({ id, nombre }),
  })
}

export async function crearGanaderia(nombre: string, accessToken: string): Promise<Ganaderia> {
  const id = crypto.randomUUID()
  try {
    let tokenCsrf = await obtenerTokenCsrf()
    let response = await enviarCreacion(id, nombre, accessToken, tokenCsrf)
    let body = await leerJson(response)

    if (response.status === 403 && codigoError(body) === 'CSRF_INVALIDO') {
      tokenCsrf = await obtenerTokenCsrf()
      response = await enviarCreacion(id, nombre, accessToken, tokenCsrf)
      body = await leerJson(response)
    }

    if ((response.status === 200 || response.status === 201) && esGanaderia(body)) return body
    if (response.status === 400 || response.status === 413 || response.status === 422) {
      throw new ErrorGanaderia('validacion')
    }
    if (response.status === 409) throw new ErrorGanaderia('conflicto')
    if (response.status === 401) throw new ErrorGanaderia('sesion')
    if (response.status === 403) throw new ErrorGanaderia('seguridad')
    throw new ErrorGanaderia('servicio')
  } catch (error) {
    convertirError(error)
  }
}
