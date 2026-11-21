import { beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarAccesoGanaderia, crearGanaderia, ErrorGanaderia, listarGanaderias } from './ganaderiasApi'

function respuesta(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

const ganaderia = {
  id: '123e4567-e89b-42d3-a456-426614174000',
  nombre: 'Los Pinos',
  estado: 'ACTIVA',
  rol: 'PROPIETARIO',
} as const

describe('API de ganaderías', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('lista con JWT en memoria y sin caché', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, [ganaderia]))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listarGanaderias('jwt-memoria')).resolves.toEqual([ganaderia])
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/ganaderias', expect.objectContaining({
      method: 'GET',
      cache: 'no-store',
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-memoria' }),
    }))
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('crea con UUIDv4, JWT y CSRF sin persistir credenciales', async () => {
    vi.spyOn(crypto, 'randomUUID').mockReturnValue(ganaderia.id)
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf' }))
      .mockResolvedValueOnce(respuesta(201, ganaderia))
    vi.stubGlobal('fetch', fetchMock)

    await expect(crearGanaderia('Los Pinos', 'jwt-memoria')).resolves.toEqual(ganaderia)
    expect(fetchMock.mock.calls[1]).toEqual([
      '/api/v1/ganaderias',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer jwt-memoria',
          'X-CSRF-TOKEN': 'csrf',
        }),
        body: JSON.stringify({ id: ganaderia.id, nombre: 'Los Pinos' }),
      }),
    ])
    expect(localStorage.length).toBe(0)
  })

  it('renueva CSRF una vez y conserva el UUID para un replay seguro', async () => {
    vi.spyOn(crypto, 'randomUUID').mockReturnValue(ganaderia.id)
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-anterior' }))
      .mockResolvedValueOnce(respuesta(403, { codigo: 'CSRF_INVALIDO' }))
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-nuevo' }))
      .mockResolvedValueOnce(respuesta(200, ganaderia))
    vi.stubGlobal('fetch', fetchMock)

    await crearGanaderia('Los Pinos', 'jwt-memoria')

    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      body: JSON.stringify({ id: ganaderia.id, nombre: 'Los Pinos' }),
    }))
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({
      body: JSON.stringify({ id: ganaderia.id, nombre: 'Los Pinos' }),
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-nuevo' }),
    }))
  })

  it('clasifica sesión vencida y respuestas de listado inesperadas', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(401, { codigo: 'AUTENTICACION_REQUERIDA' })))
    await expect(listarGanaderias('jwt-vencido'))
      .rejects.toEqual(expect.objectContaining({ tipo: 'sesion' }))

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(200, [{ id: 'incompleta' }])))
    await expect(listarGanaderias('jwt'))
      .rejects.toBeInstanceOf(ErrorGanaderia)
  })

  it('revalida el acceso vigente de la ganadería seleccionada', async () => {
    const acceso = {
      ganaderiaId: ganaderia.id,
      rol: 'EDITOR',
      estado: 'ACTIVA',
      validadoEn: '2026-11-18T18:00:00Z',
    }
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, acceso))
    vi.stubGlobal('fetch', fetchMock)

    await expect(consultarAccesoGanaderia(ganaderia.id, 'jwt-memoria')).resolves.toEqual(acceso)
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/v1/ganaderias/${ganaderia.id}/acceso`,
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer jwt-memoria' }) }),
    )
  })
})
