import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RegistroCuenta } from './RegistroCuenta'

function respuesta(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

function completarFormulario(contrasena = 'contraseña-segura-15') {
  fireEvent.change(screen.getByRole('textbox', { name: 'Correo electrónico' }), {
    target: { value: ' Productora@Ejemplo.com ' },
  })
  const camposContrasena = screen.getAllByLabelText(/contraseña/i)
  fireEvent.change(camposContrasena[0], { target: { value: contrasena } })
  fireEvent.change(camposContrasena[1], { target: { value: contrasena } })
}

describe('Registro de cuenta', () => {
  it('valida correo, longitud Unicode y confirmación antes de llamar a la API', () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<RegistroCuenta />)

    fireEvent.change(screen.getByRole('textbox', { name: 'Correo electrónico' }), {
      target: { value: 'correo-invalido' },
    })
    const camposContrasena = screen.getAllByLabelText(/contraseña/i)
    fireEvent.change(camposContrasena[0], { target: { value: '🐄'.repeat(14) } })
    fireEvent.change(camposContrasena[1], { target: { value: 'distinta' } })
    fireEvent.click(screen.getByRole('button', { name: /Crear mi cuenta/ }))

    expect(screen.getByText('Ingresa un correo electrónico válido.')).toBeInTheDocument()
    expect(screen.getByText('La contraseña debe tener entre 15 y 128 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('obtiene CSRF, registra una cuenta y no simula una sesión iniciada', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-real' }))
      .mockResolvedValueOnce(respuesta(201, {
        id: '2cdaae19-0638-43ab-af09-e5c1925fe9f5',
        correo: 'productora@ejemplo.com',
        estado: 'ACTIVO',
      }))
    vi.stubGlobal('fetch', fetchMock)
    render(<RegistroCuenta />)
    completarFormulario()

    fireEvent.click(screen.getByRole('button', { name: /Crear mi cuenta/ }))

    expect(await screen.findByText('Cuenta creada correctamente.')).toBeInTheDocument()
    expect(screen.getByText('productora@ejemplo.com')).toBeInTheDocument()
    expect(screen.getByText(/Tu sesión no se ha iniciado/)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0]).toEqual([
      '/api/v1/auth/csrf',
      expect.objectContaining({ method: 'GET', credentials: 'same-origin', cache: 'no-store' }),
    ])
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/auth/registro')
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: 'POST',
      credentials: 'same-origin',
      cache: 'no-store',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-real' }),
      body: JSON.stringify({
        correo: 'Productora@Ejemplo.com',
        contrasena: 'contraseña-segura-15',
      }),
    }))
    expect(localStorage.length).toBe(0)
  })

  it('renueva un CSRF inválido una sola vez y conserva el mismo registro', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-anterior' }))
      .mockResolvedValueOnce(respuesta(403, { codigo: 'CSRF_INVALIDO' }))
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-nuevo' }))
      .mockResolvedValueOnce(respuesta(201, {
        id: 'edda4bbf-b721-47aa-810c-aae7d5979767',
        correo: 'productora@ejemplo.com',
        estado: 'ACTIVO',
      }))
    vi.stubGlobal('fetch', fetchMock)
    render(<RegistroCuenta />)
    completarFormulario()

    fireEvent.click(screen.getByRole('button', { name: /Crear mi cuenta/ }))

    expect(await screen.findByText('Cuenta creada correctamente.')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-nuevo' }),
      body: fetchMock.mock.calls[1][1]?.body,
    }))
  })

  it.each([
    [409, { codigo: 'REGISTRO_NO_DISPONIBLE' }, /correo podría estar registrado/],
    [429, { codigo: 'DEMASIADOS_INTENTOS' }, /límite temporal de intentos/],
  ])('muestra un error seguro para HTTP %s', async (status, body, message) => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf' }))
      .mockResolvedValueOnce(respuesta(status, body)))
    render(<RegistroCuenta />)
    completarFormulario()

    fireEvent.click(screen.getByRole('button', { name: /Crear mi cuenta/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.queryByText('Cuenta creada correctamente.')).not.toBeInTheDocument()
  })

  it('informa un problema de conexión y evita envíos duplicados mientras espera', async () => {
    let rechazar: ((reason?: unknown) => void) | undefined
    const pendiente = new Promise<Response>((_, reject) => { rechazar = reject })
    const fetchMock = vi.fn().mockReturnValue(pendiente)
    vi.stubGlobal('fetch', fetchMock)
    render(<RegistroCuenta />)
    completarFormulario()

    const boton = screen.getByRole('button', { name: /Crear mi cuenta/ })
    fireEvent.click(boton)
    fireEvent.submit(boton.closest('form')!)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: /Creando cuenta/ })).toBeDisabled()

    rechazar?.(new TypeError('Failed to fetch'))
    expect(await screen.findByRole('alert')).toHaveTextContent(/No fue posible conectar/)
    await waitFor(() => expect(screen.getByRole('button', { name: /Crear mi cuenta/ })).toBeEnabled())
  })
})
