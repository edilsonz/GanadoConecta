import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccesoOfflineSeguro, PwaEstado, useEstadoConexion } from './PwaEstado'
import { registrarPwa, type EventosRegistroPwa } from './pwaRegistro'
import { useProteccionActualizacionPwa } from './pwaActividad'

vi.mock('./pwaRegistro', () => ({
  registrarPwa: vi.fn(),
}))

describe('estado e instalación PWA', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.mocked(registrarPwa).mockReset().mockReturnValue(vi.fn().mockResolvedValue(undefined))
  })

  function Actividad({ tipo }: { tipo: 'FORMULARIO' | 'SINCRONIZACION' }) {
    useProteccionActualizacionPwa(`pwa-estado-${tipo}`, tipo, true)
    return null
  }

  function Conexion() {
    return <output>{useEstadoConexion() ? 'online' : 'offline'}</output>
  }

  it('refleja la pérdida y recuperación de conexión del navegador', () => {
    render(<Conexion />)
    act(() => window.dispatchEvent(new Event('offline')))
    expect(screen.getByText('offline')).toBeInTheDocument()
    act(() => window.dispatchEvent(new Event('online')))
    expect(screen.getByText('online')).toBeInTheDocument()
  })

  it('muestra modo seguro sin sesión y no presenta instalación inexistente', () => {
    render(<PwaEstado conectado={false} sesionActiva={false} />)

    expect(screen.getByText('Aplicación sin conexión')).toBeInTheDocument()
    expect(screen.getByText(/Se necesita conexión para verificar la sesión/)).toBeInTheDocument()
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
    expect(await screen.findByText('Actualización aplicada correctamente')).toBeInTheDocument()
    expect(screen.queryByText('Nueva versión disponible')).not.toBeInTheDocument()
    expect(sessionStorage.getItem('ganadoconecta:pwa-actualizacion-aplicada')).toBe('1')
  })

  it('confirma una actualización aplicada después de la recarga sin repetir el aviso', () => {
    sessionStorage.setItem('ganadoconecta:pwa-actualizacion-aplicada', '1')

    render(<PwaEstado conectado sesionActiva={false} />)

    expect(screen.getByText('Actualización aplicada correctamente')).toBeInTheDocument()
    expect(sessionStorage.getItem('ganadoconecta:pwa-actualizacion-aplicada')).toBeNull()
  })

  it.each([
    ['FORMULARIO', /formulario con cambios sin guardar/],
    ['SINCRONIZACION', /sincronización manual en curso/],
  ] as const)('impide actualizar durante %s', async (tipo, mensaje) => {
    let eventos: EventosRegistroPwa | undefined
    const actualizar = vi.fn().mockResolvedValue(undefined)
    vi.mocked(registrarPwa).mockImplementation(recibidos => {
      eventos = recibidos
      return actualizar
    })
    render(<><Actividad tipo={tipo} /><PwaEstado conectado sesionActiva /></>)

    act(() => eventos?.onNuevaVersion())
    expect(await screen.findByText(mensaje)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Actualizar ahora' })).toBeDisabled()
    expect(actualizar).not.toHaveBeenCalled()
  })

  it('conserva la versión anterior y operaciones locales si falla la actualización', async () => {
    const actualizar = vi.fn().mockRejectedValue(new Error('fallo controlado'))
    let eventos: EventosRegistroPwa | undefined
    const borrarBase = vi.fn()
    vi.stubGlobal('indexedDB', { deleteDatabase: borrarBase })
    vi.mocked(registrarPwa).mockImplementation(recibidos => {
      eventos = recibidos
      return actualizar
    })
    render(<PwaEstado conectado sesionActiva />)

    act(() => eventos?.onNuevaVersion())
    fireEvent.click(await screen.findByRole('button', { name: 'Actualizar ahora' }))

    expect(await screen.findByText('Actualización no disponible o fallida')).toBeInTheDocument()
    expect(screen.getByText(/La versión anterior sigue disponible/)).toBeInTheDocument()
    expect(borrarBase).not.toHaveBeenCalled()
  })

  it('informa preparación offline y fallos sin afirmar acceso a datos privados', () => {
    let eventos: EventosRegistroPwa | undefined
    vi.mocked(registrarPwa).mockImplementation(recibidos => {
      eventos = recibidos
      return vi.fn().mockResolvedValue(undefined)
    })
    render(<PwaEstado conectado sesionActiva={false} />)

    act(() => eventos?.onListaOffline())
    expect(screen.getByText('Aplicación disponible sin conexión')).toBeInTheDocument()
    expect(screen.getByText(/no sustituye el inicio de sesión/)).toBeInTheDocument()

    act(() => eventos?.onError())
    expect(screen.getByText('Actualización no disponible o fallida')).toBeInTheDocument()
    expect(screen.getByText(/IndexedDB no se eliminaron/)).toBeInTheDocument()
  })

  it('explica la barrera segura al abrir desde cero sin conexión', () => {
    render(<AccesoOfflineSeguro />)
    expect(screen.getByRole('heading', { name: 'La aplicación está disponible.' }))
      .toBeInTheDocument()
    expect(screen.getByText(/no mostraremos ganaderías, animales ni operaciones locales/))
      .toBeInTheDocument()
    expect(screen.getByText(/operaciones pendientes se conservan, pero permanecen ocultas/))
      .toBeInTheDocument()
  })
})
