import { fireEvent, render, screen, waitFor, cleanup } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { VacunacionesPanel } from './VacunacionesPanel'
import { listarVacunaciones, registrarVacunacion, ErrorSanidad } from './vacunacionesApi'
vi.mock('./vacunacionesApi', async original => ({ ...await original<typeof import('./vacunacionesApi')>(), listarVacunaciones: vi.fn(), registrarVacunacion: vi.fn() }))
const props = { ganaderiaId: 'g', animalId: 'a', accessToken: 'test', puedeEscribir: true, onSesionInvalida: vi.fn() }
beforeEach(() => { vi.resetAllMocks(); vi.mocked(listarVacunaciones).mockResolvedValue([]); vi.mocked(registrarVacunacion).mockResolvedValue() })
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
})
