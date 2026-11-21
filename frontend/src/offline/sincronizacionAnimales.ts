import {
  ErrorAnimal,
  registrarAnimal,
  type ResultadoRegistroAnimal,
  type SolicitudRegistroAnimalCompatible,
} from '../features/ganado/animalesApi'
import {
  consultarAccesoGanaderia,
  ErrorGanaderia,
  type AccesoGanaderia,
} from '../features/ganaderias/ganaderiasApi'
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
import {
  AlmacenEventosSanitariosPendientesIndexedDb,
  type ConfirmacionSanitariaLocal,
  type EventoSanitarioPendienteLocal,
} from './eventosSanitariosPendientesDb'
import {
  registrarEventoSanitario,
  type ResultadoEventoSanitario,
  type SolicitudEventoSanitario,
} from '../features/sanidad/historialSanitarioApi'
import {
  ErrorSanidad,
  registrarVacunacion,
  type ResultadoVacunacion,
  type SolicitudVacunacion,
} from '../features/sanidad/vacunacionesApi'
import type { SesionActiva } from '../features/cuenta/sesionApi'

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

type RegistrarEvento = (
  ganaderiaId: string,
  animalId: string,
  accessToken: string,
  payload: SolicitudEventoSanitario,
  signal?: AbortSignal,
) => Promise<ResultadoEventoSanitario>

type RegistrarVacuna = (
  ganaderiaId: string,
  animalId: string,
  accessToken: string,
  payload: SolicitudVacunacion,
  signal?: AbortSignal,
) => Promise<ResultadoVacunacion>

type AlmacenSincronizacion = Pick<AlmacenOperacionesPendientesIndexedDb,
  | 'listarPendientes'
  | 'actualizarEstado'
  | 'reclamarParaSincronizar'
  | 'finalizarSincronizacion'
  | 'fallarSincronizacion'>

type AlmacenSincronizacionSanitaria = Pick<AlmacenEventosSanitariosPendientesIndexedDb,
  | 'listarPendientes'
  | 'actualizarEstado'
  | 'reclamarParaSincronizar'
  | 'finalizarSincronizacion'
  | 'fallarSincronizacion'>

type TipoOperacionSincronizable =
  | 'CREAR_ANIMAL'
  | 'REGISTRAR_VACUNACION'
  | 'REGISTRAR_EVENTO_SANITARIO'

type OperacionSincronizable =
  | { tipoOperacion: 'CREAR_ANIMAL'; dato: OperacionPendienteLocal }
  | { tipoOperacion: 'REGISTRAR_VACUNACION'; dato: EventoSanitarioPendienteLocal }
  | { tipoOperacion: 'REGISTRAR_EVENTO_SANITARIO'; dato: EventoSanitarioPendienteLocal }

type ConBloqueo = <T>(nombre: string, accion: () => Promise<T>) => Promise<T>

