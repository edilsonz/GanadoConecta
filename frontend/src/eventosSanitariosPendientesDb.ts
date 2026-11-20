import type { SolicitudEventoSanitario } from './historialSanitarioApi'
import {
  abrirBaseOffline,
  ALMACEN_EVENTOS_SANITARIOS,
  INDICE_EVENTOS_SANITARIOS_CONTEXTO,
  INDICE_EVENTO_SANITARIO_OPERACION,
  NOMBRE_BASE_OFFLINE,
} from './indexedDbOffline'

export type AmbitoSanitarioLocal = {
  usuarioId: string
  ganaderiaId: string
}

export type EventoSanitarioPendienteLocal = AmbitoSanitarioLocal & {
  operationId: string
  eventId: string
  animalId: string
  payload: SolicitudEventoSanitario
  estado: 'PENDIENTE'
  creadoEnLocal: string
  actualizadoEnLocal: string
}

type RegistroEventoSanitario = EventoSanitarioPendienteLocal & {
  clave: string
  contextoClave: string
}

export class ErrorEventoSanitarioLocal extends Error {
  constructor(public readonly codigo:
    | 'INDEXEDDB_NO_DISPONIBLE'
    | 'DATOS_INVALIDOS'
    | 'OPERACION_CONFLICTIVA'
    | 'TRANSACCION_FALLIDA') {
    super(codigo)
    this.name = 'ErrorEventoSanitarioLocal'
  }
}

type OpcionesAlmacen = {
  indexedDB?: IDBFactory
  nombreBase?: string
  ahora?: () => Date
}

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/
const CLAVES_PAYLOAD = new Set([
  'operationId', 'eventId', 'schemaVersion', 'tipo', 'fecha', 'descripcion',
  'responsable', 'producto', 'indicaciones', 'observaciones',
])

export class AlmacenEventosSanitariosPendientesIndexedDb {
  private readonly factory: IDBFactory | undefined
  private readonly nombreBase: string
  private readonly ahora: () => Date
  private apertura?: Promise<IDBDatabase>

  constructor(opciones: OpcionesAlmacen = {}) {
    this.factory = opciones.indexedDB ?? globalThis.indexedDB
    this.nombreBase = opciones.nombreBase ?? NOMBRE_BASE_OFFLINE
    this.ahora = opciones.ahora ?? (() => new Date())
  }

  async guardarPendiente(
    ambito: AmbitoSanitarioLocal,
    animalId: string,
    recibido: SolicitudEventoSanitario,
  ): Promise<EventoSanitarioPendienteLocal> {
    validarUuid(ambito.usuarioId)
    validarUuid(ambito.ganaderiaId)
    validarUuid(animalId)
    const payload = validarPayload(recibido)
    const db = await this.abrir()

    return new Promise((resolve, reject) => {
      const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readwrite')
      const almacen = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      let resultado: RegistroEventoSanitario | undefined
      let error: ErrorEventoSanitarioLocal | undefined

      tx.oncomplete = () => {
        if (error) reject(error)
        else if (resultado) resolve(exponer(resultado))
        else reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      }
      tx.onerror = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      tx.onabort = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))

      const consulta = almacen.index(INDICE_EVENTO_SANITARIO_OPERACION)
        .get(payload.operationId)
      consulta.onsuccess = () => {
        const existente = consulta.result as RegistroEventoSanitario | undefined
        if (existente) {
          if (existente.usuarioId === ambito.usuarioId
              && existente.ganaderiaId === ambito.ganaderiaId
              && existente.animalId === animalId
              && mismaSolicitud(existente.payload, payload)) {
            resultado = existente
          } else {
            error = new ErrorEventoSanitarioLocal('OPERACION_CONFLICTIVA')
          }
          return
        }

        const instante = this.ahora().toISOString()
        const registro: RegistroEventoSanitario = {
          ...ambito,
          animalId,
          operationId: payload.operationId,
          eventId: payload.eventId,
          payload,
          estado: 'PENDIENTE',
          creadoEnLocal: instante,
          actualizadoEnLocal: instante,
          clave: `${ambito.usuarioId}:${ambito.ganaderiaId}:${payload.operationId}`,
          contextoClave: `${ambito.usuarioId}:${ambito.ganaderiaId}`,
        }
        almacen.add(registro).onsuccess = () => { resultado = registro }
      }
    })
  }

  async listarPendientes(
    ambito: AmbitoSanitarioLocal,
  ): Promise<EventoSanitarioPendienteLocal[]> {
    validarUuid(ambito.usuarioId)
    validarUuid(ambito.ganaderiaId)
    const db = await this.abrir()
    const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readonly')
    const solicitud = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      .index(INDICE_EVENTOS_SANITARIOS_CONTEXTO)
      .getAll(`${ambito.usuarioId}:${ambito.ganaderiaId}`)
    const registros = await resultadoSolicitud<RegistroEventoSanitario[]>(solicitud)
    await completarTransaccion(tx)
    return registros
      .sort((a, b) => a.creadoEnLocal.localeCompare(b.creadoEnLocal))
      .map(exponer)
  }

  async cerrar(): Promise<void> {
    if (this.apertura) (await this.apertura).close()
    this.apertura = undefined
  }

  private abrir(): Promise<IDBDatabase> {
    if (!this.factory) {
      return Promise.reject(new ErrorEventoSanitarioLocal('INDEXEDDB_NO_DISPONIBLE'))
    }
    if (!this.apertura) this.apertura = abrirBaseOffline(this.factory, this.nombreBase)
    return this.apertura
  }
}

