import type {
  ResultadoEventoSanitario,
  SolicitudEventoSanitario,
} from '../features/sanidad/historialSanitarioApi'
import type {
  ResultadoVacunacion,
  SolicitudVacunacion,
} from '../features/sanidad/vacunacionesApi'
import type {
  CodigoErrorSeguroOperacion,
  EstadoOperacionLocal,
} from './operacionesPendientesDb'
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

export type ClaseOperacionSanitaria = 'VACUNACION' | 'EVENTO_SANITARIO'
export type SolicitudSanitariaLocal = SolicitudVacunacion | SolicitudEventoSanitario
export type ConfirmacionSanitariaLocal = ResultadoVacunacion | ResultadoEventoSanitario

export type EventoSanitarioPendienteLocal = AmbitoSanitarioLocal & {
  clase: ClaseOperacionSanitaria
  operationId: string
  eventId: string
  animalId: string
  payload: SolicitudSanitariaLocal
  estado: EstadoOperacionLocal
  ultimoError: CodigoErrorSeguroOperacion | null
  creadoEnLocal: string
  actualizadoEnLocal: string
  intentos: number
  leaseId: string | null
  leaseHasta: string | null
  confirmacion: ConfirmacionSanitariaLocal | null
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
    | 'OPERACION_NO_ENCONTRADA'
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
const ESTADOS = new Set<EstadoOperacionLocal>([
  'PENDIENTE', 'SINCRONIZANDO', 'ERROR_REINTENTABLE', 'REQUIERE_SESION',
  'RECHAZADO', 'CONFLICTO', 'SINCRONIZADO',
])
const ERRORES = new Set<CodigoErrorSeguroOperacion>([
  'RED', 'SERVIDOR', 'AUTENTICACION', 'AUTORIZACION', 'VALIDACION',
  'CONFLICTO', 'PROTOCOLO', 'ALMACENAMIENTO',
])
const CLAVES_EVENTO = [
  'operationId', 'eventId', 'schemaVersion', 'tipo', 'fecha', 'descripcion',
  'responsable', 'producto', 'indicaciones', 'observaciones',
] as const
const CLAVES_VACUNACION = [
  'operationId', 'eventId', 'schemaVersion', 'nombreVacuna', 'fechaAplicacion',
  'responsable', 'lote', 'observaciones', 'proximaFecha',
] as const

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
    recibido: SolicitudSanitariaLocal,
  ): Promise<EventoSanitarioPendienteLocal> {
    validarAmbito(ambito)
    validarUuid(animalId)
    const payload = validarPayload(recibido)
    const clase = clasePayload(payload)
    const db = await this.abrir()

    return new Promise((resolve, reject) => {
      const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readwrite')
      const almacen = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      let resultado: RegistroEventoSanitario | undefined
      let error: ErrorEventoSanitarioLocal | undefined
      tx.oncomplete = () => error ? reject(error)
        : resultado ? resolve(exponer(resultado))
          : reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      tx.onerror = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      tx.onabort = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))

      const consulta = almacen.index(INDICE_EVENTO_SANITARIO_OPERACION)
        .get(payload.operationId)
      consulta.onsuccess = () => {
        const existenteCrudo = consulta.result as RegistroEventoSanitario | undefined
        if (existenteCrudo) {
          const existente = normalizar(existenteCrudo)
          if (existente.usuarioId === ambito.usuarioId
              && existente.ganaderiaId === ambito.ganaderiaId
              && existente.animalId === animalId
              && existente.clase === clase
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
          clase,
          animalId,
          operationId: payload.operationId,
          eventId: payload.eventId,
          payload,
          estado: 'PENDIENTE',
          ultimoError: null,
          creadoEnLocal: instante,
          actualizadoEnLocal: instante,
          intentos: 0,
          leaseId: null,
          leaseHasta: null,
          confirmacion: null,
          clave: claveOperacion(ambito, payload.operationId),
          contextoClave: claveContexto(ambito),
        }
        almacen.add(registro).onsuccess = () => { resultado = registro }
      }
    })
  }

  async listarPendientes(
    ambito: AmbitoSanitarioLocal,
  ): Promise<EventoSanitarioPendienteLocal[]> {
    validarAmbito(ambito)
    const db = await this.abrir()
    const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readonly')
    const solicitud = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      .index(INDICE_EVENTOS_SANITARIOS_CONTEXTO)
      .getAll(claveContexto(ambito))
    const registros = await resultadoSolicitud<RegistroEventoSanitario[]>(solicitud)
    await completarTransaccion(tx)
    return registros.map(normalizar)
      .filter(registro => registro.estado !== 'SINCRONIZADO')
      .sort((a, b) => a.creadoEnLocal.localeCompare(b.creadoEnLocal))
      .map(exponer)
  }

  async obtener(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
  ): Promise<EventoSanitarioPendienteLocal | null> {
    validarAmbito(ambito)
    validarUuid(operationId)
    const db = await this.abrir()
    const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readonly')
    const solicitud = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      .get(claveOperacion(ambito, operationId))
    const registro = await resultadoSolicitud<RegistroEventoSanitario | undefined>(solicitud)
    await completarTransaccion(tx)
    return registro ? exponer(normalizar(registro)) : null
  }

  async actualizarEstado(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
    estado: EstadoOperacionLocal,
    ultimoError: CodigoErrorSeguroOperacion | null = null,
  ): Promise<EventoSanitarioPendienteLocal> {
    validarEstado(estado, ultimoError)
    return (await this.mutar(ambito, operationId, registro => ({
      ...registro,
      estado,
      ultimoError,
      leaseId: null,
      leaseHasta: null,
      actualizadoEnLocal: this.ahora().toISOString(),
    })))!
  }

  async reclamarParaSincronizar(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
    leaseId: string,
    duracionLeaseMs = 60_000,
  ): Promise<EventoSanitarioPendienteLocal | null> {
    validarUuid(leaseId)
    if (!Number.isInteger(duracionLeaseMs) || duracionLeaseMs < 1_000) {
      throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
    }
    return (await this.mutar(ambito, operationId, registro => {
      const instante = this.ahora()
      const leaseVigente = registro.estado === 'SINCRONIZANDO'
        && registro.leaseHasta !== null
        && Date.parse(registro.leaseHasta) > instante.getTime()
      const recuperable = esRecuperable(registro)
        || (registro.estado === 'SINCRONIZANDO' && !leaseVigente)
      if (!recuperable || leaseVigente) return null
      return {
        ...registro,
        estado: 'SINCRONIZANDO',
        ultimoError: null,
        intentos: registro.intentos + 1,
        leaseId,
        leaseHasta: new Date(instante.getTime() + duracionLeaseMs).toISOString(),
        actualizadoEnLocal: instante.toISOString(),
      }
    }, true))
  }

  async finalizarSincronizacion(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
    leaseId: string,
    confirmacion: ConfirmacionSanitariaLocal,
  ): Promise<EventoSanitarioPendienteLocal> {
    validarUuid(leaseId)
    validarConfirmacion(confirmacion, operationId)
    return (await this.mutar(ambito, operationId, registro => {
      validarLease(registro, leaseId)
      if (confirmacion.eventId !== registro.eventId) {
        throw new ErrorEventoSanitarioLocal('OPERACION_CONFLICTIVA')
      }
      return {
        ...registro,
        estado: 'SINCRONIZADO',
        ultimoError: null,
        leaseId: null,
        leaseHasta: null,
        confirmacion,
        actualizadoEnLocal: this.ahora().toISOString(),
      }
    }))!
  }

  async fallarSincronizacion(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
    leaseId: string,
    estado: Exclude<EstadoOperacionLocal, 'PENDIENTE' | 'SINCRONIZANDO' | 'SINCRONIZADO'>,
    ultimoError: CodigoErrorSeguroOperacion,
  ): Promise<EventoSanitarioPendienteLocal> {
    validarUuid(leaseId)
    validarEstado(estado, ultimoError)
    return (await this.mutar(ambito, operationId, registro => {
      validarLease(registro, leaseId)
      return {
        ...registro,
        estado,
        ultimoError,
        leaseId: null,
        leaseHasta: null,
        confirmacion: null,
        actualizadoEnLocal: this.ahora().toISOString(),
      }
    }))!
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

  private async mutar(
    ambito: AmbitoSanitarioLocal,
    operationId: string,
    cambio: (registro: RegistroEventoSanitario) => RegistroEventoSanitario | null,
    permiteOcupada = false,
  ): Promise<EventoSanitarioPendienteLocal | null> {
    validarAmbito(ambito)
    validarUuid(operationId)
    const db = await this.abrir()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readwrite')
      const almacen = tx.objectStore(ALMACEN_EVENTOS_SANITARIOS)
      let resultado: RegistroEventoSanitario | null | undefined
      let error: unknown
      tx.oncomplete = () => {
        if (error) reject(error)
        else if (resultado === null && permiteOcupada) resolve(null)
        else if (resultado) resolve(exponer(resultado))
        else reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      }
      tx.onerror = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      tx.onabort = () => reject(new ErrorEventoSanitarioLocal('TRANSACCION_FALLIDA'))
      const consulta = almacen.get(claveOperacion(ambito, operationId))
      consulta.onsuccess = () => {
        const encontrado = consulta.result as RegistroEventoSanitario | undefined
        if (!encontrado) {
          error = new ErrorEventoSanitarioLocal('OPERACION_NO_ENCONTRADA')
          return
        }
        try {
          resultado = cambio(normalizar(encontrado))
          if (resultado) almacen.put(resultado)
        } catch (fallo) {
          error = fallo
        }
      }
    })
  }
}

