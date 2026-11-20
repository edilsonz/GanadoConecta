import { afterEach, describe, expect, it, vi } from 'vitest'
import { registrarVacunacion } from './vacunacionesApi'

const solicitud = {
  operationId: '11111111-1111-4111-8111-111111111111',
  eventId: '22222222-2222-4222-8222-222222222222',
  schemaVersion: 1 as const,
  nombreVacuna: 'Aplicación documentada',
  fechaAplicacion: '2026-11-20',
}

afterEach(() => vi.unstubAllGlobals())

describe('comprobante HTTP de vacunación', () => {
  it('devuelve una confirmación idempotente válida', async () => {
    const confirmacion = {
      operationId: solicitud.operationId,
      eventId: solicitud.eventId,
      resultado: 'REPROCESADO',
      confirmadoEn: '2026-11-20T12:00:00Z',
    }
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify(confirmacion), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      })))

    await expect(registrarVacunacion('g', 'a', 'token', solicitud))
      .resolves.toEqual(confirmacion)
  })

  it('clasifica una caída real de fetch como conexión', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch')))

    await expect(registrarVacunacion('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'conexion' })
  })

  it('no acepta una confirmación con otro eventId', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'csrf' }), {
        status: 200, headers: { 'Content-Type': 'application/json' },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        operationId: solicitud.operationId,
        eventId: '99999999-9999-4999-8999-999999999999',
        resultado: 'APLICADO',
        confirmadoEn: '2026-11-20T12:00:00Z',
      }), { status: 201, headers: { 'Content-Type': 'application/json' } })))

    await expect(registrarVacunacion('g', 'a', 'token', solicitud))
      .rejects.toMatchObject({ tipo: 'temporal' })
  })
})
