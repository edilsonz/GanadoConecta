export type RegistroExitoso = {
  id: string
  correo: string
  estado: 'ACTIVO'
}

export type TipoErrorRegistro =
  | 'validacion'
  | 'duplicado'
  | 'limite'
  | 'seguridad'
  | 'conexion'
  | 'servicio'

type ErrorApi = {
  codigo?: unknown
  erroresPorCampo?: unknown
}

export class ErrorRegistro extends Error {
  constructor(
    public readonly tipo: TipoErrorRegistro,
    public readonly erroresPorCampo: Record<string, string> = {},
  ) {
    super(tipo)
    this.name = 'ErrorRegistro'
  }
}

async function leerJson(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    return null
  }
}

function esObjeto(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function erroresPorCampo(body: unknown): Record<string, string> {
  if (!esObjeto(body) || !esObjeto(body.erroresPorCampo)) return {}

  return Object.fromEntries(Object.entries(body.erroresPorCampo)
    .filter((entry): entry is [string, string] => typeof entry[1] === 'string'))
}

function codigoError(body: unknown): string | undefined {
  if (!esObjeto(body)) return undefined
  return typeof (body as ErrorApi).codigo === 'string'
    ? (body as ErrorApi).codigo as string
    : undefined
}

async function obtenerTokenCsrf(): Promise<string> {
  const response = await fetch('/api/v1/auth/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { Accept: 'application/json' },
  })
  const body = await leerJson(response)
  if (!response.ok || !esObjeto(body) || typeof body.token !== 'string' || body.token.length === 0) {
    throw new ErrorRegistro('seguridad')
  }
  return body.token
}

function enviarRegistro(correo: string, contrasena: string, tokenCsrf: string) {
  return fetch('/api/v1/auth/registro', {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': tokenCsrf,
    },
    body: JSON.stringify({ correo, contrasena }),
  })
}

async function interpretarRegistro(response: Response, body: unknown): Promise<RegistroExitoso> {
  if (response.status === 201 && esObjeto(body)
      && typeof body.id === 'string' && typeof body.correo === 'string'
      && body.estado === 'ACTIVO') {
    return { id: body.id, correo: body.correo, estado: body.estado }
  }
  if (response.status === 422 || response.status === 400 || response.status === 413) {
    throw new ErrorRegistro('validacion', erroresPorCampo(body))
  }
  if (response.status === 409) throw new ErrorRegistro('duplicado')
  if (response.status === 429) throw new ErrorRegistro('limite')
  if (response.status === 403) throw new ErrorRegistro('seguridad')
  throw new ErrorRegistro('servicio')
}

export async function registrarCuenta(correo: string, contrasena: string): Promise<RegistroExitoso> {
  try {
    let tokenCsrf = await obtenerTokenCsrf()
    let response = await enviarRegistro(correo, contrasena, tokenCsrf)
    let body = await leerJson(response)

    if (response.status === 403 && codigoError(body) === 'CSRF_INVALIDO') {
      tokenCsrf = await obtenerTokenCsrf()
      response = await enviarRegistro(correo, contrasena, tokenCsrf)
      body = await leerJson(response)
    }

    return await interpretarRegistro(response, body)
  } catch (error) {
    if (error instanceof ErrorRegistro) throw error
    throw new ErrorRegistro('conexion')
  }
}