function validarAmbito(ambito: AmbitoSanitarioLocal): void {
  validarUuid(ambito.usuarioId)
  validarUuid(ambito.ganaderiaId)
}

function validarUuid(valor: string): void {
  if (typeof valor !== 'string' || !UUID_V4.test(valor)) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
}

function validarPayload(recibido: SolicitudSanitariaLocal): SolicitudSanitariaLocal {
  if (!recibido || typeof recibido !== 'object') {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  validarUuid(recibido.operationId)
  validarUuid(recibido.eventId)
  if (recibido.schemaVersion !== 1) throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  return 'nombreVacuna' in recibido
    ? validarVacunacion(recibido)
    : validarEvento(recibido)
}

function validarVacunacion(recibido: SolicitudVacunacion): SolicitudVacunacion {
  validarClaves(recibido, CLAVES_VACUNACION)
  if (!textoValido(recibido.nombreVacuna, 150, true)
      || !FECHA_ISO.test(recibido.fechaAplicacion)
      || (recibido.proximaFecha !== undefined && recibido.proximaFecha !== null
        && (!FECHA_ISO.test(recibido.proximaFecha)
          || recibido.proximaFecha < recibido.fechaAplicacion))
      || !opcionalesValidos([
        [recibido.responsable, 150], [recibido.lote, 100],
        [recibido.observaciones, 2000],
      ])) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  return copiarCanonico(recibido, CLAVES_VACUNACION) as SolicitudVacunacion
}

function validarEvento(recibido: SolicitudEventoSanitario): SolicitudEventoSanitario {
  validarClaves(recibido, CLAVES_EVENTO)
  if (!['DESPARASITACION', 'TRATAMIENTO_SANITARIO', 'REVISION_VETERINARIA']
    .includes(recibido.tipo)
      || !FECHA_ISO.test(recibido.fecha)
      || !textoValido(recibido.descripcion, 1000, true)
      || !opcionalesValidos([
        [recibido.responsable, 150], [recibido.producto, 150],
        [recibido.indicaciones, 1000], [recibido.observaciones, 2000],
      ])) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
  return copiarCanonico(recibido, CLAVES_EVENTO) as SolicitudEventoSanitario
}

function validarClaves(
  recibido: object,
  permitidas: readonly string[],
): void {
  if (Object.keys(recibido).some(clave => !permitidas.includes(clave))) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
}

function copiarCanonico(
  recibido: object,
  claves: readonly string[],
): Record<string, unknown> {
  const origen = recibido as unknown as Record<string, unknown>
  const copia: Record<string, unknown> = {}
  for (const clave of claves) {
    if (Object.prototype.hasOwnProperty.call(origen, clave)) {
      copia[clave] = structuredClone(origen[clave])
    }
  }
  return copia
}

function opcionalesValidos(valores: Array<[string | null | undefined, number]>): boolean {
  return valores.every(([valor, maximo]) => valor === undefined || valor === null
    || textoValido(valor, maximo, false))
}

function textoValido(valor: string, maximo: number, requerido: boolean): boolean {
  if (typeof valor !== 'string') return false
  const limpio = valor.trim()
  return (!requerido || limpio.length > 0)
    && Array.from(limpio).length <= maximo
    && !/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]|\p{Cf}/u.test(limpio)
}

