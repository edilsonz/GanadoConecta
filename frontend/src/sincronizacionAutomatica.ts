import type { SesionActiva } from './sesionApi'
import {
  ErrorSincronizacion,
  sincronizarPendientesManualmente,
  type ResultadoSincronizacionManual,
} from './sincronizacionAnimales'
import { ejecutarConProteccionActualizacionPwa } from './pwaActividad'

const ESPERAS_REINTENTO_MS = [2_000, 5_000, 15_000, 30_000] as const

export type TipoEstadoSincronizacionAutomatica =
  | 'DESACTIVADA'
  | 'ACTIVADA'
  | 'ESPERANDO_RECONEXION'
  | 'SINCRONIZANDO'
  | 'REINTENTO_PROGRAMADO'
  | 'SINCRONIZADA'
  | 'ACCION_REQUERIDA'
  | 'SESION_EXPIRADA'
  | 'PERMISO_INSUFICIENTE'

export type EstadoSincronizacionAutomatica = {
  tipo: TipoEstadoSincronizacionAutomatica
  intento?: number
  reintentoEnMs?: number
  sincronizadas?: number
}

export type ContextoSincronizacionAutomatica = {
  sesion: Pick<SesionActiva, 'usuarioId' | 'accessToken' | 'expiresAt'>
  ganaderiaId: string
  conectado: boolean
  hayPendientes: boolean
  puedeEscribir: boolean
  sincronizacionManualEnCurso: boolean
}

type Temporizador = ReturnType<typeof window.setTimeout>

type OpcionesSincronizacionAutomatica = {
  sincronizar?: typeof sincronizarPendientesManualmente
  alCambiarEstado?: (estado: EstadoSincronizacionAutomatica) => void
  alObtenerResultado?: (resultado: ResultadoSincronizacionManual) => void | Promise<void>
  aleatorio?: () => number
  programar?: (accion: () => void, demoraMs: number) => Temporizador
  cancelar?: (temporizador: Temporizador) => void
}

export class ControladorSincronizacionAutomatica {
  private readonly sincronizar: typeof sincronizarPendientesManualmente
  private readonly alCambiarEstado: NonNullable<OpcionesSincronizacionAutomatica['alCambiarEstado']>
  private readonly alObtenerResultado: NonNullable<OpcionesSincronizacionAutomatica['alObtenerResultado']>
  private readonly aleatorio: () => number
  private readonly programar: NonNullable<OpcionesSincronizacionAutomatica['programar']>
  private readonly cancelar: NonNullable<OpcionesSincronizacionAutomatica['cancelar']>
  private contexto?: ContextoSincronizacionAutomatica
  private temporizador?: Temporizador
  private activada = false
  private enCurso = false
  private intento = 0
  private generacion = 0

  constructor(opciones: OpcionesSincronizacionAutomatica = {}) {
    this.sincronizar = opciones.sincronizar ?? ((sesion, ganaderiaId, signal) =>
      ejecutarConProteccionActualizacionPwa(() =>
        sincronizarPendientesManualmente(sesion, ganaderiaId, signal)))
    this.alCambiarEstado = opciones.alCambiarEstado ?? (() => undefined)
    this.alObtenerResultado = opciones.alObtenerResultado ?? (() => undefined)
    this.aleatorio = opciones.aleatorio ?? Math.random
    this.programar = opciones.programar ?? ((accion, demora) => window.setTimeout(accion, demora))
    this.cancelar = opciones.cancelar ?? (temporizador => window.clearTimeout(temporizador))
  }

  estaActivada() {
    return this.activada
  }

  activar(contexto: ContextoSincronizacionAutomatica) {
    this.contexto = contexto
    this.activada = true
    this.intento = 0
    this.cancelarReintento()
    this.evaluar(true)
  }

  desactivar() {
    this.activada = false
    this.generacion += 1
    this.intento = 0
    this.cancelarReintento()
    this.publicar({ tipo: 'DESACTIVADA' })
  }

  detener() {
    this.desactivar()
    this.contexto = undefined
  }

  destruir() {
    this.activada = false
    this.generacion += 1
    this.cancelarReintento()
    this.contexto = undefined
  }

  actualizarContexto(contexto: ContextoSincronizacionAutomatica) {
    const anterior = this.contexto
    if (anterior && (anterior.sesion.usuarioId !== contexto.sesion.usuarioId
        || anterior.ganaderiaId !== contexto.ganaderiaId)) {
      this.detener()
      this.contexto = contexto
      return
    }
    this.contexto = contexto
    if (!this.activada || this.enCurso) return
    if (!contexto.conectado) {
      this.cancelarReintento()
      this.publicar({ tipo: 'ESPERANDO_RECONEXION' })
      return
    }
    if (this.temporizador) return
    const recuperoConexion = anterior?.conectado === false && contexto.conectado
    const aparecieronPendientes = anterior?.hayPendientes === false && contexto.hayPendientes
    const terminoManual = anterior?.sincronizacionManualEnCurso === true
      && !contexto.sincronizacionManualEnCurso
    if (recuperoConexion || aparecieronPendientes || terminoManual) {
      this.intento = 0
      this.evaluar(true)
    }
  }

