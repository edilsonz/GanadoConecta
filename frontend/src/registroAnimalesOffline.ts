import type { SolicitudRegistroAnimal } from './animalesApi'
import {
  ServicioAutorizacionOffline,
  type ResultadoAutorizacionOffline,
} from './autorizacionOffline'
import type { AccesoGanaderia } from './ganaderiasApi'
import {
  AlmacenOperacionesPendientesIndexedDb,
  type OperacionPendienteLocal,
} from './operacionesPendientesDb'

const autorizacion = new ServicioAutorizacionOffline()
const operaciones = new AlmacenOperacionesPendientesIndexedDb()

type Ambito = { usuarioId: string; ganaderiaId: string }

export type ResultadoPendientesAutorizados = {
  autorizacion: ResultadoAutorizacionOffline['estado']
  contexto: ResultadoAutorizacionOffline['contexto']
  operaciones: OperacionPendienteLocal[]
}

export async function prepararAccesoOfflineConfirmado(
  usuarioId: string,
  acceso: AccesoGanaderia,
): Promise<ResultadoAutorizacionOffline> {
  return autorizacion.registrarAccesoConfirmado(usuarioId, acceso)
}

export async function listarPendientesAutorizados(
  ambito: Ambito,
): Promise<ResultadoPendientesAutorizados> {
  const resultado = await autorizacion.evaluarEscrituraLocal(
    ambito.usuarioId,
    ambito.ganaderiaId,
  )
  if (resultado.estado !== 'AUTORIZADA_ESCRITURA') {
    return {
      autorizacion: resultado.estado,
      contexto: resultado.contexto,
      operaciones: [],
    }
  }
  return {
    autorizacion: resultado.estado,
    contexto: resultado.contexto,
    operaciones: await operaciones.listarPendientes(ambito),
  }
}

export async function guardarRegistroPendienteAutorizado(
  ambito: Ambito,
  payload: SolicitudRegistroAnimal,
): Promise<{
  autorizacion: ResultadoAutorizacionOffline['estado']
  operacion: OperacionPendienteLocal | null
}> {
  const resultado = await autorizacion.evaluarEscrituraLocal(
    ambito.usuarioId,
    ambito.ganaderiaId,
  )
  if (resultado.estado !== 'AUTORIZADA_ESCRITURA') {
    return { autorizacion: resultado.estado, operacion: null }
  }
  return {
    autorizacion: resultado.estado,
    operacion: await operaciones.guardarPendiente(ambito, payload),
  }
}

export async function bloquearContextosOfflineUsuario(
  usuarioId: string,
): Promise<number> {
  return autorizacion.bloquearPorCierreOCambioDeCuenta(usuarioId)
}

export async function requerirRevalidacionOffline(
  ambito: Ambito,
): Promise<void> {
  return autorizacion.requerirRevalidacionLocal(
    ambito.usuarioId,
    ambito.ganaderiaId,
  )
}

export async function bloquearAccesoOffline(ambito: Ambito): Promise<void> {
  return autorizacion.bloquearAccesoLocal(
    ambito.usuarioId,
    ambito.ganaderiaId,
  )
}
