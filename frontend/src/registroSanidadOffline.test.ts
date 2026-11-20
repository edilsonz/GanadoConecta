import { describe, expect, it, vi } from 'vitest'
import { ServicioRegistroSanidadOffline } from './registroSanidadOffline'
import type { SolicitudEventoSanitario } from './historialSanitarioApi'

const ambito = {
  usuarioId: '11111111-1111-4111-8111-111111111111',
  ganaderiaId: '22222222-2222-4222-8222-222222222222',
}
const animalId = '33333333-3333-4333-8333-333333333333'
const payload: SolicitudEventoSanitario = {
  operationId: '44444444-4444-4444-8444-444444444444',
  eventId: '55555555-5555-4555-8555-555555555555',
  schemaVersion: 1,
  tipo: 'REVISION_VETERINARIA',
  fecha: '2026-11-20',
  descripcion: 'Revisión informada',
}

describe('barrera de autorización para sanidad offline', () => {
  it('guarda únicamente con autorización local de escritura vigente', async () => {
    const autorizador = { evaluarEscrituraLocal: vi.fn().mockResolvedValue({
      estado: 'AUTORIZADA_ESCRITURA', contexto: {},
    }) }
    const operacion = {
      ...ambito, animalId, operationId: payload.operationId, eventId: payload.eventId,
      payload, estado: 'PENDIENTE' as const, creadoEnLocal: 'ahora', actualizadoEnLocal: 'ahora',
    }
    const almacen = {
      guardarPendiente: vi.fn().mockResolvedValue(operacion),
      listarPendientes: vi.fn().mockResolvedValue([operacion]),
    }
    const servicio = new ServicioRegistroSanidadOffline(autorizador, almacen)

    await expect(servicio.guardar(ambito, animalId, payload)).resolves
      .toEqual({ autorizacion: 'AUTORIZADA_ESCRITURA', operacion })
    expect(almacen.guardarPendiente).toHaveBeenCalledWith(ambito, animalId, payload)
  })

  it.each(['SOLO_LECTURA', 'REQUIERE_SESION', 'BLOQUEADA'] as const)(
    'bloquea almacenamiento y lectura cuando el estado es %s',
    async estado => {
      const autorizador = { evaluarEscrituraLocal: vi.fn().mockResolvedValue({
        estado, contexto: null,
      }) }
      const almacen = {
        guardarPendiente: vi.fn(),
        listarPendientes: vi.fn(),
      }
      const servicio = new ServicioRegistroSanidadOffline(autorizador, almacen)

      await expect(servicio.guardar(ambito, animalId, payload)).resolves
        .toEqual({ autorizacion: estado, operacion: null })
      await expect(servicio.listar(ambito)).resolves
        .toEqual({ autorizacion: estado, operaciones: [] })
      expect(almacen.guardarPendiente).not.toHaveBeenCalled()
      expect(almacen.listarPendientes).not.toHaveBeenCalled()
    },
  )
})
