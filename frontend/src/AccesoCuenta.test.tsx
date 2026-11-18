import { useState } from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccesoCuenta } from './AccesoCuenta'
import {
  cerrarSesionRemota,
  ErrorSesion,
  iniciarSesion,
  renovarSesionControlada,
  type SesionActiva,
} from './sesionApi'

vi.mock('./sesionApi', async importOriginal => {
  const original = await importOriginal<typeof import('./sesionApi')>()
  return {
    ...original,
    iniciarSesion: vi.fn(),
    renovarSesionControlada: vi.fn(),
    cerrarSesionRemota: vi.fn(),
  }
})

const sesion = {
  usuarioId: 'usuario-1',
  correo: 'productora@ejemplo.com',
  estado: 'ACTIVO' as const,
  accessToken: 'jwt-solo-memoria',
  expiresAt: '2099-11-17T22:00:00Z',
}

function AccesoControlado() {
  const [sesionActual, setSesionActual] = useState<SesionActiva | null>(null)
  return <AccesoCuenta sesion={sesionActual} onSesionChange={setSesionActual} />
}

function renderAcceso() {
  return render(<AccesoControlado />)
}

async function esperarFormulario() {
  return screen.findByRole('textbox', { name: 'Correo electrónico' })
}

function completarFormulario() {
  fireEvent.change(screen.getByRole('textbox', { name: 'Correo electrónico' }), {
    target: { value: 'productora@ejemplo.com' },
  })
  fireEvent.change(screen.getByLabelText('Contraseña'), {
    target: { value: 'contraseña-sintética-segura' },
  })
}

describe('Acceso de cuenta', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.mocked(renovarSesionControlada).mockRejectedValue(new ErrorSesion('sesion'))
    vi.mocked(cerrarSesionRemota).mockResolvedValue()
    vi.mocked(iniciarSesion).mockReset()
  })

  it('muestra el formulario sin error cuando no existe una sesión que restaurar', async () => {
    renderAcceso()

    expect(await esperarFormulario()).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('no inventa el correo cuando me restaura únicamente id y estado', async () => {
    vi.mocked(renovarSesionControlada).mockResolvedValue({ ...sesion, correo: undefined })
    renderAcceso()

    expect(await screen.findByText('Cuenta verificada')).toBeInTheDocument()
    expect(screen.queryByText('productora@ejemplo.com')).not.toBeInTheDocument()
  })

  it('inicia sesión, muestra el correo y mantiene Mi ganado pendiente', async () => {
    vi.mocked(iniciarSesion).mockResolvedValue(sesion)
    renderAcceso()
    await esperarFormulario()
    completarFormulario()

    fireEvent.click(screen.getByRole('button', { name: /Iniciar sesión/ }))

    expect(await screen.findByText('productora@ejemplo.com')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
    expect(screen.getByText(/Mi ganado sigue en preparación/)).toBeInTheDocument()
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('muestra el mismo error comprensible para credenciales inválidas', async () => {
    vi.mocked(iniciarSesion).mockRejectedValue(new ErrorSesion('credenciales'))
    renderAcceso()
    await esperarFormulario()
    completarFormulario()

    fireEvent.click(screen.getByRole('button', { name: /Iniciar sesión/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El correo o la contraseña no son válidos.',
    )
  })

  it('cierra la sesión local y remota', async () => {
    vi.mocked(renovarSesionControlada).mockResolvedValue(sesion)
    renderAcceso()
    expect(await screen.findByText('productora@ejemplo.com')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    await waitFor(() => expect(cerrarSesionRemota).toHaveBeenCalledOnce())
    expect(await esperarFormulario()).toBeInTheDocument()
    expect(localStorage.length).toBe(0)
  })

  it('bloquea localmente y conserva solo una marca no secreta si falla el cierre remoto', async () => {
    vi.mocked(renovarSesionControlada).mockResolvedValue(sesion)
    vi.mocked(cerrarSesionRemota).mockRejectedValue(new ErrorSesion('conexion'))
    renderAcceso()
    await screen.findByText('productora@ejemplo.com')

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Cierre remoto pendiente')
    expect(localStorage.getItem('ganadoconecta:salida-remota-pendiente')).toBe('1')
    expect(JSON.stringify(localStorage)).not.toContain('jwt-solo-memoria')
  })

  it('informa un error de conexión sin duplicar el envío', async () => {
    let rechazar: ((error: unknown) => void) | undefined
    vi.mocked(iniciarSesion).mockReturnValue(new Promise((_, reject) => { rechazar = reject }))
    renderAcceso()
    await esperarFormulario()
    completarFormulario()

    const boton = screen.getByRole('button', { name: /Iniciar sesión/ })
    fireEvent.click(boton)
    fireEvent.submit(boton.closest('form')!)
    expect(iniciarSesion).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: /Iniciando sesión/ })).toBeDisabled()

    rechazar?.(new ErrorSesion('conexion'))
    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible conectar')
  })
})