  private evaluar(iniciar: boolean) {
    const contexto = this.contexto
    if (!this.activada || !contexto) return
    if (!contexto.puedeEscribir) {
      this.publicar({ tipo: 'PERMISO_INSUFICIENTE' })
      return
    }
    if (!contexto.conectado) {
      this.publicar({ tipo: 'ESPERANDO_RECONEXION' })
      return
    }
    if (!contexto.hayPendientes || contexto.sincronizacionManualEnCurso) {
      this.publicar({ tipo: 'ACTIVADA' })
      return
    }
    if (iniciar) void this.ejecutarIntento()
  }

  private async ejecutarIntento() {
    const contexto = this.contexto
    if (!this.activada || !contexto || this.enCurso
        || !contexto.conectado || !contexto.puedeEscribir
        || contexto.sincronizacionManualEnCurso || !contexto.hayPendientes) return
    this.enCurso = true
    this.intento += 1
    const generacion = this.generacion
    this.publicar({ tipo: 'SINCRONIZANDO', intento: this.intento })
    try {
      const resultado = await this.sincronizar(contexto.sesion, contexto.ganaderiaId)
      await Promise.resolve(this.alObtenerResultado(resultado)).catch(() => undefined)
      if (!this.activada || generacion !== this.generacion) return
      const transitorias = resultado.operaciones.filter(
        operacion => operacion.estado === 'ERROR_REINTENTABLE',
      )
      if (transitorias.length > 0) {
        const retryAfterMs = Math.max(0, ...transitorias
          .map(operacion => operacion.reintentoDespuesMs ?? 0))
        this.programarReintento(retryAfterMs)
      } else if (resultado.operaciones.some(operacion =>
        operacion.estado === 'REQUIERE_SESION')) {
        this.publicar({ tipo: 'SESION_EXPIRADA' })
      } else if (resultado.operaciones.some(operacion =>
        operacion.estado === 'RECHAZADA'
          || operacion.estado === 'CONFLICTO'
          || operacion.estado === 'ERROR_LOCAL')) {
        this.publicar({ tipo: 'ACCION_REQUERIDA' })
      } else if (resultado.sincronizadas > 0) {
        this.intento = 0
        this.publicar({
          tipo: 'SINCRONIZADA',
          sincronizadas: resultado.sincronizadas,
        })
      } else if (resultado.total === 0 && contexto.hayPendientes) {
        this.publicar({ tipo: 'ACCION_REQUERIDA' })
      } else {
        this.publicar({ tipo: 'ACTIVADA' })
      }
    } catch (error) {
      if (!this.activada || generacion !== this.generacion) return
      const tipo = error instanceof ErrorSincronizacion ? error.tipo : 'servicio'
      if (tipo === 'sesion') this.publicar({ tipo: 'SESION_EXPIRADA' })
      else if (tipo === 'permiso') this.publicar({ tipo: 'PERMISO_INSUFICIENTE' })
      else if (tipo === 'conexion' || tipo === 'servicio') this.programarReintento(0)
      else this.publicar({ tipo: 'ACCION_REQUERIDA' })
    } finally {
      this.enCurso = false
    }
  }

  private programarReintento(retryAfterMs: number) {
    if (!this.activada) return
    const contexto = this.contexto
    if (!contexto?.conectado) {
      this.publicar({ tipo: 'ESPERANDO_RECONEXION' })
      return
    }
    const indice = this.intento - 1
    const base = ESPERAS_REINTENTO_MS[indice]
    if (base === undefined) {
      this.publicar({ tipo: 'ACCION_REQUERIDA' })
      return
    }
    const jitter = Math.floor(Math.max(0, Math.min(1, this.aleatorio())) * 1_001)
    const demora = Math.max(base + jitter, retryAfterMs)
    this.cancelarReintento()
    this.publicar({
      tipo: 'REINTENTO_PROGRAMADO',
      intento: this.intento + 1,
      reintentoEnMs: demora,
    })
    const generacion = this.generacion
    this.temporizador = this.programar(() => {
      this.temporizador = undefined
      if (!this.activada || generacion !== this.generacion) return
      this.evaluar(true)
    }, demora)
  }

  private cancelarReintento() {
    if (this.temporizador !== undefined) this.cancelar(this.temporizador)
    this.temporizador = undefined
  }

  private publicar(estado: EstadoSincronizacionAutomatica) {
    this.alCambiarEstado(estado)
  }
}