type OpcionesServicio = {
  almacen?: AlmacenSincronizacion
  almacenSanidad?: AlmacenSincronizacionSanitaria
  consultarAcceso?: ConsultarAcceso
  registrar?: Registrar
  registrarEvento?: RegistrarEvento
  registrarVacuna?: RegistrarVacuna
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

const ALMACEN_SANITARIO_VACIO: AlmacenSincronizacionSanitaria = {
  listarPendientes: async () => [],
  actualizarEstado: async () => { throw new Error('Almacén sanitario no configurado') },
  reclamarParaSincronizar: async () => null,
  finalizarSincronizacion: async () => { throw new Error('Almacén sanitario no configurado') },
  fallarSincronizacion: async () => { throw new Error('Almacén sanitario no configurado') },
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
  tipoOperacion?: TipoOperacionSincronizable
  estado: EstadoResultadoOperacion
  confirmadaRemotamente: boolean
  resultadoRemoto?: ResultadoRegistroAnimal['resultado']
  reintentoDespuesMs?: number
  codigoError?: CodigoErrorSeguroOperacion
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
  private readonly almacenSanidad: AlmacenSincronizacionSanitaria
  private readonly consultarAcceso: ConsultarAcceso
  private readonly registrar: Registrar
  private readonly registrarEvento: RegistrarEvento
  private readonly registrarVacuna: RegistrarVacuna
  private readonly bloquearAcceso: (ambito: AmbitoOperacionLocal) => Promise<void>
  private readonly requerirSesion: (ambito: AmbitoOperacionLocal) => Promise<void>
  private readonly conBloqueo: ConBloqueo
  private readonly leaseId: string
  private readonly ahora: () => Date
  private readonly timeoutMs: number
  private readonly ambitosEnCurso = new Set<string>()

  constructor(opciones: OpcionesServicio = {}) {
    this.almacen = opciones.almacen ?? new AlmacenOperacionesPendientesIndexedDb()
    this.almacenSanidad = opciones.almacenSanidad
      ?? (opciones.almacen ? ALMACEN_SANITARIO_VACIO
        : new AlmacenEventosSanitariosPendientesIndexedDb())
    this.consultarAcceso = opciones.consultarAcceso ?? consultarAccesoGanaderia
    this.registrar = opciones.registrar ?? registrarAnimal
    this.registrarEvento = opciones.registrarEvento ?? registrarEventoSanitario
    this.registrarVacuna = opciones.registrarVacuna ?? registrarVacunacion
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
      for (const pendiente of pendientes.filter(item => esCandidata(item.dato))) {
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
    pendiente: OperacionSincronizable,
    signal?: AbortSignal,
  ): Promise<ResultadoOperacionInterno> {
    const reclamada = await this.conBloqueo(
      `ganadoconecta-sync:${pendiente.dato.operationId}`,
      () => this.reclamar(ambito, pendiente),
    )
    if (!reclamada) {
      return { ...resultadoLocal(pendiente, 'OCUPADA'), detenerLote: false }
    }

    const control = crearControlTimeout(signal, this.timeoutMs)
    try {
      const confirmacion = await this.enviar(
        ambito, reclamada, accessToken, control.signal,
      )
      try {
        await this.finalizar(ambito, reclamada, confirmacion)
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
        await this.fallar(ambito, reclamada, clasificacion)
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
          clasificacion.errorSeguro,
        ),
        detenerLote: clasificacion.detenerLote,
      }
    } finally {
      control.limpiar()
    }
  }

  private async reclamar(
    ambito: AmbitoOperacionLocal,
    operacion: OperacionSincronizable,
  ): Promise<OperacionSincronizable | null> {
    const dato = operacion.tipoOperacion === 'CREAR_ANIMAL'
      ? await this.almacen.reclamarParaSincronizar(
        ambito, operacion.dato.operationId, this.leaseId, DURACION_LEASE_MS,
      )
      : await this.almacenSanidad.reclamarParaSincronizar(
        ambito, operacion.dato.operationId, this.leaseId, DURACION_LEASE_MS,
      )
    return dato ? { ...operacion, dato } as OperacionSincronizable : null
  }

  private async enviar(
    ambito: AmbitoOperacionLocal,
    operacion: OperacionSincronizable,
    accessToken: string,
    signal: AbortSignal,
  ): Promise<ResultadoRegistroAnimal | ConfirmacionSanitariaLocal> {
    if (operacion.tipoOperacion === 'CREAR_ANIMAL') {
      return this.registrar(
        ambito.ganaderiaId,
        operacion.dato.payload,
        accessToken,
        signal,
      )
    }
    if (operacion.tipoOperacion === 'REGISTRAR_VACUNACION') {
      return this.registrarVacuna(
        ambito.ganaderiaId,
        operacion.dato.animalId,
        accessToken,
        operacion.dato.payload as SolicitudVacunacion,
        signal,
      )
    }
    return this.registrarEvento(
      ambito.ganaderiaId,
      operacion.dato.animalId,
      accessToken,
      operacion.dato.payload as SolicitudEventoSanitario,
      signal,
    )
  }

  private async finalizar(
    ambito: AmbitoOperacionLocal,
    operacion: OperacionSincronizable,
    confirmacion: ResultadoRegistroAnimal | ConfirmacionSanitariaLocal,
  ): Promise<void> {
    if (operacion.tipoOperacion === 'CREAR_ANIMAL') {
      await this.almacen.finalizarSincronizacion(
        ambito,
        operacion.dato.operationId,
        this.leaseId,
        confirmacion as ResultadoRegistroAnimal,
      )
      return
    }
    await this.almacenSanidad.finalizarSincronizacion(
      ambito,
      operacion.dato.operationId,
      this.leaseId,
      confirmacion as ConfirmacionSanitariaLocal,
    )
  }

  private async fallar(
    ambito: AmbitoOperacionLocal,
    operacion: OperacionSincronizable,
    clasificacion: ReturnType<typeof clasificarFallo>,
  ): Promise<void> {
    if (operacion.tipoOperacion === 'CREAR_ANIMAL') {
      await this.almacen.fallarSincronizacion(
        ambito,
        operacion.dato.operationId,
        this.leaseId,
        clasificacion.estadoLocal,
        clasificacion.errorSeguro,
      )
      return
    }
    await this.almacenSanidad.fallarSincronizacion(
      ambito,
      operacion.dato.operationId,
      this.leaseId,
      clasificacion.estadoLocal,
      clasificacion.errorSeguro,
    )
  }

  private async actualizarEstado(
    ambito: AmbitoOperacionLocal,
    operacion: OperacionSincronizable,
    estado: EstadoOperacionLocal,
    error: CodigoErrorSeguroOperacion,
  ): Promise<void> {
    if (operacion.tipoOperacion === 'CREAR_ANIMAL') {
      await this.almacen.actualizarEstado(
        ambito, operacion.dato.operationId, estado, error,
      )
      return
    }
    await this.almacenSanidad.actualizarEstado(
      ambito, operacion.dato.operationId, estado, error,
    )
  }

  private async listar(ambito: AmbitoOperacionLocal): Promise<OperacionSincronizable[]> {
    try {
      const [animales, sanitarias] = await Promise.all([
        this.almacen.listarPendientes(ambito),
        this.almacenSanidad.listarPendientes(ambito),
      ])
      return [
        ...animales.map(dato => ({
          tipoOperacion: 'CREAR_ANIMAL' as const,
          dato,
        })),
        ...sanitarias.map(dato => ({
          tipoOperacion: dato.clase === 'VACUNACION'
            ? 'REGISTRAR_VACUNACION' as const
            : 'REGISTRAR_EVENTO_SANITARIO' as const,
          dato,
        })),
      ].sort((a, b) => a.dato.creadoEnLocal.localeCompare(b.dato.creadoEnLocal))
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
  }

  private async marcarAmbito(
    ambito: AmbitoOperacionLocal,
    estado: EstadoOperacionLocal,
    error: CodigoErrorSeguroOperacion,
  ): Promise<void> {
    let pendientes: OperacionSincronizable[]
    try {
      pendientes = await this.listar(ambito)
    } catch {
      throw new ErrorSincronizacion('almacenamiento')
    }
    try {
      await Promise.all(pendientes
        .filter(operacion => operacion.dato.estado !== 'CONFLICTO'
          && !(operacion.dato.estado === 'RECHAZADO'
            && operacion.dato.ultimoError === 'VALIDACION'))
        .map(operacion => this.actualizarEstado(
          ambito, operacion, estado, error,
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

function esCandidata(
  operacion: OperacionPendienteLocal | EventoSanitarioPendienteLocal,
): boolean {
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
  const fallo = error instanceof ErrorAnimal || error instanceof ErrorSanidad
    ? error : new ErrorAnimal('conexion')
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
  if (fallo.tipo === 'seguridad') {
    return {
      estadoLocal: 'RECHAZADO',
      errorSeguro: 'PROTOCOLO',
      estadoResultado: 'RECHAZADA',
      detenerLote: true,
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
    errorSeguro: fallo.tipo === 'conexion' ? 'RED' : 'SERVIDOR',
    estadoResultado: 'ERROR_REINTENTABLE',
    detenerLote: false,
    reintentoDespuesMs: fallo instanceof ErrorAnimal ? fallo.retryAfterMs : undefined,
  }
}

function resultadoLocal(
  operacion: OperacionSincronizable,
  estado: EstadoResultadoOperacion,
  reintentoDespuesMs?: number,
  codigoError?: CodigoErrorSeguroOperacion,
): ResultadoOperacionSincronizacion {
  return {
    operationId: operacion.dato.operationId,
    animalId: operacion.dato.animalId,
    tipoOperacion: operacion.tipoOperacion,
    estado,
    confirmadaRemotamente: false,
    ...(reintentoDespuesMs !== undefined && { reintentoDespuesMs }),
    ...(codigoError !== undefined && { codigoError }),
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

export { ServicioSincronizacionAnimales as ServicioSincronizacionPendientes }

export function sincronizarPendientesManualmente(
  sesion: Pick<SesionActiva, 'usuarioId' | 'accessToken' | 'expiresAt'>,
  ganaderiaId: string,
  signal?: AbortSignal,
): Promise<ResultadoSincronizacionManual> {
  return servicio.sincronizar(sesion, ganaderiaId, signal)
}
