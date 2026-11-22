import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { preguntarAsistente } from './analisisApi'
import { AsistentePanel } from './AsistentePanel'

vi.mock('./analisisApi', () => ({ preguntarAsistente: vi.fn() }))
const props = { ganaderiaId: 'sintetica', token: 'en-memoria', animal: 'Animal sintético',
  filtros: { desde: '2026-09-01', hasta: '2026-09-30', animalId: 'a' }, onSesionInvalida: vi.fn() }

beforeEach(() => vi.resetAllMocks())

function preguntar(texto: string) {
  fireEvent.change(screen.getByLabelText('Pregunta'), { target: { value: texto } })
  fireEvent.click(screen.getByRole('button', { name: 'Preguntar' }))
}

it('muestra la selección de registros, el ámbito y las referencias', async () => {
  vi.mocked(preguntarAsistente).mockResolvedValue({ estado: 'RESPUESTA', texto: 'Variación registrada: +15 kg.',
    fuentes: [{ registroId: '1', tipo: 'Pesaje', fecha: '2026-09-01' },
      { registroId: '2', tipo: 'Pesaje', fecha: '2026-09-30' }] })
  render(<AsistentePanel {...props} />)
  preguntar('¿Cuánto cambió su peso?')
  expect(await screen.findByText('Variación registrada: +15 kg.')).toBeVisible()
  expect(within(screen.getByRole('list', { name: 'Registros de origen' })).getAllByRole('listitem')).toHaveLength(2)
  expect(preguntarAsistente).toHaveBeenCalledWith('sintetica', 'en-memoria', '¿Cuánto cambió su peso?', 'PESO', props.filtros)
})

it('distingue datos insuficientes, servicio caído y revocación', async () => {
  vi.mocked(preguntarAsistente).mockResolvedValueOnce({ estado: 'INSUFICIENTE', texto: 'No hay información suficiente.', fuentes: [] })
    .mockRejectedValueOnce(new ErrorSolicitudPrivada(503)).mockRejectedValueOnce(new ErrorSolicitudPrivada(403))
  render(<AsistentePanel {...props} />)
  preguntar('¿Hay registros?')
  expect(await screen.findByText('No hay información suficiente.')).toBeVisible()
  preguntar('¿Y su peso?')
  expect(await screen.findByText(/El asistente no está disponible/)).toBeVisible()
  preguntar('¿Sigue disponible?')
  expect(await screen.findByText('No tienes acceso al ámbito de esta consulta.')).toBeVisible()
  expect(screen.queryByText('No hay información suficiente.')).toBeNull()
})

it('requiere animal para peso y permite consultar el calendario general', async () => {
  vi.mocked(preguntarAsistente).mockResolvedValue({ estado: 'INSUFICIENTE', texto: 'No hay actividades.', fuentes: [] })
  render(<AsistentePanel {...props} animal="" filtros={{ ...props.filtros, animalId: '' }} />)
  preguntar('¿Cuánto pesa?')
  expect(screen.getByRole('alert')).toHaveTextContent('Elige un animal')
  expect(preguntarAsistente).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Tema'), { target: { value: 'AGENDA' } })
  preguntar('¿Y el calendario?')
  expect(await screen.findByText('No hay actividades.')).toBeVisible()
})