function validarUuid(valor: string): void {
  if (!UUID_V4.test(valor)) throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
}

function validarPayload(recibido: SolicitudEventoSanitario): SolicitudEventoSanitario {
  if (!recibido || Object.keys(recibido).some(clave => !CLAVES_PAYLOAD.has(clave))) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  validarUuid(recibido.operationId)
  validarUuid(recibido.eventId)
  if (recibido.schemaVersion !== 1
      || !['DESPARASITACION', 'TRATAMIENTO_SANITARIO', 'REVISION_VETERINARIA']
        .includes(recibido.tipo)
      || !FECHA_ISO.test(recibido.fecha)
      || !textoValido(recibido.descripcion, 1000, true)) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  const opcionales: Array<[string | undefined, number]> = [
    [recibido.responsable, 150],
    [recibido.producto, 150],
    [recibido.indicaciones, 1000],
    [recibido.observaciones, 2000],
  ]
  if (opcionales.some(([valor, maximo]) => valor !== undefined
      && !textoValido(valor, maximo, false))) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  return structuredClone(recibido)
}

function textoValido(valor: string | undefined, maximo: number, requerido: boolean): boolean {
  if (valor === undefined) return !requerido
  const limpio = valor.trim()
  return (!requerido || limpio.length > 0)
    && Array.from(limpio).length <= maximo
    && !/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]|\p{Cf}/u.test(limpio)
}

function mismaSolicitud(a: SolicitudEventoSanitario, b: SolicitudEventoSanitario): boolean {
  return a.operationId === b.operationId && a.eventId === b.eventId
    && a.schemaVersion === b.schemaVersion && a.tipo === b.tipo
    && a.fecha === b.fecha && a.descripcion === b.descripcion
    && a.responsable === b.responsable && a.producto === b.producto
    && a.indicaciones === b.indicaciones && a.observaciones === b.observaciones
}

function exponer(registro: RegistroEventoSanitario): EventoSanitarioPendienteLocal {
  const { clave: _clave, contextoClave: _contexto, ...dato } = structuredClone(registro)
  return dato
}

function resultadoSolicitud<T>(solicitud: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    solicitud.onsuccess = () => resolve(solicitud.result)
    solicitud.onerror = () => reject(solicitud.error)
  })
}

function completarTransaccion(tx: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
    tx.onabort = () => reject(tx.error)
  })
}
