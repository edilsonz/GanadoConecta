import { fireEvent, render, screen, waitFor, cleanup } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { VacunacionesPanel } from './VacunacionesPanel'
import { listarVacunaciones, registrarVacunacion, ErrorSanidad } from './vacunacionesApi'
import {
  guardarEventoSanitarioPendienteAutorizado,
  listarEventosSanitariosPendientesAutorizados,
} from './registroSanidadOffline'
vi.mock('./vacunacionesApi', async original => ({ ...await original<typeof import('./vacunacionesApi')>(), listarVacunaciones: vi.fn(), registrarVacunacion: vi.fn() }))
vi.mock('./registroSanidadOffline', () => ({
  guardarEventoSanitarioPendienteAutorizado: vi.fn(),
  listarEventosSanitariosPendientesAutorizados: vi.fn(),
}))
const props = { usuarioId: 'u', ganaderiaId: 'g', animalId: 'a', accessToken: 'test', puedeEscribir: true, onSesionInvalida: vi.fn() }
beforeEach(() => { vi.resetAllMocks(); Object.defineProperty(navigator, 'onLine', { configurable: true, value: true }); vi.mocked(listarVacunaciones).mockResolvedValue([]); vi.mocked(registrarVacunacion).mockResolvedValue({ operationId: 'o', eventId: 'e', resultado: 'APLICADO', confirmadoEn: '2026-11-20T00:00:00Z' }); vi.mocked(listarEventosSanitariosPendientesAutorizados).mockResolvedValue({ autorizacion: 'AUTORIZADA_ESCRITURA', operaciones: [] }) })
afterEach(cleanup)
function llenar() {
  fireEvent.change(screen.getByLabelText('Nombre de vacuna *'), { target: { value: 'Aplicación documentada' } })
  fireEvent.change(screen.getByLabelText('Fecha de aplicación *'), { target: { value: '2026-01-01' } })
}
describe('Vacunaciones', () => {
  it('distingue vacío confirmado y error inicial', async () => {
    vi.mocked(listarVacunaciones).mockRejectedValueOnce(new Error('red'))
    render(<VacunacionesPanel {...props} />)
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar')
    expect(screen.queryByText('Aún no hay vacunaciones registradas.')).not.toBeInTheDocument()
    fireEvent.click(screen.getByText('Actualizar vacunaciones'))
    expect(await screen.findByText('Aún no hay vacunaciones registradas.')).toBeInTheDocument()
  })
  it('registra y actualiza el historial', async () => {
    render(<VacunacionesPanel {...props} />); llenar()
    fireEvent.click(screen.getByText('Guardar vacunación'))
    expect(await screen.findByText('Vacunación registrada correctamente.')).toBeInTheDocument()
    expect(registrarVacunacion).toHaveBeenCalledWith('g', 'a', 'test', expect.objectContaining({ schemaVersion: 1, nombreVacuna: 'Aplicación documentada' }))
  })
  it('conserva identidad y payload al reintentar respuesta perdida', async () => {
    vi.mocked(registrarVacunacion).mockRejectedValueOnce(new Error('red'))
    render(<VacunacionesPanel {...props} />); llenar()
    fireEvent.click(screen.getByText('Guardar vacunación'))
    fireEvent.click(await screen.findByText('Reintentar misma vacunación'))
    await waitFor(() => expect(registrarVacunacion).toHaveBeenCalledTimes(2))
    expect(vi.mocked(registrarVacunacion).mock.calls[0]).toEqual(vi.mocked(registrarVacunacion).mock.calls[1])
  })
  it('lector consulta sin formulario', async () => {
    render(<VacunacionesPanel {...props} puedeEscribir={false} />)
    await screen.findByText('Aún no hay vacunaciones registradas.')
    expect(screen.queryByText('Guardar vacunación')).not.toBeInTheDocument()
  })
  it('bloquea el reenvío tras revocación', async () => {
    vi.mocked(registrarVacunacion).mockRejectedValueOnce(new ErrorSanidad('permiso'))
    render(<VacunacionesPanel {...props} />); llenar(); fireEvent.click(screen.getByText('Guardar vacunación'))
    expect(await screen.findByText('No tienes permiso vigente para registrar vacunaciones.')).toBeInTheDocument()
    expect(screen.getByText('Reintentar misma vacunación')).toBeDisabled()
  })
  it('guarda una vacunación offline separada del historial confirmado', async () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false })
    vi.mocked(guardarEventoSanitarioPendienteAutorizado).mockImplementation(async (ambito, animalId, payload) => ({
      autorizacion: 'AUTORIZADA_ESCRITURA',
      operacion: {
        ...ambito, clase: 'VACUNACION', animalId, operationId: payload.operationId,
        eventId: payload.eventId, payload, estado: 'PENDIENTE', ultimoError: null,
        creadoEnLocal: '2026-11-20T00:00:00Z', actualizadoEnLocal: '2026-11-20T00:00:00Z',
        intentos: 0, leaseId: null, leaseHasta: null, confirmacion: null,
      },
    }))
    render(<VacunacionesPanel {...props} />); llenar()
    fireEvent.click(screen.getByText('Guardar vacunación'))
    expect(await screen.findByText('Vacunación guardada en este dispositivo. Pendiente de sincronización.')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Vacunaciones pendientes' }))
      .toHaveTextContent('Aplicación documentada')
    expect(registrarVacunacion).not.toHaveBeenCalled()
  })
})
