import {
  ErrorAnimal,
  registrarAnimal,
  type ResultadoRegistroAnimal,
  type SolicitudRegistroAnimalCompatible,
} from './animalesApi'
import {
  consultarAccesoGanaderia,
  ErrorGanaderia,
  type AccesoGanaderia,
} from './ganaderiasApi'
import {
  AlmacenOperacionesPendientesIndexedDb,
  type AmbitoOperacionLocal,
  type CodigoErrorSeguroOperacion,
  type EstadoOperacionLocal,
  type OperacionPendienteLocal,
} from './operacionesPendientesDb'
import {
  bloquearAccesoOffline,
  requerirRevalidacionOffline,
} from './registroAnimalesOffline'
import type { SesionActiva } from './sesionApi'

const DURACION_LEASE_MS = 60_000
const TIMEOUT_HTTP_MS = 20_000

type ConsultarAcceso = (
  ganaderiaId: string,
  accessToken: string,
  signal?: AbortSignal,
) => Promise<AccesoGanaderia>

type Registrar = (
  ganaderiaId: string,
  payload: SolicitudRegistroAnimalCompatible,
  accessToken: string,
  signal?: AbortSignal,
) => Promise<ResultadoRegistroAnimal>

type AlmacenSincronizacion = Pick<AlmacenOperacionesPendientesIndexedDb,
  | 'listarPendientes'
  | 'actualizarEstado'
  | 'reclamarParaSincronizar'
  | 'finalizarSincronizacion'
  | 'fallarSincronizacion'>

type ConBloqueo = <T>(nombre: string, accion: () => Promise<T>) => Promise<T>

type OpcionesServicio = {
  almacen?: AlmacenSincronizacion
  consultarAcceso?: ConsultarAcceso
  registrar?: Registrar
  bloquearAcceso?: (ambito: AmbitoOperacionLocal) => Promise<void>
  requerirSesion?: (ambito: AmbitoOperacionLocal) => Promise<void>
  conBloqueo?: ConBloqueo
  crearLeaseId?: () => string
  ahora?: () => Date
  timeoutMs?: number
}

type ResultadoOperacionInterno = ResultadoOperacionSincronizacion & {
  detenerLote: boolean
}

export type EstadoResultadoOperacion =
  | 'SINCRONIZADA'
  | 'ERROR_REINTENTABLE'
  | 'REQUIERE_SESION'
  | 'RECHAZADA'
  | 'CONFLICTO'
  | 'OCUPADA'
  | 'ERROR_LOCAL'

export type ResultadoOperacionSincronizacion = {
  operationId: string
  animalId: string
  estado: EstadoResultadoOperacion
  confirmadaRemotamente: boolean
  resultadoRemoto?: ResultadoRegistroAnimal['resultado']
  reintentoDespuesMs?: number
}

export type ResultadoSincronizacionManual = {
  total: number
  sincronizadas: number
  confirmadasRemotamente: number
  fallidas: number
  ocupadas: number
  operaciones: ResultadoOperacionSincronizacion[]
}

export type TipoErrorSincronizacion =
  | 'sesion'
  | 'permiso'
  | 'conexion'
  | 'servicio'
  | 'almacenamiento'
  | 'enCurso'

export class ErrorSincronizacion extends Error {
  constructor(public readonly tipo: TipoErrorSincronizacion) {
    super(tipo)
    this.name = 'ErrorSincronizacion'
  }
}

export class ServicioSincronizacionAnimales {
  private readonly almacen: AlmacenSincronizacion
  private readonly consultarAcceso: ConsultarAcceso
  private readonly registrar: Registrar
  private readonly bloquearAcceso: (ambito: AmbitoOperacionLocal) => Promise<void>
  private readonly requerirSesion: (ambito: AmbitoOperacionLocal) => Promise<void>
  private readonly conBloqueo: ConBloqueo
  private readonly leaseId: string
  private readonly ahora: () => Date
  private readonly timeoutMs: number
  private readonly ambitosEnCurso = new Set<string>()

