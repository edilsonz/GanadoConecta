import { afterEach, describe, expect, it, vi } from 'vitest'
import { registrarEventoSanitario } from './historialSanitarioApi'

const solicitud = {
  operationId: '11111111-1111-4111-8111-111111111111',
  eventId: '22222222-2222-4222-8222-222222222222',
  schemaVersion: 1 as const,
  tipo: 'DESPARASITACION' as const,
  fecha: '2026-11-20',
  descripcion: 'Hecho',
}

afterEach(() => vi.unstubAllGlobals())

describe('clasificación del envío sanitario', () => {
  it('acepta y devuelve únicamente un comprobante que coincide con los UUID enviados', async () => {
    const confirmacion = {
      operationId: solicitud.operationId,
      eventId: solicitud.eventId,
      resultado: 'REPROCESADO',
      confirmadoEn: '2026-11-20T12:00:00Z',
    }
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify(confirmacion), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
    vi.stubGlobal('fetch', fetch)

    await expect(registrarEventoSanitario('g', 'a', 'token', solicitud))
      .resolves.toEqual(confirmacion)
  })

  it('rechaza como protocolo temporal una confirmación con otro operationId', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        operationId: '99999999-9999-4999-8999-999999999999',
        eventId: solicitud.eventId,
        resultado: 'APLICADO',
        confirmadoEn: '2026-11-20T12:00:00Z',
      }), { status: 201, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetch)

    await expect(registrarEventoSanitario('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'temporal' })
  })

  it('clasifica como conexión solo una caída real de fetch', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetch)

    await expect(registrarEventoSanitario('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'conexion' })
  })

  it('mantiene un HTTP 500 como fallo temporal y no como desconexión', async () => {
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ codigo: 'ERROR_INTERNO' }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      }))
    vi.stubGlobal('fetch', fetch)

    await expect(registrarEventoSanitario('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'temporal' })
  })

  it('trata un fallo 500 al obtener CSRF como servidor recuperable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response(null, {
      status: 500,
    })))

    await expect(registrarEventoSanitario('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'temporal' })
  })
})
