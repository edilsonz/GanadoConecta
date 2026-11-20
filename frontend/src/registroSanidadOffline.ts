import {
  ServicioAutorizacionOffline,
  type ResultadoAutorizacionOffline,
} from './autorizacionOffline'
import {
  AlmacenEventosSanitariosPendientesIndexedDb,
  type AmbitoSanitarioLocal,
  type EventoSanitarioPendienteLocal,
} from './eventosSanitariosPendientesDb'
import type { SolicitudEventoSanitario } from './historialSanitarioApi'

type Autorizador = Pick<ServicioAutorizacionOffline, 'evaluarEscrituraLocal'>
type Almacen = Pick<AlmacenEventosSanitariosPendientesIndexedDb,
  'guardarPendiente' | 'listarPendientes'>

export class ServicioRegistroSanidadOffline {
  constructor(
    private readonly autorizador: Autorizador = new ServicioAutorizacionOffline(),
    private readonly almacen: Almacen = new AlmacenEventosSanitariosPendientesIndexedDb(),
  ) {}

  async guardar(
    ambito: AmbitoSanitarioLocal,
    animalId: string,
    payload: SolicitudEventoSanitario,
  ): Promise<{
    autorizacion: ResultadoAutorizacionOffline['estado']
    operacion: EventoSanitarioPendienteLocal | null
  }> {
    const resultado = await this.autorizador.evaluarEscrituraLocal(
      ambito.usuarioId,
      ambito.ganaderiaId,
    )
    if (resultado.estado !== 'AUTORIZADA_ESCRITURA') {
      return { autorizacion: resultado.estado, operacion: null }
    }
    return {
      autorizacion: resultado.estado,
      operacion: await this.almacen.guardarPendiente(ambito, animalId, payload),
    }
  }

  async listar(ambito: AmbitoSanitarioLocal): Promise<{
    autorizacion: ResultadoAutorizacionOffline['estado']
    operaciones: EventoSanitarioPendienteLocal[]
  }> {
    const resultado = await this.autorizador.evaluarEscrituraLocal(
      ambito.usuarioId,
      ambito.ganaderiaId,
    )
    if (resultado.estado !== 'AUTORIZADA_ESCRITURA') {
      return { autorizacion: resultado.estado, operaciones: [] }
    }
    return {
      autorizacion: resultado.estado,
      operaciones: await this.almacen.listarPendientes(ambito),
    }
  }
}

const servicio = new ServicioRegistroSanidadOffline()

export const guardarEventoSanitarioPendienteAutorizado = (
  ambito: AmbitoSanitarioLocal,
  animalId: string,
  payload: SolicitudEventoSanitario,
) => servicio.guardar(ambito, animalId, payload)

export const listarEventosSanitariosPendientesAutorizados = (
  ambito: AmbitoSanitarioLocal,
) => servicio.listar(ambito)