function clasePayload(payload: SolicitudSanitariaLocal): ClaseOperacionSanitaria {
  return 'nombreVacuna' in payload ? 'VACUNACION' : 'EVENTO_SANITARIO'
}

function mismaSolicitud(a: SolicitudSanitariaLocal, b: SolicitudSanitariaLocal): boolean {
  return JSON.stringify(validarPayload(a)) === JSON.stringify(validarPayload(b))
}

function validarEstado(
  estado: EstadoOperacionLocal,
  error: CodigoErrorSeguroOperacion | null,
): void {
  if (!ESTADOS.has(estado) || (error !== null && !ERRORES.has(error))) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
}

function validarConfirmacion(
  confirmacion: ConfirmacionSanitariaLocal,
  operationId: string,
): void {
  if (!confirmacion || confirmacion.operationId !== operationId
      || !UUID_V4.test(confirmacion.eventId)
      || !['APLICADO', 'REPROCESADO'].includes(confirmacion.resultado)
      || !Number.isFinite(Date.parse(confirmacion.confirmadoEn))) {
    throw new ErrorEventoSanitarioLocal('DATOS_INVALIDOS')
  }
}

function validarLease(registro: RegistroEventoSanitario, leaseId: string): void {
  if (registro.estado !== 'SINCRONIZANDO' || registro.leaseId !== leaseId) {
    throw new ErrorEventoSanitarioLocal('OPERACION_CONFLICTIVA')
  }
}

