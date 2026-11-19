import { codigoError, ErrorRegistro, esObjeto, leerJson, obtenerTokenCsrf } from './authApi'

export type SexoAnimal = 'MACHO' | 'HEMBRA'
export type CategoriaAnimal = 'TERNERA' | 'VAQUILLA' | 'VACA' | 'TERNERO' | 'TORETE' | 'TORO'

export type Animal = {
  animalId: string
  ganaderiaId: string
  sexo: SexoAnimal
  nombre: string | null
  areteFrente: string | null
  areteReverso: string | null
  raza: string | null
  fechaNacimiento: string | null
  nacimientoEstimado: boolean | null
  categoria: CategoriaAnimal | null
  observaciones: string | null
  estado: 'ACTIVO'
  version: number
  creadoEn: string
}

export type PaginaAnimales = {
  items: Animal[]
  siguienteCursor: string | null
  consultadoEn: string
}

export type SolicitudRegistroAnimal = {
  operationId: string
  animalId: string
  tipo: 'CREAR_ANIMAL'
  schemaVersion: 1
  baseVersion: 0
  sexo: SexoAnimal
  nombre?: string
  areteFrente?: string
  areteReverso?: string
  raza?: string
  fechaNacimiento?: string
  nacimientoEstimado?: boolean
  categoria?: CategoriaAnimal
  observaciones?: string
}

export type ResultadoRegistroAnimal = {
  operationId: string
  animalId: string
  resultado: 'APLICADO' | 'REPROCESADO'
  versionCreada: 1
  confirmadoEn: string
  advertencias: Array<'ARETE_COINCIDENTE'>
}

export type TipoErrorAnimal =
  | 'validacion'
  | 'conflicto'
  | 'permiso'
  | 'seguridad'
  | 'limite'
  | 'sesion'
  | 'noEncontrado'
  | 'conexion'
  | 'servicio'

export class ErrorAnimal extends Error {
  constructor(
    public readonly tipo: TipoErrorAnimal,
    public readonly erroresPorCampo: Record<string, string> = {},
  ) {
    super(tipo)
    this.name = 'ErrorAnimal'
  }
}

function esSexo(valor: unknown): valor is SexoAnimal {
  return valor === 'MACHO' || valor === 'HEMBRA'
}

function esCategoria(valor: unknown): valor is CategoriaAnimal {
  return valor === 'TERNERA' || valor === 'VAQUILLA' || valor === 'VACA'
    || valor === 'TERNERO' || valor === 'TORETE' || valor === 'TORO'
}

function esNullable<T>(valor: unknown, comprobar: (dato: unknown) => dato is T): valor is T | null {
  return valor === null || comprobar(valor)
}

function esAnimal(valor: unknown): valor is Animal {
  return esObjeto(valor)
    && typeof valor.animalId === 'string'
    && typeof valor.ganaderiaId === 'string'
    && esSexo(valor.sexo)
    && esNullable(valor.nombre, dato => typeof dato === 'string')
    && esNullable(valor.areteFrente, dato => typeof dato === 'string')
    && esNullable(valor.areteReverso, dato => typeof dato === 'string')
    && esNullable(valor.raza, dato => typeof dato === 'string')
    && esNullable(valor.fechaNacimiento, dato => typeof dato === 'string')
    && esNullable(valor.nacimientoEstimado, dato => typeof dato === 'boolean')
    && esNullable(valor.categoria, esCategoria)
    && esNullable(valor.observaciones, dato => typeof dato === 'string')
    && valor.estado === 'ACTIVO'
    && typeof valor.version === 'number'
    && Number.isInteger(valor.version)
    && typeof valor.creadoEn === 'string'
    && Number.isFinite(Date.parse(valor.creadoEn))
}

function erroresPorCampo(body: unknown): Record<string, string> {
  if (!esObjeto(body) || !esObjeto(body.erroresPorCampo)) return {}
  return Object.fromEntries(Object.entries(body.erroresPorCampo)
    .filter((entry): entry is [string, string] => typeof entry[1] === 'string'))
}

function convertirError(error: unknown): never {
  if (error instanceof ErrorAnimal) throw error
  if (error instanceof ErrorRegistro) {
    if (error.tipo === 'conexion') throw new ErrorAnimal('conexion')
    if (error.tipo === 'seguridad') throw new ErrorAnimal('seguridad')
    throw new ErrorAnimal('servicio')
  }
  throw new ErrorAnimal('conexion')
}