  constructor(opciones: OpcionesServicio = {}) {
    this.almacen = opciones.almacen ?? new AlmacenOperacionesPendientesIndexedDb()
    this.consultarAcceso = opciones.consultarAcceso ?? consultarAccesoGanaderia
    this.registrar = opciones.registrar ?? registrarAnimal
    this.bloquearAcceso = opciones.bloquearAcceso ?? bloquearAccesoOffline
    this.requerirSesion = opciones.requerirSesion ?? requerirRevalidacionOffline
    this.conBloqueo = opciones.conBloqueo ?? bloqueoNavegador
    this.leaseId = (opciones.crearLeaseId ?? (() => crypto.randomUUID()))()
    this.ahora = opciones.ahora ?? (() => new Date())
    this.timeoutMs = opciones.timeoutMs ?? TIMEOUT_HTTP_MS
  }

  async sincronizar(
    sesion: Pick<SesionActiva, 'usuarioId' | 'accessToken' | 'expiresAt'>,
    ganaderiaId: string,
    signal?: AbortSignal,
  ): Promise<ResultadoSincronizacionManual> {
    const ambito = { usuarioId: sesion.usuarioId, ganaderiaId }
    const claveAmbito = `${sesion.usuarioId}:${ganaderiaId}`
    if (this.ambitosEnCurso.has(claveAmbito)) {
      throw new ErrorSincronizacion('enCurso')
    }
    this.ambitosEnCurso.add(claveAmbito)
    try {
      if (signal?.aborted) throw new ErrorSincronizacion('conexion')
      if (Date.parse(sesion.expiresAt) <= this.ahora().getTime()) {
        await this.marcarAmbito(ambito, 'REQUIERE_SESION', 'AUTENTICACION')
        await this.requerirSinOcultar(ambito)
        throw new ErrorSincronizacion('sesion')
      }
      const acceso = await this.revalidarAcceso(ambito, sesion.accessToken, signal)
      if (acceso.rol === 'LECTOR') {
        await this.marcarAmbito(ambito, 'RECHAZADO', 'AUTORIZACION')
        await this.bloquearSinOcultar(ambito)
        throw new ErrorSincronizacion('permiso')
      }

      const pendientes = await this.listar(ambito)
      const operaciones: ResultadoOperacionSincronizacion[] = []
      for (const pendiente of pendientes.filter(esCandidata)) {
        if (signal?.aborted) break
        const { detenerLote, ...resultado } = await this.sincronizarOperacion(
          ambito,
          sesion.accessToken,
          pendiente,
          signal,
        )
        operaciones.push(resultado)
        if (detenerLote) break
      }
      return resumir(operaciones)
    } finally {
      this.ambitosEnCurso.delete(claveAmbito)
    }
  }

  private async revalidarAcceso(
    ambito: AmbitoOperacionLocal,
    accessToken: string,
    signal?: AbortSignal,
  ): Promise<AccesoGanaderia> {
    try {
      const acceso = await this.consultarAcceso(
        ambito.ganaderiaId,
        accessToken,
        signal,
      )
      if (acceso.ganaderiaId !== ambito.ganaderiaId) {
        throw new ErrorGanaderia('seguridad')
      }
      return acceso
    } catch (error) {
      const fallo = error instanceof ErrorGanaderia
        ? error : new ErrorGanaderia('conexion')
      if (fallo.tipo === 'sesion') {
        await this.marcarAmbito(ambito, 'REQUIERE_SESION', 'AUTENTICACION')
        await this.requerirSinOcultar(ambito)
        throw new ErrorSincronizacion('sesion')
      }
      if (fallo.tipo === 'seguridad') {
        await this.marcarAmbito(ambito, 'RECHAZADO', 'AUTORIZACION')
        await this.bloquearSinOcultar(ambito)
        throw new ErrorSincronizacion('permiso')
      }
      throw new ErrorSincronizacion(
        fallo.tipo === 'conexion' ? 'conexion' : 'servicio',
      )
    }
  }