function esRecuperable(registro: RegistroEventoSanitario): boolean {
  return registro.estado === 'PENDIENTE'
    || registro.estado === 'ERROR_REINTENTABLE'
    || registro.estado === 'REQUIERE_SESION'
    || (registro.estado === 'RECHAZADO' && registro.ultimoError === 'AUTORIZACION')
}

function normalizar(registro: RegistroEventoSanitario): RegistroEventoSanitario {
  const payload = validarPayload(registro.payload)
  return {
    ...registro,
    clase: registro.clase === 'VACUNACION' || registro.clase === 'EVENTO_SANITARIO'
      ? registro.clase : clasePayload(payload),
    payload,
    estado: ESTADOS.has(registro.estado) ? registro.estado : 'PENDIENTE',
    ultimoError: registro.ultimoError && ERRORES.has(registro.ultimoError)
      ? registro.ultimoError : null,
    intentos: Number.isInteger(registro.intentos) && registro.intentos >= 0
      ? registro.intentos : 0,
    leaseId: typeof registro.leaseId === 'string' ? registro.leaseId : null,
    leaseHasta: typeof registro.leaseHasta === 'string' ? registro.leaseHasta : null,
    confirmacion: registro.confirmacion ?? null,
  }
}

function exponer(registro: RegistroEventoSanitario): EventoSanitarioPendienteLocal {
  const { clave: _clave, contextoClave: _contexto, ...dato } = structuredClone(registro)
  return dato
}

function claveContexto(ambito: AmbitoSanitarioLocal): string {
  return `${ambito.usuarioId}:${ambito.ganaderiaId}`
}

function claveOperacion(ambito: AmbitoSanitarioLocal, operationId: string): string {
  return `${claveContexto(ambito)}:${operationId}`
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
