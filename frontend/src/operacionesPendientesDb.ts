import type {
  ResultadoRegistroAnimal,
  SolicitudRegistroAnimalCompatible,
} from './animalesApi'
import {
  abrirBaseOffline,
  ALMACEN_OPERACIONES,
  INDICE_ANIMAL_PENDIENTE,
  INDICE_OPERACION,
  INDICE_OPERACIONES_CONTEXTO,
  NOMBRE_BASE_OFFLINE,
} from './indexedDbOffline'

const CLAVES_PAYLOAD = new Set([
  'operationId', 'animalId', 'tipo', 'schemaVersion', 'baseVersion', 'sexo',
  'nombre', 'areteFrente', 'areteReverso', 'coloresIdentificacionTextil',
  'ubicacionIdentificacionTextil', 'raza', 'fechaNacimiento',
  'nacimientoEstimado', 'categoria', 'observaciones',
])

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/
const CONTROLES_SIN_SALTOS = /[\u0000-\u001F\u007F-\u009F]|\p{Cf}/u
const CONTROLES_CON_SALTOS = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]|\p{Cf}/u
const CATEGORIAS = new Set([
  'TERNERA', 'VAQUILLA', 'VACA', 'TERNERO', 'TORETE', 'TORO',
])
const ESTADOS = new Set<EstadoOperacionLocal>([
  'PENDIENTE', 'SINCRONIZANDO', 'ERROR_REINTENTABLE', 'REQUIERE_SESION',
  'RECHAZADO', 'CONFLICTO', 'SINCRONIZADO',
])
const ERRORES_SEGUROS = new Set<CodigoErrorSeguroOperacion>([
  'RED', 'SERVIDOR', 'AUTENTICACION', 'AUTORIZACION', 'VALIDACION',
  'CONFLICTO', 'PROTOCOLO', 'ALMACENAMIENTO',
])

export type EstadoOperacionLocal =
  | 'PENDIENTE'
  | 'SINCRONIZANDO'
  | 'ERROR_REINTENTABLE'
  | 'REQUIERE_SESION'
  | 'RECHAZADO'
  | 'CONFLICTO'
  | 'SINCRONIZADO'

export type CodigoErrorSeguroOperacion =
  | 'RED'
  | 'SERVIDOR'
  | 'AUTENTICACION'
  | 'AUTORIZACION'
  | 'VALIDACION'
  | 'CONFLICTO'
  | 'PROTOCOLO'
  | 'ALMACENAMIENTO'

export type AmbitoOperacionLocal = {
  usuarioId: string
  ganaderiaId: string
}

export type SolicitudRegistroAnimalLocal = SolicitudRegistroAnimalCompatible

export type OperacionPendienteLocal = AmbitoOperacionLocal & {
  operationId: string
  animalId: string
  payload: SolicitudRegistroAnimalLocal
  estado: EstadoOperacionLocal
  ultimoError: CodigoErrorSeguroOperacion | null
  creadoEnLocal: string
  actualizadoEnLocal: string
  intentos: number
  leaseId: string | null
  leaseHasta: string | null
  confirmacion: ResultadoRegistroAnimal | null
}

type RegistroOperacion = OperacionPendienteLocal & {
  clave: string
  contextoClave: string
  animalClave: string
}

export type CodigoErrorAlmacenamientoLocal =
  | 'INDEXEDDB_NO_DISPONIBLE'
  | 'APERTURA_FALLIDA'
  | 'TRANSACCION_FALLIDA'
  | 'DATOS_INVALIDOS'
  | 'OPERACION_CONFLICTIVA'
  | 'OPERACION_NO_ENCONTRADA'

export class ErrorAlmacenamientoLocal extends Error {
  constructor(public readonly codigo: CodigoErrorAlmacenamientoLocal) {
    super(codigo)
    this.name = 'ErrorAlmacenamientoLocal'
  }
}

type OpcionesAlmacen = {
  indexedDB?: IDBFactory
  nombreBase?: string
  ahora?: () => Date
}

