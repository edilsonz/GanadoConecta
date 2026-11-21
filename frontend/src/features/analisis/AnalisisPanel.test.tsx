import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { listarAnimales, type Animal } from '../ganado/animalesApi'
import { consultarAlertas, consultarAnalisis, type Analisis } from './analisisApi'
import { AnalisisPanel } from './AnalisisPanel'

vi.mock('./analisisApi', () => ({ consultarAlertas: vi.fn(), consultarAnalisis: vi.fn() }))
vi.mock('../ganado/animalesApi', async original => ({ ...await original<object>(), listarAnimales: vi.fn() }))

const props = { ganaderiaId: 'ganaderia-a', token: 'sintetico', onSesionInvalida: vi.fn() }
const vacio: Analisis = {
  periodo: { desde: '2026-09-01', hasta: '2026-09-30' }, sanidad: { total: 0, porTipo: [] },
  reproduccion: { montas: 0, inseminaciones: 0, evaluaciones: 0, confirmadas: 0, noConfirmadas: 0,
    indeterminadas: 0, partos: 0, crias: 0 }, pesajes: [],
}
const pagina = { items: [] as Animal[], siguienteCursor: null, consultadoEn: '2026-09-30T00:00:00Z' }

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(consultarAnalisis).mockResolvedValue(vacio)
  vi.mocked(consultarAlertas).mockResolvedValue({ fechaCorte: '2026-09-30', avisos: [] })
  vi.mocked(listarAnimales).mockResolvedValue(pagina)
})

it('identifica ausencia de datos y disponibilidad real del asistente', async () => {
  render(<AnalisisPanel {...props} />)
  expect(await screen.findByText('No hay eventos sanitarios en el período.')).toBeVisible()
  expect(screen.getByText('No hay eventos reproductivos en el período.')).toBeVisible()
  expect(await screen.findByText('No hay actividades de hoy o vencidas por atender.')).toBeVisible()
  const asistente = screen.getByRole('region', { name: 'Asistente conversacional' })
  expect(within(asistente).getByText(/No disponible/)).toBeVisible()
  expect(within(asistente).queryByRole('textbox')).toBeNull()
})

it('pagina animales y muestra el resultado confirmado del intervalo seleccionado', async () => {
  vi.mocked(listarAnimales).mockResolvedValueOnce({ ...pagina, siguienteCursor: 'cursor' })
    .mockResolvedValueOnce({ ...pagina, items: [{ animalId: 'animal-a', nombre: 'Animal sintético' } as Animal] })
  render(<AnalisisPanel {...props} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Cargar más animales' }))
  await screen.findByRole('option', { name: 'Animal sintético' })
  expect(listarAnimales).toHaveBeenLastCalledWith('ganaderia-a', 'sintetico', 'cursor')
  vi.mocked(consultarAnalisis).mockResolvedValue({
    ...vacio, sanidad: { total: 2, porTipo: [{ tipo: 'VACUNACION', total: 2 }] },
    reproduccion: { ...vacio.reproduccion, partos: 1, crias: 2 },
    pesajes: [{ id: 'a', fecha: '2026-09-01', kilos: 140, variacion: null },
      { id: 'b', fecha: '2026-09-30', kilos: 150, variacion: 10 }],
  })
  fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-09-01' } })
  fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-09-30' } })
  fireEvent.change(screen.getByLabelText('Animal'), { target: { value: 'animal-a' } })
  fireEvent.click(screen.getByRole('button', { name: 'Consultar análisis' }))
  expect(await screen.findByText('Mayor frecuencia registrada: Vacunaciones (2 por tipo).')).toBeVisible()
  expect(screen.getByText(/Período del.*inclusive/, { exact: false })).toBeVisible()
  expect(screen.getByText('10 kg')).toBeVisible()
  expect(consultarAnalisis).toHaveBeenLastCalledWith('ganaderia-a', 'sintetico',
    { desde: '2026-09-01', hasta: '2026-09-30', animalId: 'animal-a' })
})

it('distingue fallo recuperable de vacío y retira avisos si se revoca el acceso', async () => {
  vi.mocked(consultarAlertas).mockRejectedValueOnce(new ErrorSolicitudPrivada(503))
    .mockResolvedValueOnce({ fechaCorte: '2026-09-30', avisos: [{ actividadId: 'a', tipo: 'CAMPANA_VACUNACION',
      fechaPrevista: '2026-09-29', descripcion: '<b>Seguimiento sintético</b>', estado: 'VENCIDA' }] })
    .mockRejectedValueOnce(new ErrorSolicitudPrivada(403))
  const { container } = render(<AnalisisPanel {...props} />)
  expect(await screen.findByText('No se pudo consultar las alertas.')).toBeVisible()
  expect(screen.queryByText('No hay actividades de hoy o vencidas por atender.')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar alertas' }))
  expect(await screen.findByText('Vencida · Campaña de vacunación')).toBeVisible()
  expect(screen.getByText(/<b>Seguimiento sintético<\/b>/)).toBeVisible()
  expect(container.querySelector('b')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar alertas' }))
  expect(await screen.findByText('No tienes acceso a las alertas.')).toBeVisible()
  expect(screen.queryByText(/Seguimiento sintético/)).toBeNull()
})

it('descarta solicitudes antiguas al cambiar de ganadería y delega la sesión vencida', async () => {
  let resolverAnterior: (datos: Analisis) => void = () => {}
  vi.mocked(consultarAnalisis).mockReturnValueOnce(new Promise(resolve => { resolverAnterior = resolve }))
    .mockRejectedValueOnce(new ErrorSolicitudPrivada(401))
  const vista = render(<AnalisisPanel key="a" {...props} />)
  vista.rerender(<AnalisisPanel key="b" {...props} ganaderiaId="ganaderia-b" />)
  resolverAnterior(vacio)
  await waitFor(() => expect(props.onSesionInvalida).toHaveBeenCalledOnce())
  expect(screen.queryByText('No hay eventos sanitarios en el período.')).toBeNull()
})
