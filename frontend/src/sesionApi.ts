import { codigoError, ErrorRegistro, esObjeto, leerJson, obtenerTokenCsrf } from './authApi'

export type TipoErrorSesion =
  | 'credenciales'
  | 'limite'
  | 'seguridad'
  | 'conexion'
  | 'sesion'
  | 'servicio'

export type SesionActiva = {
  usuarioId: string
  correo?: string
  estado: 'ACTIVO'
  accessToken: string
  expiresAt: string
}

export class ErrorSesion extends Error {
  constructor(public readonly tipo: TipoErrorSesion) {
    super(tipo)
    this.name = 'ErrorSesion'
  }
}

type RespuestaConCuerpo = { response: Response, body: unknown }

let renovacionEnCurso: Promise<SesionActiva> | null = null

function respuestaTokenValida(body: unknown): body is Record<string, unknown> & {
  accessToken: string
  expiresAt: string
} {
  return esObjeto(body)
    && typeof body.accessToken === 'string'
    && body.accessToken.length > 0
    && typeof body.expiresAt === 'string'
    && Number.isFinite(Date.parse(body.expiresAt))
}

async function enviarConCsrf(
  ruta: string,
  inicializacion: Omit<RequestInit, 'headers'> & { headers?: Record<string, string> },
): Promise<RespuestaConCuerpo> {
  let tokenCsrf = await obtenerTokenCsrf()

  async function enviar() {
    const response = await fetch(ruta, {
      ...inicializacion,
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {
        Accept: 'application/json',
        ...inicializacion.headers,
        'X-CSRF-TOKEN': tokenCsrf,
      },
    })
    return { response, body: await leerJson(response) }
  }

  let resultado = await enviar()
  if (resultado.response.status === 403 && codigoError(resultado.body) === 'CSRF_INVALIDO') {
    tokenCsrf = await obtenerTokenCsrf()
    resultado = await enviar()
  }
  return resultado
}

function convertirError(error: unknown): never {
  if (error instanceof ErrorSesion) throw error
  if (error instanceof ErrorRegistro) {
    if (error.tipo === 'seguridad') throw new ErrorSesion('seguridad')
    if (error.tipo === 'conexion') throw new ErrorSesion('conexion')
    throw new ErrorSesion('servicio')
  }
  throw new ErrorSesion('conexion')
}

export async function iniciarSesion(correo: string, contrasena: string): Promise<SesionActiva> {
  try {
    const { response, body } = await enviarConCsrf('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ correo, contrasena }),
    })
    if (response.ok && esObjeto(body) && respuestaTokenValida(body)
        && typeof body.usuarioId === 'string' && typeof body.correo === 'string'
        && body.estado === 'ACTIVO') {
      return {
        usuarioId: body.usuarioId,
        correo: body.correo,
        estado: body.estado,
        accessToken: body.accessToken,
        expiresAt: body.expiresAt,
      }
    }
    if (response.status === 401) throw new ErrorSesion('credenciales')
    if (response.status === 429) throw new ErrorSesion('limite')
    if (response.status === 403) throw new ErrorSesion('seguridad')
    throw new ErrorSesion('servicio')
  } catch (error) {
    convertirError(error)
  }
}

async function renovarSinBloqueo(): Promise<SesionActiva> {
  const { response, body } = await enviarConCsrf('/api/v1/auth/renovar', {
    method: 'POST',
  })
  if (!response.ok || !respuestaTokenValida(body)) {
    if (response.status === 401) throw new ErrorSesion('sesion')
    if (response.status === 403) throw new ErrorSesion('seguridad')
    throw new ErrorSesion('servicio')
  }

  const identidad = await consultarSesion(body.accessToken)
  return {
    ...identidad,
    accessToken: body.accessToken,
    expiresAt: body.expiresAt,
  }
}

async function conBloqueoRenovacion<T>(accion: () => Promise<T>): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request('ganadoconecta-renovacion', { mode: 'exclusive' }, accion)
  }
  return accion()
}

export function renovarSesionControlada(): Promise<SesionActiva> {
  if (renovacionEnCurso) return renovacionEnCurso
  renovacionEnCurso = conBloqueoRenovacion(renovarSinBloqueo)
    .catch(convertirError)
    .finally(() => { renovacionEnCurso = null })
  return renovacionEnCurso
}

export async function consultarSesion(accessToken: string): Promise<Pick<SesionActiva, 'usuarioId' | 'estado'>> {
  try {
    const response = await fetch('/api/v1/auth/me', {
      method: 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    })
    const body = await leerJson(response)
    if (response.ok && esObjeto(body)
        && typeof body.usuarioId === 'string' && body.estado === 'ACTIVO') {
      return { usuarioId: body.usuarioId, estado: body.estado }
    }
    if (response.status === 401) throw new ErrorSesion('sesion')
    throw new ErrorSesion('servicio')
  } catch (error) {
    convertirError(error)
  }
}

export async function cerrarSesionRemota(): Promise<void> {
  try {
    const { response } = await enviarConCsrf('/api/v1/auth/logout', { method: 'POST' })
    if (response.status === 204) return
    if (response.status === 403) throw new ErrorSesion('seguridad')
    throw new ErrorSesion('servicio')
  } catch (error) {
    convertirError(error)
  }
}
