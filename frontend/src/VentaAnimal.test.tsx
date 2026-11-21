import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { VentaAnimal } from './VentaAnimal'
import { solicitudPrivada } from './solicitudPrivada'

vi.mock(import('./solicitudPrivada'), async original => ({ ...await original(), solicitudPrivada: vi.fn() }))
afterEach(() => vi.unstubAllGlobals())

it('pide confirmar y conserva el identificador al reintentar', async () => {
  vi.stubGlobal('confirm', vi.fn(() => true))
  const confirmar = vi.fn()
  vi.mocked(solicitudPrivada).mockRejectedValueOnce(new Error('red'))
    .mockImplementationOnce(async (_ruta,_t,_m, datos) => ({
      operationId:(datos as {operationId:string}).operationId,animalId:'a',anunciosCerrados:1,
    }))
  render(<VentaAnimal animalId="a" ganaderiaId="g" token="sintético" onVenta={confirmar} onSesionInvalida={vi.fn()} />)
  fireEvent.click(screen.getByRole('button',{name:'Registrar como vendido'}))
  fireEvent.click(await screen.findByRole('button',{name:'Reintentar venta'}))
  await waitFor(() => expect(confirmar).toHaveBeenCalledOnce())
  expect(vi.mocked(solicitudPrivada).mock.calls[0]).toEqual(vi.mocked(solicitudPrivada).mock.calls[1])
})
