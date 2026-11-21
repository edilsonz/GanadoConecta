import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CatalogoPanel } from './CatalogoPanel'
import { buscarCatalogo } from './catalogoApi'

vi.mock(import('./catalogoApi'), async original => ({ ...await original(), buscarCatalogo: vi.fn() }))

it('filtra resultados públicos y conserva los filtros al paginar', async () => {
  const anuncio = { id:'a', titulo:'Anuncio sintético', caracteristicas:'Descripción pública',
    ubicacionGeneral:'Lima', precioPen:120, sexo:'HEMBRA' as const, fotos:[], genealogia:[] }
  vi.mocked(buscarCatalogo).mockResolvedValueOnce({ items:[anuncio], siguientePagina:2 })
    .mockResolvedValueOnce({ items:[anuncio], siguientePagina:2 }).mockResolvedValueOnce({ items:[] })
  render(<CatalogoPanel />)
  fireEvent.click(screen.getByText('Ver anuncios de ganado'))
  expect(await screen.findByText('Anuncio sintético')).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Ubicación general'),{target:{value:'Lima'}})
  fireEvent.click(screen.getByRole('button',{name:'Buscar anuncios'}))
  await waitFor(() => expect(buscarCatalogo).toHaveBeenLastCalledWith({ubicacion:'Lima'},1))
  fireEvent.click(await screen.findByRole('button',{name:'Siguiente'}))
  expect(await screen.findByText('No se encontraron anuncios para estos filtros.')).toBeInTheDocument()
  expect(buscarCatalogo).toHaveBeenLastCalledWith({ubicacion:'Lima'},2)
})
