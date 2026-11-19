import type { RolGanaderia } from './ganaderiasApi'
import {
  abrirBaseOffline,
  ALMACEN_CONTEXTOS,
  INDICE_CONTEXTOS_USUARIO,
  NOMBRE_BASE_OFFLINE,
} from './indexedDbOffline'

export const HORAS_MAXIMAS_AUTORIZACION_OFFLINE = 168
const DURACION_MAXIMA_MS = HORAS_MAXIMAS_AUTORIZACION_OFFLINE * 60 * 60 * 1000
const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export type EstadoContextoOffline =
  | 'VIGENTE'
  | 'REQUIERE_REVALIDACION'
  | 'BLOQUEADO'

export type MotivoContextoOffline =
  | 'VENCIDA'
  | 'RELOJ_INCONSISTENTE'
  | 'SESION_NO_DISPONIBLE'
  | 'ACCESO_RECHAZADO'

export type ContextoOffline = {
  usuarioId: string
  ganaderiaId: string
  rol: RolGanaderia
  estado: EstadoContextoOffline
  motivo: MotivoContextoOffline | null
  validadoEnServidor: string
  autorizadoHastaServidor: string
  observadoEnCliente: string
  autorizadoHastaCliente: string
  ultimoComprobadoEnCliente: string
}

type RegistroContextoOffline = ContextoOffline & { clave: string }

export type ValidacionOnlineContexto = {
  usuarioId: string
  ganaderiaId: string
  rol: RolGanaderia
  estado: 'ACTIVA'
  validadoEn: string
}

export type ResultadoEvaluacionContexto =
  | { resultado: 'AUTORIZADA_ESCRITURA', contexto: ContextoOffline }
  | { resultado: 'SOLO_LECTURA', contexto: ContextoOffline }
  | { resultado: 'AUSENTE', contexto: null }
  | { resultado: 'VENCIDA', contexto: ContextoOffline }
  | { resultado: 'RELOJ_INCONSISTENTE', contexto: ContextoOffline }
  | { resultado: 'REQUIERE_SESION', contexto: ContextoOffline }
  | { resultado: 'BLOQUEADA', contexto: ContextoOffline }

export type CodigoErrorContextoOffline =
  | 'INDEXEDDB_NO_DISPONIBLE'
  | 'APERTURA_FALLIDA'
  | 'TRANSACCION_FALLIDA'
  | 'DATOS_INVALIDOS'

export class ErrorContextoOffline extends Error {
  constructor(public readonly codigo: CodigoErrorContextoOffline) {
    super(codigo)
    this.name = 'ErrorContextoOffline'
  }
}

type OpcionesAlmacenContextos = {
  indexedDB?: IDBFactory
  nombreBase?: string
  ahora?: () => Date
}

export class AlmacenContextosOfflineIndexedDb {
  private readonly factory: IDBFactory | undefined
  private readonly nombreBase: string
  private readonly ahora: () => Date
  private apertura?: Promise<IDBDatabase>

  constructor(opciones: OpcionesAlmacenContextos = {}) {
    this.factory = opciones.indexedDB ?? globalThis.indexedDB
    this.nombreBase = opciones.nombreBase ?? NOMBRE_BASE_OFFLINE
    this.ahora = opciones.ahora ?? (() => new Date())
  }

