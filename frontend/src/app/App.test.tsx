import { act, fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { useEstadoConexion } from '../pwa/PwaEstado'

vi.mock('../features/cuenta/AccesoCuenta', () => ({
  AccesoCuenta: () => <section aria-label="Acceso de cuenta" />,
}))

vi.mock('../features/ganaderias/GanaderiasPanel', () => ({
  GanaderiasPanel: () => <section aria-label="GanaderÃ­as" />,
}))

vi.mock('../pwa/PwaEstado', () => ({
  useEstadoConexion: vi.fn(() => true),
  PwaEstado: () => <aside aria-label="Estado PWA" />,
  AccesoOfflineSeguro: () => <section aria-label="Acceso offline seguro" />,
}))

vi.mock('../features/catalogo/CatalogoPanel', () => ({
  CatalogoPanel: () => <section aria-label="Catálogo público" />,
}))

function mockHealth(status = 'UP', ok = true) {
  const fetchMock = vi.fn().mockResolvedValue({ ok, json: async () => ({ status }) })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

describe('Inicio de GanadoConecta', () => {
  beforeEach(() => {
    vi.mocked(useEstadoConexion).mockReturnValue(true)
    window.history.replaceState(null, '', '/')
  })

  it('muestra el inicio público y reserva el espacio de ganadería a una sesión', async () => {
    mockHealth()
    render(<App />)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Todo conectado.')
    expect(screen.queryByRole('link', { name: 'Mi ganadería' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Catálogo público' })).toHaveAttribute('href', '#catalogo')
    expect(screen.getByRole('heading', { name: 'Crea tu cuenta.' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Correo electrónico' })).toBeInTheDocument()
    expect(screen.getByText(/PWA instalable/)).toBeInTheDocument()
    expect(await screen.findByText('Conexión con el servicio disponible')).toBeInTheDocument()
  })

  it('separa el catálogo del inicio y la cuenta', () => {
    mockHealth()
    render(<App />)
    act(() => {
      window.history.replaceState(null, '', '#catalogo')
      window.dispatchEvent(new HashChangeEvent('hashchange'))
    })
    expect(screen.getByRole('region', { name: 'Catálogo público' })).toBeVisible()
    expect(screen.queryByRole('heading', { name: 'Crea tu cuenta.' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Catálogo público' })).toHaveAttribute('aria-current', 'page')
  })

  it('representa un fallo de red sin inventar conexión y permite comprobar otra vez', async () => {
    const fetchMock = vi.fn().mockRejectedValueOnce(new Error('Red no disponible'))
      .mockResolvedValue({ ok: true, json: async () => ({ status: 'UP' }) })
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)
    expect(await screen.findByText('No se pudo comprobar la conexión')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Comprobar de nuevo/ }))
    expect(await screen.findByText('Conexión con el servicio disponible')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('no trata un HTTP fallido como conexión disponible', async () => {
    mockHealth('UP', false)
    render(<App />)
    expect(await screen.findByText('No se pudo comprobar la conexión')).toBeInTheDocument()
  })

  it('no trata un servicio DOWN como disponible', async () => {
    mockHealth('DOWN')
    render(<App />)
    expect(await screen.findByText('No se pudo comprobar la conexión')).toBeInTheDocument()
  })

  it('rechaza una respuesta de salud inesperada', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ detalle: 'sin estado' }) }))
    render(<App />)
    expect(await screen.findByText('No se pudo comprobar la conexión')).toBeInTheDocument()
  })

  it('sin conexión y sin sesión muestra la barrera segura y oculta flujos privados', () => {
    vi.mocked(useEstadoConexion).mockReturnValue(false)
    mockHealth()

    render(<App />)

    expect(screen.getByRole('region', { name: 'Acceso offline seguro' })).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Acceso de cuenta' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Crea tu cuenta.' })).not.toBeInTheDocument()
  })
})
