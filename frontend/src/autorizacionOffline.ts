import {
  consultarAccesoGanaderia,
  ErrorGanaderia,
  type AccesoGanaderia,
} from './ganaderiasApi'
import type { SesionActiva } from './sesionApi'
import {
  AlmacenContextosOfflineIndexedDb,
  ErrorContextoOffline,
  type ContextoOffline,
  type ResultadoEvaluacionContexto,
} from './contextosOfflineDb'

export type ResultadoAutorizacionOffline = {
  estado:
    | ResultadoEvaluacionContexto['resultado']
    | 'RED_NO_DISPONIBLE'
    | 'SERVICIO_NO_DISPONIBLE'
  contexto: ContextoOffline | null
}

export class ErrorAutorizacionOffline extends Error {
  constructor(public readonly codigo: 'ALMACENAMIENTO_LOCAL') {
    super(codigo)
    this.name = 'ErrorAutorizacionOffline'
  }
}

type ConsultarAcceso = (
  ganaderiaId: string,
  accessToken: string,
) => Promise<AccesoGanaderia>

type OpcionesServicio = {
  almacen?: AlmacenContextosOfflineIndexedDb
  consultarAcceso?: ConsultarAcceso
}

/**
 * Barrera local conservadora. Sus datos son manipulables en el dispositivo y
 * nunca sustituyen la autorización real del backend antes de sincronizar.
 */
export class ServicioAutorizacionOffline {
  private readonly almacen: AlmacenContextosOfflineIndexedDb
  private readonly consultarAcceso: ConsultarAcceso

  constructor(opciones: OpcionesServicio = {}) {
    this.almacen = opciones.almacen ?? new AlmacenContextosOfflineIndexedDb()
    this.consultarAcceso = opciones.consultarAcceso ?? consultarAccesoGanaderia
  }

  async revalidarOnline(
    sesion: Pick<SesionActiva, 'usuarioId' | 'accessToken'>,
    ganaderiaId: string,
  ): Promise<ResultadoAutorizacionOffline> {
    try {
      const acceso = await this.consultarAcceso(ganaderiaId, sesion.accessToken)
      if (acceso.ganaderiaId !== ganaderiaId) {
        throw new ErrorGanaderia('seguridad')
      }
      return this.registrarAccesoConfirmado(sesion.usuarioId, acceso)
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      const fallo = error instanceof ErrorGanaderia
        ? error : new ErrorGanaderia('servicio')
      if (fallo.tipo === 'sesion') {
        await this.cambiarContextoSinOcultarError(
          () => this.almacen.requerirSesion(sesion.usuarioId, ganaderiaId),
        )
        return this.resultadoActual(
          sesion.usuarioId,
          ganaderiaId,
          'REQUIERE_SESION',
        )
      }
      if (fallo.tipo === 'seguridad') {
        await this.cambiarContextoSinOcultarError(
          () => this.almacen.bloquearAcceso(sesion.usuarioId, ganaderiaId),
        )
        return this.resultadoActual(
          sesion.usuarioId,
          ganaderiaId,
          'BLOQUEADA',
        )
      }
      if (fallo.tipo === 'conexion') {
        return this.resultadoActual(
          sesion.usuarioId,
          ganaderiaId,
          'RED_NO_DISPONIBLE',
        )
      }
      return this.resultadoActual(
        sesion.usuarioId,
        ganaderiaId,
        'SERVICIO_NO_DISPONIBLE',
      )
    }
  }

  async registrarAccesoConfirmado(
    usuarioId: string,
    acceso: AccesoGanaderia,
  ): Promise<ResultadoAutorizacionOffline> {
    try {
      const contexto = await this.almacen.guardarValidacion({
        usuarioId,
        ganaderiaId: acceso.ganaderiaId,
        rol: acceso.rol,
        estado: acceso.estado,
        validadoEn: acceso.validadoEn,
      })
      return contexto.rol === 'LECTOR'
        ? { estado: 'SOLO_LECTURA', contexto }
        : { estado: 'AUTORIZADA_ESCRITURA', contexto }
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      throw error
    }
  }

  async requerirRevalidacionLocal(
    usuarioId: string,
    ganaderiaId: string,
  ): Promise<void> {
    await this.cambiarContextoSinOcultarError(
      () => this.almacen.requerirSesion(usuarioId, ganaderiaId),
    )
  }

  async bloquearAccesoLocal(
    usuarioId: string,
    ganaderiaId: string,
  ): Promise<void> {
    await this.cambiarContextoSinOcultarError(
      () => this.almacen.bloquearAcceso(usuarioId, ganaderiaId),
    )
  }

  async evaluarEscrituraLocal(
    usuarioId: string,
    ganaderiaId: string,
  ): Promise<ResultadoAutorizacionOffline> {
    try {
      const evaluacion = await this.almacen.evaluarEscritura(
        usuarioId,
        ganaderiaId,
      )
      return { estado: evaluacion.resultado, contexto: evaluacion.contexto }
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      throw error
    }
  }

  async bloquearPorCierreOCambioDeCuenta(usuarioId: string): Promise<number> {
    try {
      return await this.almacen.bloquearUsuario(usuarioId)
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      throw error
    }
  }

  private async resultadoActual(
    usuarioId: string,
    ganaderiaId: string,
    estado: ResultadoAutorizacionOffline['estado'],
  ): Promise<ResultadoAutorizacionOffline> {
    try {
      return {
        estado,
        contexto: await this.almacen.obtener(usuarioId, ganaderiaId),
      }
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      throw error
    }
  }

  private async cambiarContextoSinOcultarError(
    accion: () => Promise<void>,
  ): Promise<void> {
    try {
      await accion()
    } catch (error) {
      if (error instanceof ErrorContextoOffline) {
        throw new ErrorAutorizacionOffline('ALMACENAMIENTO_LOCAL')
      }
      throw error
    }
  }
}
