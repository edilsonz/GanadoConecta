import { codigoError, leerJson, obtenerTokenCsrf } from './authApi'

export class ErrorSolicitudPrivada extends Error {
  constructor(readonly estado: number) { super('La solicitud privada no se completó') }
}

export async function solicitudPrivada<T>(ruta: string, token: string,
  metodo = 'GET', datos?: unknown): Promise<T> {
  for (let intento = 0; intento < 2; intento++) {
    const headers = new Headers({ Authorization: `Bearer ${token}` })
    if (metodo !== 'GET') headers.set('X-CSRF-TOKEN', await obtenerTokenCsrf())
    if (datos !== undefined) headers.set('Content-Type', 'application/json')
    const respuesta = await fetch(ruta, {
      method: metodo, headers, credentials: 'same-origin', cache: 'no-store',
      body: datos === undefined ? undefined : JSON.stringify(datos),
    })
    if (respuesta.status === 204) return undefined as T
    const cuerpo = await leerJson(respuesta)
    if (respuesta.ok) return cuerpo as T
    if (intento === 0 && metodo !== 'GET' && respuesta.status === 403
        && codigoError(cuerpo) === 'CSRF_INVALIDO') continue
    throw new ErrorSolicitudPrivada(respuesta.status)
  }
  throw new ErrorSolicitudPrivada(403)
}
