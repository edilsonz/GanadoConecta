import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { GanaderiasPanel } from './GanaderiasPanel'
import { crearGanaderia, ErrorGanaderia, listarGanaderias } from './ganaderiasApi'

vi.mock('./AnimalesPanel', () => ({
  AnimalesPanel: () => <section aria-label="Mis animales" />,
}))

vi.mock('./ganaderiasApi', async importOriginal => {
  const original = await importOriginal<typeof import('./ganaderiasApi')>()
  return {
    ...original,
    crearGanaderia: vi.fn(),
    listarGanaderias: vi.fn(),
  }
})

const sesion = {
  usuarioId: 'usuario-1',
  correo: 'productora@ejemplo.com',
  estado: 'ACTIVO' as const,
  accessToken: 'jwt-solo-memoria',
  expiresAt: '2099-11-17T22:00:00Z',
}
const propia = {
  id: '123e4567-e89b-42d3-a456-426614174000',
  nombre: 'Los Pinos',
  estado: 'ACTIVA' as const,
  rol: 'PROPIETARIO' as const,
}

describe('Panel de ganaderías', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.mocked(listarGanaderias).mockReset()
    vi.mocked(crearGanaderia).mockReset()
  })

  it('no consulta sin sesión y explica el requisito', () => {
    render(<GanaderiasPanel sesion={null} onSesionInvalida={vi.fn()} />)

    expect(screen.getByText(/Inicia sesión para crear/)).toBeInTheDocument()
    expect(listarGanaderias).not.toHaveBeenCalled()
  })

  it('representa el estado vacío autorizado', async () => {
    vi.mocked(listarGanaderias).mockResolvedValue([])
    render(<GanaderiasPanel sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText(/Aún no tienes ganaderías/)).toBeInTheDocument()
  })

  it('lista y selecciona una ganadería solo en memoria', async () => {
    vi.mocked(listarGanaderias).mockResolvedValue([propia])
    render(<GanaderiasPanel sesion={sesion} onSesionInvalida={vi.fn()} />)

    const opcion = await screen.findByRole('button', { name: /Los Pinos/ })
    fireEvent.click(opcion)

    expect(opcion).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText(/volverá a verificar tu membresía/)).toBeInTheDocument()
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('crea y selecciona sin duplicar envíos', async () => {
    vi.mocked(listarGanaderias).mockResolvedValue([])
    let resolver: ((valor: typeof propia) => void) | undefined
    vi.mocked(crearGanaderia).mockReturnValue(new Promise(resolve => { resolver = resolve }))
    render(<GanaderiasPanel sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no tienes ganaderías/)

    fireEvent.change(screen.getByLabelText('Nombre de la ganadería'), { target: { value: 'Los Pinos' } })
    const boton = screen.getByRole('button', { name: 'Crear ganadería' })
    fireEvent.click(boton)
    fireEvent.submit(boton.closest('form')!)
    expect(crearGanaderia).toHaveBeenCalledOnce()
    expect(crearGanaderia).toHaveBeenCalledWith('Los Pinos', 'jwt-solo-memoria')

    resolver?.(propia)
    const opcion = await screen.findByRole('button', { name: /Los Pinos/ })
    expect(opcion).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText(/creada y seleccionada correctamente/)).toBeInTheDocument()
  })

  it('invalida la sesión si el backend la rechaza', async () => {
    const invalidar = vi.fn()
    vi.mocked(listarGanaderias).mockRejectedValue(new ErrorGanaderia('sesion'))
    render(<GanaderiasPanel sesion={sesion} onSesionInvalida={invalidar} />)

    await waitFor(() => expect(invalidar).toHaveBeenCalledOnce())
    expect(screen.getByText(/Tu sesión terminó/)).toBeInTheDocument()
  })

  it('muestra un conflicto seguro de creación', async () => {
    vi.mocked(listarGanaderias).mockResolvedValue([])
    vi.mocked(crearGanaderia).mockRejectedValue(new ErrorGanaderia('conflicto'))
    render(<GanaderiasPanel sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no tienes ganaderías/)

    fireEvent.change(screen.getByLabelText('Nombre de la ganadería'), { target: { value: 'Los Pinos' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear ganadería' }))

    expect(await screen.findByText(/existe un conflicto/)).toBeInTheDocument()
  })
})