export class AlmacenOperacionesPendientesIndexedDb {
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
    ambito: AmbitoOperacionLocal,
    payloadRecibido: SolicitudRegistroAnimalLocal,
  ): Promise<OperacionPendienteLocal> {
    const ambitoValidado = validarAmbito(ambito)
    const payload = validarYCopiarPayload(payloadRecibido)
    const db = await this.abrir()
    try {
      const registro = await guardarEnTransaccion(
        db,
        ambitoValidado,
        payload,
        this.ahora,
      )
      return exponer(registro)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async listarPendientes(ambito: AmbitoOperacionLocal): Promise<OperacionPendienteLocal[]> {
    const ambitoValidado = validarAmbito(ambito)
    const db = await this.abrir()
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readonly')
    const completada = esperarTransaccion(transaccion)
    try {
      const registros = await esperarSolicitud<RegistroOperacion[]>(
        transaccion.objectStore(ALMACEN_OPERACIONES)
          .index(INDICE_OPERACIONES_CONTEXTO).getAll(claveContexto(ambitoValidado)),
      )
      await completada
      return registros
        .filter(registro => registro.estado !== 'SINCRONIZADO')
        .sort((a, b) => a.creadoEnLocal.localeCompare(b.creadoEnLocal))
        .map(exponer)
    } catch (error) {
      await consumirFalloTransaccion(completada)
      throw traducirError(error)
    }
  }

  async obtener(
    ambito: AmbitoOperacionLocal,
    operationId: string,
  ): Promise<OperacionPendienteLocal | null> {
    const ambitoValidado = validarAmbito(ambito)
    validarUuid(operationId)
    const db = await this.abrir()
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readonly')
    const completada = esperarTransaccion(transaccion)
    try {
      const registro = await esperarSolicitud<RegistroOperacion | undefined>(
        transaccion.objectStore(ALMACEN_OPERACIONES)
          .get(claveOperacion(ambitoValidado, operationId)),
      )
      await completada
      return registro ? exponer(registro) : null
    } catch (error) {
      await consumirFalloTransaccion(completada)
      throw traducirError(error)
    }
  }

  async actualizarEstado(
    ambito: AmbitoOperacionLocal,
    operationId: string,
    estado: EstadoOperacionLocal,
    ultimoError: CodigoErrorSeguroOperacion | null = null,
  ): Promise<OperacionPendienteLocal> {
    const ambitoValidado = validarAmbito(ambito)
    validarUuid(operationId)
    if (!ESTADOS.has(estado)) {
      throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
    }
    if (ultimoError !== null && !ERRORES_SEGUROS.has(ultimoError)) {
      throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
    }
    const db = await this.abrir()
    try {
      const actualizado = await actualizarEnTransaccion(
        db,
        ambitoValidado,
        operationId,
        estado,
        ultimoError,
        this.ahora,
      )
      return exponer(actualizado)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async reclamarParaSincronizar(
    ambito: AmbitoOperacionLocal,
    operationId: string,
    leaseId: string,
    duracionLeaseMs = 60_000,
  ): Promise<OperacionPendienteLocal | null> {
    const ambitoValidado = validarAmbito(ambito)
    validarUuid(operationId)
    validarUuid(leaseId)
    if (!Number.isInteger(duracionLeaseMs) || duracionLeaseMs < 1_000) {
      throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
    }
    const db = await this.abrir()
    try {
      const reclamada = await reclamarEnTransaccion(
        db,
        ambitoValidado,
        operationId,
        leaseId,
        duracionLeaseMs,
        this.ahora,
      )
      return reclamada ? exponer(reclamada) : null
    } catch (error) {
      throw traducirError(error)
    }
  }

  async finalizarSincronizacion(
    ambito: AmbitoOperacionLocal,
    operationId: string,
    leaseId: string,
    confirmacion: ResultadoRegistroAnimal,
  ): Promise<OperacionPendienteLocal> {
    const ambitoValidado = validarAmbito(ambito)
    validarUuid(operationId)
    validarUuid(leaseId)
    validarConfirmacion(confirmacion, operationId)
    const db = await this.abrir()
    try {
      const actualizada = await resolverLeaseEnTransaccion(
        db,
        ambitoValidado,
        operationId,
        leaseId,
        'SINCRONIZADO',
        null,
        confirmacion,
        this.ahora,
      )
      return exponer(actualizada)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async fallarSincronizacion(
    ambito: AmbitoOperacionLocal,
    operationId: string,
    leaseId: string,
    estado: Exclude<EstadoOperacionLocal, 'PENDIENTE' | 'SINCRONIZANDO' | 'SINCRONIZADO'>,
    ultimoError: CodigoErrorSeguroOperacion,
  ): Promise<OperacionPendienteLocal> {
    const ambitoValidado = validarAmbito(ambito)
    validarUuid(operationId)
    validarUuid(leaseId)
    if (!ESTADOS.has(estado) || !ERRORES_SEGUROS.has(ultimoError)) {
      throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
    }
    const db = await this.abrir()
    try {
      const actualizada = await resolverLeaseEnTransaccion(
        db,
        ambitoValidado,
        operationId,
        leaseId,
        estado,
        ultimoError,
        null,
        this.ahora,
      )
      return exponer(actualizada)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async cerrar(): Promise<void> {
    const apertura = this.apertura
    this.apertura = undefined
    if (apertura) {
      const db = await apertura
      db.close()
    }
  }

  private abrir(): Promise<IDBDatabase> {
    if (!this.factory) {
      return Promise.reject(
        new ErrorAlmacenamientoLocal('INDEXEDDB_NO_DISPONIBLE'),
      )
    }
    if (!this.apertura) {
      this.apertura = abrirBaseOffline(this.factory, this.nombreBase)
        .catch(error => {
          this.apertura = undefined
          throw traducirError(error, 'APERTURA_FALLIDA')
        })
    }
    return this.apertura
  }
}

function guardarEnTransaccion(
  db: IDBDatabase,
  ambito: AmbitoOperacionLocal,
  payload: SolicitudRegistroAnimalLocal,
  ahora: () => Date,
): Promise<RegistroOperacion> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_OPERACIONES)
    let resultado: RegistroOperacion | undefined
    let errorControlado: ErrorAlmacenamientoLocal | undefined
    let finalizada = false
    const fallar = (error: ErrorAlmacenamientoLocal) => {
      if (!finalizada) {
        finalizada = true
        reject(error)
      }
    }
    transaccion.oncomplete = () => {
      if (errorControlado) {
        fallar(errorControlado)
      } else if (resultado) {
        finalizada = true
        resolve(resultado)
      } else {
        fallar(new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'))
      }
    }
    transaccion.onerror = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )
    transaccion.onabort = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )

    const porOperacion = almacen.index(INDICE_OPERACION).get(payload.operationId)
    porOperacion.onsuccess = () => {
      const existente = porOperacion.result as RegistroOperacion | undefined
      if (existente) {
        if (mismaOperacion(existente, ambito, payload)) {
          resultado = existente
        } else {
          errorControlado = new ErrorAlmacenamientoLocal('OPERACION_CONFLICTIVA')
        }
        return
      }
      const porAnimal = almacen.index(INDICE_ANIMAL_PENDIENTE)
        .get(claveAnimal(ambito, payload.animalId))
      porAnimal.onsuccess = () => {
        if (porAnimal.result) {
          errorControlado = new ErrorAlmacenamientoLocal('OPERACION_CONFLICTIVA')
          return
        }
        const instante = ahora().toISOString()
        const registro: RegistroOperacion = {
          ...ambito,
          operationId: payload.operationId,
          animalId: payload.animalId,
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
          animalClave: claveAnimal(ambito, payload.animalId),
        }
        const agregado = almacen.add(registro)
        agregado.onsuccess = () => {
          resultado = registro
        }
      }
    }
  })
}

function actualizarEnTransaccion(
  db: IDBDatabase,
  ambito: AmbitoOperacionLocal,
  operationId: string,
  estado: EstadoOperacionLocal,
  ultimoError: CodigoErrorSeguroOperacion | null,
  ahora: () => Date,
): Promise<RegistroOperacion> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_OPERACIONES)
    let resultado: RegistroOperacion | undefined
    let noEncontrada = false
    let finalizada = false
    const fallar = (error: ErrorAlmacenamientoLocal) => {
      if (!finalizada) {
        finalizada = true
        reject(error)
      }
    }
    transaccion.oncomplete = () => {
      if (noEncontrada) {
        fallar(new ErrorAlmacenamientoLocal('OPERACION_NO_ENCONTRADA'))
      } else if (resultado) {
        finalizada = true
        resolve(resultado)
      } else {
        fallar(new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'))
      }
    }
    transaccion.onerror = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )
    transaccion.onabort = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )

    const consulta = almacen.get(claveOperacion(ambito, operationId))
    consulta.onsuccess = () => {
      const registro = consulta.result as RegistroOperacion | undefined
      if (!registro) {
        noEncontrada = true
        return
      }
      const actualizado: RegistroOperacion = {
        ...normalizarRegistro(registro),
        estado,
        ultimoError,
        actualizadoEnLocal: ahora().toISOString(),
        ...(estado !== 'SINCRONIZANDO' && {
          leaseId: null,
          leaseHasta: null,
        }),
      }
      const guardado = almacen.put(actualizado)
      guardado.onsuccess = () => {
        resultado = actualizado
      }
    }
  })
}

