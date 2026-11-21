import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  cerrarSesionRemota,
  ErrorSesion,
  iniciarSesion,
  renovarSesionControlada,
} from './sesionApi'

function respuesta(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

describe('API de sesión', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('inicia sesión con CSRF y conserva JWT, contraseña y refresh fuera del almacenamiento', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-login' }))
      .mockResolvedValueOnce(respuesta(200, {
        usuarioId: 'usuario-1',
        correo: 'productora@ejemplo.com',
        estado: 'ACTIVO',
        accessToken: 'jwt-solo-memoria',
        expiresAt: '2099-11-17T22:00:00Z',
      }))
    vi.stubGlobal('fetch', fetchMock)

    const sesion = await iniciarSesion('productora@ejemplo.com', 'contraseña-sintética-segura')

    expect(sesion.accessToken).toBe('jwt-solo-memoria')
    expect(fetchMock.mock.calls[1]).toEqual([
      '/api/v1/auth/login',
      expect.objectContaining({
        method: 'POST',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-login' }),
      }),
    ])
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(JSON.stringify(localStorage)).not.toContain('jwt-solo-memoria')
    expect(document.cookie).not.toContain('__Host-GC-Refresh')
  })

  it('restaura mediante renovación controlada y consulta de identidad', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-renovacion' }))
      .mockResolvedValueOnce(respuesta(200, {
        accessToken: 'jwt-renovado',
        expiresAt: '2099-11-17T22:00:00Z',
      }))
      .mockResolvedValueOnce(respuesta(200, { usuarioId: 'usuario-2', estado: 'ACTIVO' }))
    vi.stubGlobal('fetch', fetchMock)

    const sesion = await renovarSesionControlada()

    expect(sesion).toEqual(expect.objectContaining({
      usuarioId: 'usuario-2',
      accessToken: 'jwt-renovado',
      estado: 'ACTIVO',
    }))
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/auth/renovar')
    expect(fetchMock.mock.calls[2]).toEqual([
      '/api/v1/auth/me',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer jwt-renovado' }),
      }),
    ])
    expect(localStorage.length).toBe(0)
  })

  it('comparte una única renovación simultánea dentro de la pestaña', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-unico' }))
      .mockResolvedValueOnce(respuesta(200, {
        accessToken: 'jwt-unico',
        expiresAt: '2099-11-17T22:00:00Z',
      }))
      .mockResolvedValueOnce(respuesta(200, { usuarioId: 'usuario-3', estado: 'ACTIVO' }))
    vi.stubGlobal('fetch', fetchMock)

    const primera = renovarSesionControlada()
    const segunda = renovarSesionControlada()

    expect(primera).toBe(segunda)
    await expect(Promise.all([primera, segunda])).resolves.toHaveLength(2)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('clasifica credenciales incorrectas sin mostrar detalles del servidor', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf' }))
      .mockResolvedValueOnce(respuesta(401, {
        codigo: 'CREDENCIALES_INVALIDAS',
        mensajeSeguro: 'Las credenciales no son válidas.',
      })))

    await expect(iniciarSesion('persona@ejemplo.com', 'incorrecta'))
      .rejects.toEqual(expect.objectContaining({ tipo: 'credenciales' }))
  })

  it('clasifica el límite temporal sin revelar información interna', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf' }))
      .mockResolvedValueOnce(respuesta(429, {
        codigo: 'LIMITE_AUTENTICACION',
        mensajeSeguro: 'Inténtalo más tarde.',
      })))

    await expect(iniciarSesion('persona@ejemplo.com', 'contraseña-sintética-segura'))
      .rejects.toEqual(expect.objectContaining({ tipo: 'limite' }))
  })

  it('renueva una vez el CSRF inválido y conserva el cuerpo de login', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-anterior' }))
      .mockResolvedValueOnce(respuesta(403, { codigo: 'CSRF_INVALIDO' }))
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-nuevo' }))
      .mockResolvedValueOnce(respuesta(200, {
        usuarioId: 'usuario-4',
        correo: 'persona@ejemplo.com',
        estado: 'ACTIVO',
        accessToken: 'jwt-nuevo',
        expiresAt: '2099-11-17T22:00:00Z',
      }))
    vi.stubGlobal('fetch', fetchMock)

    await iniciarSesion('persona@ejemplo.com', 'contraseña-sintética-segura')

    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({
      body: JSON.stringify({
        correo: 'persona@ejemplo.com',
        contrasena: 'contraseña-sintética-segura',
      }),
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-nuevo' }),
    }))
  })

  it('cierra sesión con CSRF sin leer la cookie HttpOnly', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-logout' }))
      .mockResolvedValueOnce(respuesta(204, null))
    vi.stubGlobal('fetch', fetchMock)

    await cerrarSesionRemota()

    expect(fetchMock.mock.calls[1]).toEqual([
      '/api/v1/auth/logout',
      expect.objectContaining({
        method: 'POST',
        credentials: 'same-origin',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-logout' }),
      }),
    ])
  })

  it('no reintenta automáticamente una renovación con resultado de red incierto', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf' }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetchMock)

    await expect(renovarSesionControlada())
      .rejects.toBeInstanceOf(ErrorSesion)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
