import { codigoError, leerJson, obtenerTokenCsrf, esObjeto } from './authApi'

export type DatosVacunacion = {
  nombreVacuna: string
  fechaAplicacion: string
  responsable?: string | null
  lote?: string | null
  observaciones?: string | null
  proximaFecha?: string | null
}
export type SolicitudVacunacion = DatosVacunacion & {
  operationId: string; eventId: string; schemaVersion: 1
}
export type Vacunacion = DatosVacunacion & { eventId: string; creadoEn: string }
export class ErrorSanidad extends Error {
  constructor(public readonly tipo: 'sesion' | 'permiso' | 'seguridad' | 'validacion' | 'conflicto' | 'conexion' | 'temporal') {
    super(tipo)
  }
}
const ruta = (g: string, a: string) => `/api/v1/ganaderias/${encodeURIComponent(g)}/animales/${encodeURIComponent(a)}/vacunaciones`
function comprobarError(r: Response, body: unknown): never {
  if (r.status === 401) throw new ErrorSanidad('sesion')
  if (r.status === 403) throw new ErrorSanidad(['CSRF_INVALIDO', 'ORIGEN_NO_PERMITIDO'].includes(codigoError(body) ?? '') ? 'seguridad' : 'permiso')
  if (r.status === 409) throw new ErrorSanidad('conflicto')
  if ([400, 404, 422].includes(r.status)) throw new ErrorSanidad('validacion')
  throw new ErrorSanidad('temporal')
}
function esVacunacion(v: unknown): v is Vacunacion {
  return esObjeto(v) && typeof v.eventId === 'string' && typeof v.nombreVacuna === 'string'
    && typeof v.fechaAplicacion === 'string' && typeof v.creadoEn === 'string'
    && ['responsable', 'lote', 'observaciones', 'proximaFecha'].every(k => v[k] === null || typeof v[k] === 'string')
}
export async function listarVacunaciones(g: string, a: string, token: string): Promise<Vacunacion[]> {
  const r = await fetch(ruta(g, a), { credentials: 'same-origin', cache: 'no-store', headers: { Authorization: `Bearer ${token}` } })
  const body = await leerJson(r)
  if (r.ok && Array.isArray(body) && body.every(esVacunacion)) return body
  comprobarError(r, body)
}
export async function registrarVacunacion(g: string, a: string, token: string, solicitud: SolicitudVacunacion): Promise<void> {
  for (let intento = 0; intento < 2; intento++) {
    const csrf = await obtenerTokenCsrf()
    const r = await fetch(ruta(g, a), {
      method: 'POST', credentials: 'same-origin', cache: 'no-store',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf },
      body: JSON.stringify(solicitud),
    })
    const body = await leerJson(r)
    if (r.status === 403 && codigoError(body) === 'CSRF_INVALIDO' && intento === 0) continue
    if ([200, 201].includes(r.status) && esObjeto(body) && body.eventId === solicitud.eventId
        && body.operationId === solicitud.operationId && ['APLICADO', 'REPROCESADO'].includes(String(body.resultado))) return
    comprobarError(r, body)
  }
}