function reclamarEnTransaccion(
  db: IDBDatabase,
  ambito: AmbitoOperacionLocal,
  operationId: string,
  leaseId: string,
  duracionLeaseMs: number,
  ahora: () => Date,
): Promise<RegistroOperacion | null> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_OPERACIONES)
    let resultado: RegistroOperacion | null = null
    let errorControlado: ErrorAlmacenamientoLocal | undefined
    let finalizada = false
    const fallar = (error: ErrorAlmacenamientoLocal) => {
      if (!finalizada) {
        finalizada = true
        reject(error)
      }
    }
    transaccion.oncomplete = () => {
      if (errorControlado) fallar(errorControlado)
      else {
        finalizada = true
        resolve(resultado)
      }
    }
    transaccion.onerror = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )
    transaccion.onabort = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )

    const consulta = almacen.get(claveOperacion(ambito, operationId))
    consulta.onsuccess = () => {
      const encontrado = consulta.result as RegistroOperacion | undefined
      if (!encontrado) {
        errorControlado = new ErrorAlmacenamientoLocal('OPERACION_NO_ENCONTRADA')
        return
      }
      const registro = normalizarRegistro(encontrado)
      const instante = ahora()
      const leaseVigente = registro.estado === 'SINCRONIZANDO'
        && registro.leaseHasta !== null
        && Date.parse(registro.leaseHasta) > instante.getTime()
      const recuperable = registro.estado === 'PENDIENTE'
        || registro.estado === 'ERROR_REINTENTABLE'
        || registro.estado === 'REQUIERE_SESION'
        || (registro.estado === 'RECHAZADO'
          && registro.ultimoError === 'AUTORIZACION')
        || (registro.estado === 'SINCRONIZANDO' && !leaseVigente)
      if (!recuperable || leaseVigente) return

      resultado = {
        ...registro,
        estado: 'SINCRONIZANDO',
        ultimoError: null,
        intentos: registro.intentos + 1,
        leaseId,
        leaseHasta: new Date(instante.getTime() + duracionLeaseMs).toISOString(),
        actualizadoEnLocal: instante.toISOString(),
      }
      almacen.put(resultado)
    }
  })
}