  async guardarValidacion(
    validacion: ValidacionOnlineContexto,
  ): Promise<ContextoOffline> {
    validarUuid(validacion.usuarioId)
    validarUuid(validacion.ganaderiaId)
    validarRol(validacion.rol)
    if (validacion.estado !== 'ACTIVA') throw datosInvalidos()
    const validadoEn = instanteValido(validacion.validadoEn)
    const observadoEn = instanteValido(this.ahora().toISOString())
    const contexto: RegistroContextoOffline = {
      usuarioId: validacion.usuarioId,
      ganaderiaId: validacion.ganaderiaId,
      rol: validacion.rol,
      estado: 'VIGENTE',
      motivo: null,
      validadoEnServidor: validadoEn.toISOString(),
      autorizadoHastaServidor: new Date(
        validadoEn.getTime() + DURACION_MAXIMA_MS,
      ).toISOString(),
      observadoEnCliente: observadoEn.toISOString(),
      autorizadoHastaCliente: new Date(
        observadoEn.getTime() + DURACION_MAXIMA_MS,
      ).toISOString(),
      ultimoComprobadoEnCliente: observadoEn.toISOString(),
      clave: claveContexto(validacion.usuarioId, validacion.ganaderiaId),
    }
    const db = await this.abrir()
    try {
      await guardarRegistro(db, contexto)
      return exponer(contexto)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async evaluarEscritura(
    usuarioId: string,
    ganaderiaId: string,
  ): Promise<ResultadoEvaluacionContexto> {
    validarUuid(usuarioId)
    validarUuid(ganaderiaId)
    const db = await this.abrir()
    const ahora = this.ahora()
    try {
      const contexto = await mutarContexto(
        db,
        claveContexto(usuarioId, ganaderiaId),
        actual => evaluarYActualizar(actual, ahora),
      )
      return evaluarResultado(contexto)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async obtener(
    usuarioId: string,
    ganaderiaId: string,
  ): Promise<ContextoOffline | null> {
    validarUuid(usuarioId)
    validarUuid(ganaderiaId)
    const db = await this.abrir()
    const transaccion = db.transaction(ALMACEN_CONTEXTOS, 'readonly')
    const terminada = esperarTransaccion(transaccion)
    try {
      const registro = await esperarSolicitud<RegistroContextoOffline | undefined>(
        transaccion.objectStore(ALMACEN_CONTEXTOS)
          .get(claveContexto(usuarioId, ganaderiaId)),
      )
      await terminada
      return registro ? exponer(registro) : null
    } catch (error) {
      await consumirFallo(terminada)
      throw traducirError(error)
    }
  }

  async requerirSesion(usuarioId: string, ganaderiaId: string): Promise<void> {
    await this.cambiarEstado(
      usuarioId,
      ganaderiaId,
      'REQUIERE_REVALIDACION',
      'SESION_NO_DISPONIBLE',
    )
  }

  async bloquearAcceso(usuarioId: string, ganaderiaId: string): Promise<void> {
    await this.cambiarEstado(
      usuarioId,
      ganaderiaId,
      'BLOQUEADO',
      'ACCESO_RECHAZADO',
    )
  }

  async bloquearUsuario(usuarioId: string): Promise<number> {
    validarUuid(usuarioId)
    const db = await this.abrir()
    try {
      return await bloquearContextosUsuario(db, usuarioId, this.ahora)
    } catch (error) {
      throw traducirError(error)
    }
  }

  async cerrar(): Promise<void> {
    const apertura = this.apertura
    this.apertura = undefined
    if (apertura) (await apertura).close()
  }

  private async cambiarEstado(
    usuarioId: string,
    ganaderiaId: string,
    estado: EstadoContextoOffline,
    motivo: MotivoContextoOffline,
  ): Promise<void> {
    validarUuid(usuarioId)
    validarUuid(ganaderiaId)
    const db = await this.abrir()
    try {
      await mutarContexto(
        db,
        claveContexto(usuarioId, ganaderiaId),
        actual => actual ? {
          ...actual,
          estado,
          motivo,
          ultimoComprobadoEnCliente: this.ahora().toISOString(),
        } : null,
      )
    } catch (error) {
      throw traducirError(error)
    }
  }

  private abrir(): Promise<IDBDatabase> {
    if (!this.factory) {
      return Promise.reject(new ErrorContextoOffline('INDEXEDDB_NO_DISPONIBLE'))
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

function guardarRegistro(
  db: IDBDatabase,
  contexto: RegistroContextoOffline,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_CONTEXTOS, 'readwrite')
    let finalizada = false
    const fallar = () => {
      if (!finalizada) {
        finalizada = true
        reject(new ErrorContextoOffline('TRANSACCION_FALLIDA'))
      }
    }
    transaccion.oncomplete = () => {
      finalizada = true
      resolve()
    }
    transaccion.onerror = fallar
    transaccion.onabort = fallar
    transaccion.objectStore(ALMACEN_CONTEXTOS).put(contexto)
  })
}

function mutarContexto(
  db: IDBDatabase,
  clave: string,
  transformar: (
    contexto: RegistroContextoOffline | null,
  ) => RegistroContextoOffline | null,
): Promise<RegistroContextoOffline | null> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_CONTEXTOS, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_CONTEXTOS)
    let resultado: RegistroContextoOffline | null = null
    let finalizada = false
    const fallar = () => {
      if (!finalizada) {
        finalizada = true
        reject(new ErrorContextoOffline('TRANSACCION_FALLIDA'))
      }
    }
    transaccion.oncomplete = () => {
      finalizada = true
      resolve(resultado)
    }
    transaccion.onerror = fallar
    transaccion.onabort = fallar
    const consulta = almacen.get(clave)
    consulta.onsuccess = () => {
      const actual = consulta.result as RegistroContextoOffline | undefined
      resultado = transformar(actual ?? null)
      if (resultado) almacen.put(resultado)
    }
  })
}

