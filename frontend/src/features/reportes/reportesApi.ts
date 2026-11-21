import { solicitudPrivada } from '../../shared/api/solicitudPrivada'

export type FiltrosReporte = { desde: string; hasta: string; animalId: string }
export type MedicionReporte = { id: string; fecha: string; kilos: number; variacion: number | null }
export type ReporteGanadero = {
  sanidad: { total: number; porTipo: { tipo: string; total: number }[] }
  reproduccion: { montas: number; inseminaciones: number; evaluaciones: number; confirmadas: number;
    noConfirmadas: number; indeterminadas: number; partos: number; crias: number }
  peso: { total: number; ultimo: MedicionReporte | null; mediciones: MedicionReporte[] }
}

export function consultarReporte(ganaderiaId: string, token: string, filtros: FiltrosReporte) {
  const query = new URLSearchParams({ desde: filtros.desde, hasta: filtros.hasta })
  if (filtros.animalId) query.set('animalId', filtros.animalId)
  return solicitudPrivada<ReporteGanadero>(`/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}/reportes?${query}`, token)
}