function resolverLeaseEnTransaccion(
  db: IDBDatabase,
  ambito: AmbitoOperacionLocal,
  operationId: string,
  leaseId: string,
  estado: EstadoOperacionLocal,
  ultimoError: CodigoErrorSeguroOperacion | null,
  confirmacion: ResultadoRegistroAnimal | null,
  ahora: () => Date,
): Promise<RegistroOperacion> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_OPERACIONES, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_OPERACIONES)
    let resultado: RegistroOperacion | undefined
    let errorControlado: ErrorAlmacenamientoLocal | undefined
    let finalizada = false
    const fallar = (error: ErrorAlmacenamientoLocal) => {
      if (!finalizada) {
        finalizada = true
        reject(error)
      }
    }
    transaccion.oncomplete = () => {
      if (errorControlado) fallar(errorControlado)
      else if (resultado) {
        finalizada = true
        resolve(resultado)
      } else fallar(new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'))
    }
    transaccion.onerror = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )
    transaccion.onabort = () => fallar(
      new ErrorAlmacenamientoLocal('TRANSACCION_FALLIDA'),
    )

    const consulta = almacen.get(claveOperacion(ambito, operationId))
    consulta.onsuccess = () => {
      const encontrado = consulta.result as RegistroOperacion | undefined
      if (!encontrado) {
        errorControlado = new ErrorAlmacenamientoLocal('OPERACION_NO_ENCONTRADA')
        return
      }
      const registro = normalizarRegistro(encontrado)
      if (registro.estado !== 'SINCRONIZANDO' || registro.leaseId !== leaseId) {
        errorControlado = new ErrorAlmacenamientoLocal('OPERACION_CONFLICTIVA')
        return
      }
      if (confirmacion && confirmacion.animalId !== registro.animalId) {
        errorControlado = new ErrorAlmacenamientoLocal('OPERACION_CONFLICTIVA')
        return
      }
      resultado = {
        ...registro,
        estado,
        ultimoError,
        leaseId: null,
        leaseHasta: null,
        confirmacion,
        actualizadoEnLocal: ahora().toISOString(),
      }
      almacen.put(resultado)
    }
  })
}