function bloquearContextosUsuario(
  db: IDBDatabase,
  usuarioId: string,
  ahora: () => Date,
): Promise<number> {
  return new Promise((resolve, reject) => {
    const transaccion = db.transaction(ALMACEN_CONTEXTOS, 'readwrite')
    const almacen = transaccion.objectStore(ALMACEN_CONTEXTOS)
    let cantidad = 0
    let finalizada = false
    const fallar = () => {
      if (!finalizada) {
        finalizada = true
        reject(new ErrorContextoOffline('TRANSACCION_FALLIDA'))
      }
    }
    transaccion.oncomplete = () => {
      finalizada = true
      resolve(cantidad)
    }
    transaccion.onerror = fallar
    transaccion.onabort = fallar
    const consulta = almacen.index(INDICE_CONTEXTOS_USUARIO).getAll(usuarioId)
    consulta.onsuccess = () => {
      const contextos = consulta.result as RegistroContextoOffline[]
      cantidad = contextos.length
      const instante = ahora().toISOString()
      for (const contexto of contextos) {
        almacen.put({
          ...contexto,
          estado: 'REQUIERE_REVALIDACION',
          motivo: 'SESION_NO_DISPONIBLE',
          ultimoComprobadoEnCliente: instante,
        } satisfies RegistroContextoOffline)
      }
    }
  })
}

function evaluarYActualizar(
  contexto: RegistroContextoOffline | null,
  ahora: Date,
): RegistroContextoOffline | null {
  if (!contexto || contexto.estado !== 'VIGENTE') return contexto
  const ahoraMs = ahora.getTime()
  const ultimoMs = Date.parse(contexto.ultimoComprobadoEnCliente)
  const venceServidorMs = Date.parse(contexto.autorizadoHastaServidor)
  const venceClienteMs = Date.parse(contexto.autorizadoHastaCliente)
  if (!Number.isFinite(ahoraMs) || !Number.isFinite(ultimoMs)
      || !Number.isFinite(venceServidorMs) || !Number.isFinite(venceClienteMs)
      || ahoraMs < ultimoMs) {
    return {
      ...contexto,
      estado: 'REQUIERE_REVALIDACION',
      motivo: 'RELOJ_INCONSISTENTE',
    }
  }
  if (ahoraMs >= Math.min(venceServidorMs, venceClienteMs)) {
    return {
      ...contexto,
      estado: 'REQUIERE_REVALIDACION',
      motivo: 'VENCIDA',
      ultimoComprobadoEnCliente: ahora.toISOString(),
    }
  }
  return { ...contexto, ultimoComprobadoEnCliente: ahora.toISOString() }
}

function evaluarResultado(
  registro: RegistroContextoOffline | null,
): ResultadoEvaluacionContexto {
  if (!registro) return { resultado: 'AUSENTE', contexto: null }
  const contexto = exponer(registro)
  if (registro.estado === 'BLOQUEADO') {
    return { resultado: 'BLOQUEADA', contexto }
  }
  if (registro.estado === 'REQUIERE_REVALIDACION') {
    if (registro.motivo === 'VENCIDA') return { resultado: 'VENCIDA', contexto }
    if (registro.motivo === 'RELOJ_INCONSISTENTE') {
      return { resultado: 'RELOJ_INCONSISTENTE', contexto }
    }
    return { resultado: 'REQUIERE_SESION', contexto }
  }
  return registro.rol === 'LECTOR'
    ? { resultado: 'SOLO_LECTURA', contexto }
    : { resultado: 'AUTORIZADA_ESCRITURA', contexto }
}

function validarUuid(valor: string): void {
  if (typeof valor !== 'string' || !UUID_V4.test(valor)) throw datosInvalidos()
}

function validarRol(rol: string): asserts rol is RolGanaderia {
  if (rol !== 'PROPIETARIO' && rol !== 'EDITOR' && rol !== 'LECTOR') {
    throw datosInvalidos()
  }
}

function instanteValido(valor: string): Date {
  const instante = new Date(valor)
  if (!valor || !Number.isFinite(instante.getTime())) throw datosInvalidos()
  return instante
}

function claveContexto(usuarioId: string, ganaderiaId: string): string {
  return `${usuarioId}:${ganaderiaId}`
}

function exponer(registro: RegistroContextoOffline): ContextoOffline {
  const { clave: _clave, ...contexto } = registro
  return structuredClone(contexto)
}

function datosInvalidos(): ErrorContextoOffline {
  return new ErrorContextoOffline('DATOS_INVALIDOS')
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

async function consumirFallo(promesa: Promise<void>): Promise<void> {
  try {
    await promesa
  } catch {
    // El error se traduce sin registrar contenido local.
  }
}

function traducirError(
  error: unknown,
  codigo: CodigoErrorContextoOffline = 'TRANSACCION_FALLIDA',
): ErrorContextoOffline {
  return error instanceof ErrorContextoOffline
    ? error : new ErrorContextoOffline(codigo)
}
