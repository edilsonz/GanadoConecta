import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { PesosPanel } from './PesosPanel'
import { solicitudPrivada } from './solicitudPrivada'

vi.mock(import('./solicitudPrivada'), async original => ({
  ...await original(), solicitudPrivada: vi.fn(),
}))
const props = { ganaderiaId: 'g', animalId: 'a', token: 'sintético', onSesionInvalida: vi.fn() }

it('consulta el último peso, gráfico y diferencia para un lector', async () => {
  const actual = { id: 'b', kilos: 82, fecha: '2026-01-03' }
  vi.mocked(solicitudPrivada).mockResolvedValueOnce({
    ultimo: actual, mediciones: [{ id: 'a', kilos: 75, fecha: '2026-01-01' }, actual],
  })
  render(<PesosPanel {...props} puedeEscribir={false} />)
  expect(await screen.findByRole('img', { name: /Evolución del peso/ })).toBeTruthy()
  expect(screen.getByText('Variación frente al 2026-01-01: 7 kg')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Guardar peso' })).toBeNull()
})

it('distingue el fallo de una respuesta vacía y permite actualizar', async () => {
  vi.mocked(solicitudPrivada).mockRejectedValueOnce(new Error('red'))
    .mockResolvedValueOnce({ ultimo: null, mediciones: [] })
  render(<PesosPanel {...props} puedeEscribir />)
  expect(await screen.findByRole('alert')).toBeTruthy()
  expect(screen.queryByText('Aún no hay mediciones de peso.')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar pesos' }))
  expect(await screen.findByText('Aún no hay mediciones de peso.')).toBeTruthy()
})