function esperarSolicitud<T>(solicitud: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    solicitud.onsuccess = () => resolve(solicitud.result)
    solicitud.onerror = () => reject(solicitud.error)
  })
}

function esperarTransaccion(transaccion: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    transaccion.oncomplete = () => resolve()
    transaccion.onerror = () => reject(transaccion.error)
    transaccion.onabort = () => reject(transaccion.error)
  })
}

async function consumirFalloTransaccion(promesa: Promise<void>): Promise<void> {
  try {
    await promesa
  } catch {
    // El error público se traduce en el punto de llamada, sin registrar datos.
  }
}

function validarAmbito(ambito: AmbitoOperacionLocal): AmbitoOperacionLocal {
  validarUuid(ambito.usuarioId)
  validarUuid(ambito.ganaderiaId)
  return { usuarioId: ambito.usuarioId, ganaderiaId: ambito.ganaderiaId }
}

function validarYCopiarPayload(
  payload: SolicitudRegistroAnimalLocal,
): SolicitudRegistroAnimalLocal {
  if (!payload || typeof payload !== 'object') {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  const objeto = payload as unknown as Record<string, unknown>
  if (Object.keys(objeto).some(clave => !CLAVES_PAYLOAD.has(clave))) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  validarUuid(payload.operationId)
  validarUuid(payload.animalId)
  if (payload.tipo !== 'CREAR_ANIMAL' || payload.baseVersion !== 0
      || (payload.schemaVersion !== 1 && payload.schemaVersion !== 2)
      || (payload.sexo !== 'MACHO' && payload.sexo !== 'HEMBRA')) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  validarTextoOpcional(payload.nombre, 100, false)
  validarTextoOpcional(payload.areteFrente, 100, false)
  validarTextoOpcional(payload.areteReverso, 100, false)
  validarTextoOpcional(payload.raza, 100, false)
  validarTextoOpcional(payload.observaciones, 2000, true)
  if (payload.fechaNacimiento !== undefined
      && (typeof payload.fechaNacimiento !== 'string'
        || !FECHA_ISO.test(payload.fechaNacimiento))) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  if ((payload.fechaNacimiento === undefined)
      !== (payload.nacimientoEstimado === undefined)
      || (payload.nacimientoEstimado !== undefined
        && typeof payload.nacimientoEstimado !== 'boolean')) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  if (payload.categoria !== undefined
      && (!CATEGORIAS.has(payload.categoria)
        || !categoriaCompatible(payload.sexo, payload.categoria))) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  if (payload.schemaVersion === 1
      && ('coloresIdentificacionTextil' in payload
        || 'ubicacionIdentificacionTextil' in payload)) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
  if (payload.schemaVersion === 2) {
    validarTextoOpcional(payload.coloresIdentificacionTextil, 100, false)
    const tieneColores = Boolean(payload.coloresIdentificacionTextil)
    const tieneUbicacion = Boolean(payload.ubicacionIdentificacionTextil)
    if (tieneColores !== tieneUbicacion
        || (payload.ubicacionIdentificacionTextil !== undefined
          && payload.ubicacionIdentificacionTextil !== 'IZQUIERDA'
          && payload.ubicacionIdentificacionTextil !== 'DERECHA'
          && payload.ubicacionIdentificacionTextil !== 'AMBAS')) {
      throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
    }
  }
  const copiaCanonica: Record<string, unknown> = {}
  for (const clave of CLAVES_PAYLOAD) {
    if (Object.prototype.hasOwnProperty.call(objeto, clave)) {
      copiaCanonica[clave] = structuredClone(objeto[clave])
    }
  }
  return copiaCanonica as SolicitudRegistroAnimalLocal
}

function validarTextoOpcional(
  valor: unknown,
  maximo: number,
  admiteNuevaLinea: boolean,
): void {
  if (valor === undefined) return
  if (typeof valor !== 'string' || valor.length === 0
      || Array.from(valor).length > maximo
      || valor !== valor.normalize('NFC').trim()
      || (admiteNuevaLinea ? CONTROLES_CON_SALTOS : CONTROLES_SIN_SALTOS)
        .test(valor)) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
}

function categoriaCompatible(sexo: 'MACHO' | 'HEMBRA', categoria: string): boolean {
  return sexo === 'HEMBRA'
    ? categoria === 'TERNERA' || categoria === 'VAQUILLA' || categoria === 'VACA'
    : categoria === 'TERNERO' || categoria === 'TORETE' || categoria === 'TORO'
}

function validarUuid(valor: string): void {
  if (typeof valor !== 'string' || !UUID_V4.test(valor)) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
}

function validarConfirmacion(
  confirmacion: ResultadoRegistroAnimal,
  operationId: string,
): void {
  if (!confirmacion || confirmacion.operationId !== operationId
      || typeof confirmacion.animalId !== 'string'
      || !UUID_V4.test(confirmacion.animalId)
      || (confirmacion.resultado !== 'APLICADO'
        && confirmacion.resultado !== 'REPROCESADO')
      || confirmacion.versionCreada !== 1
      || !Number.isFinite(Date.parse(confirmacion.confirmadoEn))
      || !Array.isArray(confirmacion.advertencias)
      || confirmacion.advertencias.some(item => item !== 'ARETE_COINCIDENTE')) {
    throw new ErrorAlmacenamientoLocal('DATOS_INVALIDOS')
  }
}

function claveContexto(ambito: AmbitoOperacionLocal): string {
  return `${ambito.usuarioId}:${ambito.ganaderiaId}`
}

function claveOperacion(ambito: AmbitoOperacionLocal, operationId: string): string {
  return `${claveContexto(ambito)}:${operationId}`
}

function claveAnimal(ambito: AmbitoOperacionLocal, animalId: string): string {
  return `${claveContexto(ambito)}:${animalId}`
}

function mismaOperacion(
  existente: RegistroOperacion,
  ambito: AmbitoOperacionLocal,
  payload: SolicitudRegistroAnimalLocal,
): boolean {
  return existente.usuarioId === ambito.usuarioId
    && existente.ganaderiaId === ambito.ganaderiaId
    && JSON.stringify(existente.payload) === JSON.stringify(payload)
}

function exponer(registro: RegistroOperacion): OperacionPendienteLocal {
  const normalizado = normalizarRegistro(registro)
  const {
    clave: _clave,
    contextoClave: _contextoClave,
    animalClave: _animalClave,
    ...publico
  } = normalizado
  return structuredClone(publico)
}

function normalizarRegistro(registro: RegistroOperacion): RegistroOperacion {
  return {
    ...registro,
    intentos: Number.isInteger(registro.intentos) && registro.intentos >= 0
      ? registro.intentos : 0,
    leaseId: typeof registro.leaseId === 'string' ? registro.leaseId : null,
    leaseHasta: typeof registro.leaseHasta === 'string' ? registro.leaseHasta : null,
    confirmacion: registro.confirmacion ?? null,
  }
}

function traducirError(
  error: unknown,
  codigo: CodigoErrorAlmacenamientoLocal = 'TRANSACCION_FALLIDA',
): ErrorAlmacenamientoLocal {
  if (error instanceof ErrorAlmacenamientoLocal) {
    return error
  }
  return new ErrorAlmacenamientoLocal(codigo)
}