function interpretarError(response: Response, body: unknown): never {
  if (response.status === 400 || response.status === 413 || response.status === 422) {
    throw new ErrorAnimal('validacion', erroresPorCampo(body))
  }
  if (response.status === 409) throw new ErrorAnimal('conflicto')
  if (response.status === 429) throw new ErrorAnimal('limite')
  if (response.status === 401) throw new ErrorAnimal('sesion')
  if (response.status === 403) {
    const codigo = codigoError(body)
    if (codigo === 'CSRF_INVALIDO' || codigo === 'ORIGEN_NO_PERMITIDO') {
      throw new ErrorAnimal('seguridad')
    }
    throw new ErrorAnimal('permiso')
  }
  if (response.status === 404) throw new ErrorAnimal('noEncontrado')
  throw new ErrorAnimal('servicio')
}

function esPagina(valor: unknown, ganaderiaId: string): valor is PaginaAnimales {
  return esObjeto(valor)
    && Array.isArray(valor.items)
    && valor.items.every(item => esAnimal(item) && item.ganaderiaId === ganaderiaId)
    && (valor.siguienteCursor === null || typeof valor.siguienteCursor === 'string')
    && typeof valor.consultadoEn === 'string'
    && Number.isFinite(Date.parse(valor.consultadoEn))
}

function esResultado(valor: unknown, solicitud: SolicitudRegistroAnimal): valor is ResultadoRegistroAnimal {
  return esObjeto(valor)
    && valor.operationId === solicitud.operationId
    && valor.animalId === solicitud.animalId
    && (valor.resultado === 'APLICADO' || valor.resultado === 'REPROCESADO')
    && valor.versionCreada === 1
    && typeof valor.confirmadoEn === 'string'
    && Number.isFinite(Date.parse(valor.confirmadoEn))
    && Array.isArray(valor.advertencias)
    && valor.advertencias.every(advertencia => advertencia === 'ARETE_COINCIDENTE')
}

export async function listarAnimales(
  ganaderiaId: string,
  accessToken: string,
  cursor?: string,
): Promise<PaginaAnimales> {
  try {
    const consulta = new URLSearchParams({ limit: '20' })
    if (cursor) consulta.set('cursor', cursor)
    const response = await fetch(
      `/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}/animales?${consulta}`,
      {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` },
      },
    )
    const body = await leerJson(response)
    if (response.ok && esPagina(body, ganaderiaId)) return body
    interpretarError(response, body)
  } catch (error) {
    convertirError(error)
  }
}

export async function consultarAnimal(
  ganaderiaId: string,
  animalId: string,
  accessToken: string,
): Promise<Animal> {
  try {
    const response = await fetch(
      `/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}/animales/${encodeURIComponent(animalId)}`,
      {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` },
      },
    )
    const body = await leerJson(response)
    if (response.ok && esAnimal(body)
        && body.ganaderiaId === ganaderiaId && body.animalId === animalId) return body
    interpretarError(response, body)
  } catch (error) {
    convertirError(error)
  }
}

async function enviarRegistro(
  ganaderiaId: string,
  solicitud: SolicitudRegistroAnimal,
  accessToken: string,
  tokenCsrf: string,
) {
  return fetch(`/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}/animales`, {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': tokenCsrf,
    },
    body: JSON.stringify(solicitud),
  })
}

export async function registrarAnimal(
  ganaderiaId: string,
  solicitud: SolicitudRegistroAnimal,
  accessToken: string,
): Promise<ResultadoRegistroAnimal> {
  try {
    let tokenCsrf = await obtenerTokenCsrf()
    let response = await enviarRegistro(ganaderiaId, solicitud, accessToken, tokenCsrf)
    let body = await leerJson(response)
    if (response.status === 403 && codigoError(body) === 'CSRF_INVALIDO') {
      tokenCsrf = await obtenerTokenCsrf()
      response = await enviarRegistro(ganaderiaId, solicitud, accessToken, tokenCsrf)
      body = await leerJson(response)
    }
    if ((response.status === 200 || response.status === 201) && esResultado(body, solicitud)) {
      return body
    }
    interpretarError(response, body)
  } catch (error) {
    convertirError(error)
  }
}