  private async sincronizarOperacion(
    ambito: AmbitoOperacionLocal,
    accessToken: string,
    pendiente: OperacionPendienteLocal,
    signal?: AbortSignal,
  ): Promise<ResultadoOperacionInterno> {
    const reclamada = await this.conBloqueo(
      `ganadoconecta-sync:${pendiente.operationId}`,
      () => this.almacen.reclamarParaSincronizar(
        ambito,
        pendiente.operationId,
        this.leaseId,
        DURACION_LEASE_MS,
      ),
    )
    if (!reclamada) {
      return { ...resultadoLocal(pendiente, 'OCUPADA'), detenerLote: false }
    }

    const control = crearControlTimeout(signal, this.timeoutMs)
    try {
      const confirmacion = await this.registrar(
        ambito.ganaderiaId,
        reclamada.payload,
        accessToken,
        control.signal,
      )
      try {
        await this.almacen.finalizarSincronizacion(
          ambito,
          reclamada.operationId,
          this.leaseId,
          confirmacion,
        )
        return {
          ...resultadoLocal(reclamada, 'SINCRONIZADA'),
          confirmadaRemotamente: true,
          resultadoRemoto: confirmacion.resultado,
          detenerLote: false,
        }
      } catch {
        return {
          ...resultadoLocal(reclamada, 'ERROR_LOCAL'),
          confirmadaRemotamente: true,
          resultadoRemoto: confirmacion.resultado,
          detenerLote: false,
        }
      }
    } catch (error) {
      const clasificacion = clasificarFallo(error)
      try {
        await this.almacen.fallarSincronizacion(
          ambito,
          reclamada.operationId,
          this.leaseId,
          clasificacion.estadoLocal,
          clasificacion.errorSeguro,
        )
      } catch {
        return {
          ...resultadoLocal(reclamada, 'ERROR_LOCAL'),
          detenerLote: clasificacion.detenerLote,
        }
      }
      if (clasificacion.estadoLocal === 'REQUIERE_SESION') {
        await this.requerirSinOcultar(ambito)
      }
      if (clasificacion.estadoLocal === 'RECHAZADO'
          && clasificacion.errorSeguro === 'AUTORIZACION') {
        await this.bloquearSinOcultar(ambito)
      }
      return {
        ...resultadoLocal(
          reclamada,
          clasificacion.estadoResultado,
          clasificacion.reintentoDespuesMs,
        ),
        detenerLote: clasificacion.detenerLote,
      }
    } finally {
      control.limpiar()
    }
  }

