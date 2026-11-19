import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccesoOfflineSeguro, PwaEstado } from './PwaEstado'
import { registrarPwa, type EventosRegistroPwa } from './pwaRegistro'

vi.mock('./pwaRegistro', () => ({
  registrarPwa: vi.fn(),
}))

describe('estado e instalación PWA', () => {
  beforeEach(() => {
    vi.mocked(registrarPwa).mockReset().mockReturnValue(vi.fn().mockResolvedValue(undefined))
  })

  it('muestra modo seguro sin sesión y no presenta instalación inexistente', () => {
    render(<PwaEstado conectado={false} sesionActiva={false} />)

    expect(screen.getByText('Aplicación sin conexión')).toBeInTheDocument()
    expect(screen.getByText(/recuperar conexión e iniciar sesión/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Instalar aplicación' })).not.toBeInTheDocument()
  })

  it('ofrece instalar solo después de beforeinstallprompt', async () => {
    const prompt = vi.fn().mockResolvedValue(undefined)
    const evento = Object.assign(new Event('beforeinstallprompt'), {
      prompt,
      userChoice: Promise.resolve({ outcome: 'accepted' as const, platform: 'web' }),
    })
    render(<PwaEstado conectado sesionActiva={false} />)
    expect(screen.queryByRole('button', { name: 'Instalar aplicación' })).not.toBeInTheDocument()

    act(() => window.dispatchEvent(evento))
    fireEvent.click(await screen.findByRole('button', { name: 'Instalar aplicación' }))

    await waitFor(() => expect(prompt).toHaveBeenCalledOnce())
    await waitFor(() => expect(
      screen.queryByRole('button', { name: 'Instalar aplicación' }),
    ).not.toBeInTheDocument())
  })

  it('avisa una actualización y espera confirmación antes de recargar', async () => {
    const actualizar = vi.fn().mockResolvedValue(undefined)
    let eventos: EventosRegistroPwa | undefined
    vi.mocked(registrarPwa).mockImplementation(recibidos => {
      eventos = recibidos
      return actualizar
    })
    render(<PwaEstado conectado sesionActiva />)

    act(() => eventos?.onNuevaVersion())
    expect(await screen.findByText('Nueva versión disponible')).toBeInTheDocument()
    expect(actualizar).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Actualizar ahora' }))
    await waitFor(() => expect(actualizar).toHaveBeenCalledWith(true))
  })

  it('informa preparación offline y fallos sin afirmar acceso a datos privados', () => {
    let eventos: EventosRegistroPwa | undefined
    vi.mocked(registrarPwa).mockImplementation(recibidos => {
      eventos = recibidos
      return vi.fn().mockResolvedValue(undefined)
    })
    render(<PwaEstado conectado sesionActiva={false} />)

    act(() => eventos?.onListaOffline())
    expect(screen.getByText('Recursos básicos disponibles sin conexión')).toBeInTheDocument()
    expect(screen.getByText(/no sustituye el inicio de sesión/)).toBeInTheDocument()

    act(() => eventos?.onError())
    expect(screen.getByText('No se prepararon los recursos offline')).toBeInTheDocument()
    expect(screen.getByText(/IndexedDB no se eliminaron/)).toBeInTheDocument()
  })

  it('explica la barrera segura al abrir desde cero sin conexión', () => {
    render(<AccesoOfflineSeguro />)
    expect(screen.getByRole('heading', { name: 'La aplicación está disponible.' }))
      .toBeInTheDocument()
    expect(screen.getByText(/no mostraremos ganaderías, animales ni operaciones locales/))
      .toBeInTheDocument()
  })
})
