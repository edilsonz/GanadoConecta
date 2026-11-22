import { solicitudPrivada } from '../../shared/api/solicitudPrivada'
import type { TipoActividad } from '../calendario/agendaSanitariaApi'
import type { FiltrosReporte, MedicionReporte, ReporteGanadero } from '../reportes/reportesApi'

export type Analisis = Pick<ReporteGanadero, 'sanidad' | 'reproduccion'> & {
  periodo: { desde: string; hasta: string }; pesajes: MedicionReporte[]
}
export type Alertas = {
  fechaCorte: string
  avisos: { actividadId: string; tipo: TipoActividad; descripcion: string;
    fechaPrevista: string; estado: 'PENDIENTE' | 'VENCIDA' }[]
}

export type TemaAsistente = 'PESO' | 'SANIDAD' | 'REPRODUCCION' | 'AGENDA'
export type RespuestaAsistente = {
  estado: 'RESPUESTA' | 'INSUFICIENTE'
  texto: string
  fuentes: { registroId: string; tipo: string; fecha: string }[]
}

const ruta = (ganaderiaId: string) => `/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}`

export function consultarAnalisis(ganaderiaId: string, token: string, { desde, hasta, animalId }: FiltrosReporte) {
  const query = new URLSearchParams({ desde, hasta })
  if (animalId) query.set('animalId', animalId)
  return solicitudPrivada<Analisis>(`${ruta(ganaderiaId)}/analisis?${query}`, token)
}

export function consultarAlertas(ganaderiaId: string, token: string) {
  return solicitudPrivada<Alertas>(`${ruta(ganaderiaId)}/alertas`, token)
}

export function preguntarAsistente(ganaderiaId: string, token: string, pregunta: string, tema: TemaAsistente,
  { desde, hasta, animalId }: FiltrosReporte) {
  return solicitudPrivada<RespuestaAsistente>(`${ruta(ganaderiaId)}/asistente`, token, 'POST', {
    pregunta, tema, desde, hasta, ...(animalId ? { animalId } : {}),
  })
}