  private async listar(ambito: AmbitoOperacionLocal): Promise<OperacionPendienteLocal[]> {
    try {
      return await this.almacen.listarPendientes(ambito)
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
  }

  private async marcarAmbito(
    ambito: AmbitoOperacionLocal,
    estado: EstadoOperacionLocal,
    error: CodigoErrorSeguroOperacion,
  ): Promise<void> {
    let pendientes: OperacionPendienteLocal[]
    try {
      pendientes = await this.almacen.listarPendientes(ambito)
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
    try {
      await Promise.all(pendientes
        .filter(operacion => operacion.estado !== 'CONFLICTO'
          && !(operacion.estado === 'RECHAZADO'
            && operacion.ultimoError === 'VALIDACION'))
        .map(operacion => this.almacen.actualizarEstado(
          ambito,
          operacion.operationId,
          estado,
          error,
        )))
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
  }

  private async bloquearSinOcultar(ambito: AmbitoOperacionLocal): Promise<void> {
    try {
      await this.bloquearAcceso(ambito)
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
  }

  private async requerirSinOcultar(ambito: AmbitoOperacionLocal): Promise<void> {
    try {
      await this.requerirSesion(ambito)
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
  }
}

function esCandidata(operacion: OperacionPendienteLocal): boolean {
  return operacion.estado === 'PENDIENTE'
    || operacion.estado === 'ERROR_REINTENTABLE'
    || operacion.estado === 'REQUIERE_SESION'
    || operacion.estado === 'SINCRONIZANDO'
    || (operacion.estado === 'RECHAZADO'
      && operacion.ultimoError === 'AUTORIZACION')
}

function clasificarFallo(error: unknown): {
  estadoLocal: Exclude<EstadoOperacionLocal, 'PENDIENTE' | 'SINCRONIZANDO' | 'SINCRONIZADO'>
  errorSeguro: CodigoErrorSeguroOperacion
  estadoResultado: EstadoResultadoOperacion
  detenerLote: boolean
  reintentoDespuesMs?: number
} {
  const fallo = error instanceof ErrorAnimal ? error : new ErrorAnimal('conexion')
  if (fallo.tipo === 'sesion') {
    return {
      estadoLocal: 'REQUIERE_SESION',
      errorSeguro: 'AUTENTICACION',
      estadoResultado: 'REQUIERE_SESION',
      detenerLote: true,
    }
  }
  if (fallo.tipo === 'permiso') {
    return {
      estadoLocal: 'RECHAZADO',
      errorSeguro: 'AUTORIZACION',
      estadoResultado: 'RECHAZADA',
      detenerLote: true,
    }
  }
  if (fallo.tipo === 'validacion' || fallo.tipo === 'noEncontrado') {
    return {
      estadoLocal: 'RECHAZADO',
      errorSeguro: fallo.tipo === 'validacion' ? 'VALIDACION' : 'PROTOCOLO',
      estadoResultado: 'RECHAZADA',
      detenerLote: false,
    }
  }
  if (fallo.tipo === 'conflicto') {
    return {
      estadoLocal: 'CONFLICTO',
      errorSeguro: 'CONFLICTO',
      estadoResultado: 'CONFLICTO',
      detenerLote: false,
    }
  }
  return {
    estadoLocal: 'ERROR_REINTENTABLE',
    errorSeguro: fallo.tipo === 'conexion' ? 'RED'
      : fallo.tipo === 'seguridad' ? 'PROTOCOLO' : 'SERVIDOR',
    estadoResultado: 'ERROR_REINTENTABLE',
    detenerLote: false,
    reintentoDespuesMs: fallo.retryAfterMs,
  }
}

function resultadoLocal(
  operacion: Pick<OperacionPendienteLocal, 'operationId' | 'animalId'>,
  estado: EstadoResultadoOperacion,
  reintentoDespuesMs?: number,
): ResultadoOperacionSincronizacion {
  return {
    operationId: operacion.operationId,
    animalId: operacion.animalId,
    estado,
    confirmadaRemotamente: false,
    ...(reintentoDespuesMs !== undefined && { reintentoDespuesMs }),
  }
}

function resumir(
  operaciones: ResultadoOperacionSincronizacion[],
): ResultadoSincronizacionManual {
  return {
    total: operaciones.length,
    sincronizadas: operaciones.filter(item => item.estado === 'SINCRONIZADA').length,
    confirmadasRemotamente: operaciones
      .filter(item => item.confirmadaRemotamente).length,
    fallidas: operaciones.filter(item => item.estado !== 'SINCRONIZADA'
      && item.estado !== 'OCUPADA').length,
    ocupadas: operaciones.filter(item => item.estado === 'OCUPADA').length,
    operaciones,
  }
}

function crearControlTimeout(signal: AbortSignal | undefined, timeoutMs: number) {
  const control = new AbortController()
  const propagar = () => control.abort()
  signal?.addEventListener('abort', propagar, { once: true })
  if (signal?.aborted) control.abort()
  const temporizador = window.setTimeout(() => control.abort(), timeoutMs)
  return {
    signal: control.signal,
    limpiar: () => {
      window.clearTimeout(temporizador)
      signal?.removeEventListener('abort', propagar)
    },
  }
}

async function bloqueoNavegador<T>(
  nombre: string,
  accion: () => Promise<T>,
): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request(nombre, { mode: 'exclusive' }, accion)
  }
  return accion()
}

const servicio = new ServicioSincronizacionAnimales()

export function sincronizarPendientesManualmente(
  sesion: Pick<SesionActiva, 'usuarioId' | 'accessToken' | 'expiresAt'>,
  ganaderiaId: string,
  signal?: AbortSignal,
): Promise<ResultadoSincronizacionManual> {
  return servicio.sincronizar(sesion, ganaderiaId, signal)
}
