import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ReportesPanel } from './ReportesPanel'
import { consultarReporte, type ReporteGanadero } from './reportesApi'
import { listarAnimales, type Animal } from '../ganado/animalesApi'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'

vi.mock('./reportesApi', () => ({ consultarReporte: vi.fn() }))
vi.mock('../ganado/animalesApi', async importOriginal => ({ ...await importOriginal<object>(), listarAnimales: vi.fn() }))
const props = { ganaderiaId: 'g', token: 'sintetico', onSesionInvalida: vi.fn() }
const vacio: ReporteGanadero = {
  sanidad: { total: 0, porTipo: [] },
  reproduccion: { montas: 0, inseminaciones: 0, evaluaciones: 0, confirmadas: 0, noConfirmadas: 0,
    indeterminadas: 0, partos: 0, crias: 0 },
  peso: { total: 0, ultimo: null, mediciones: [] },
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(listarAnimales).mockResolvedValue({
    items: [{ animalId: 'a', nombre: 'Luna de prueba' } as Animal], siguienteCursor: null, consultadoEn: '2026-11-21T00:00Z',
  })
  vi.mocked(consultarReporte).mockResolvedValue(vacio)
})

it('diferencia vacío real e historial que requiere seleccionar el animal', async () => {
  render(<ReportesPanel {...props} />)
  expect(await screen.findByText('No hay eventos sanitarios confirmados para este filtro.')).toBeVisible()
  expect(screen.getByText('No hay registros reproductivos para este filtro.')).toBeVisible()
  expect(screen.getByText('Elige un animal para consultar su evolución de peso.')).toBeVisible()
})

it('aplica los filtros y muestra indicadores, gráfico y variación procedentes del servidor', async () => {
  render(<ReportesPanel {...props} />)
  await screen.findByRole('option', { name: 'Luna de prueba' })
  const m = { id: 'peso-2', fecha: '2026-09-30', kilos: 150, variacion: 15 }
  vi.mocked(consultarReporte).mockResolvedValue({
    ...vacio, sanidad: { total: 1, porTipo: [{ tipo: 'VACUNACION', total: 1 }] },
    reproduccion: { ...vacio.reproduccion, montas: 1 },
    peso: { total: 2, ultimo: m, mediciones: [{ id: 'peso-1', fecha: '2026-09-01', kilos: 135, variacion: null }, m] },
  })
  fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-01' } })
  fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-30' } })
  fireEvent.change(screen.getByLabelText('Animal'), { target: { value: 'a' } })
  fireEvent.click(screen.getByRole('button', { name: 'Consultar reporte' }))
  await waitFor(() => expect(consultarReporte).toHaveBeenLastCalledWith('g', 'sintetico', {
    desde: '2026-09-01', hasta: '2026-09-30', animalId: 'a',
  }))
  expect(await screen.findByText('Vacunaciones')).toBeVisible()
  expect(screen.getByText('Montas')).toBeVisible()
  expect(screen.getByRole('img', { name: /Evolución del peso/ })).toBeVisible()
  expect(within(screen.getByRole('table')).getByText('+15')).toBeVisible()
  expect(within(screen.getByRole('table')).getByText('Sin anterior')).toBeVisible()
})

it('no presenta un 403 como un reporte vacío', async () => {
  vi.mocked(consultarReporte).mockRejectedValue(new ErrorSolicitudPrivada(403))
  render(<ReportesPanel {...props} />)
  expect(await screen.findByText('No tienes acceso a este reporte.')).toBeVisible()
  expect(screen.queryByText('No hay registros reproductivos para este filtro.')).not.toBeInTheDocument()
})
