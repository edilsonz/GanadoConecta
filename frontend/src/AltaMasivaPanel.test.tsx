import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { AltaMasivaPanel } from './AltaMasivaPanel'
import { ErrorAnimal, registrarAnimal } from './animalesApi'
import type { FormularioAnimal } from './formularioAnimal'

vi.mock(import('./animalesApi'), async original => ({
  ...await original(), registrarAnimal: vi.fn(),
}))
const enviar = vi.mocked(registrarAnimal)
const refrescar = vi.fn()

beforeEach(() => { enviar.mockReset(); refrescar.mockReset() })

function abrir() {
  render(<AltaMasivaPanel ganaderiaId="g" token="sintético" conectado categorias={{
    TERNERA: 'Ternera', VAQUILLA: 'Vaquilla', VACA: 'Vaca',
    TERNERO: 'Ternero', TORETE: 'Torete', TORO: 'Toro',
  }} alConfirmar={refrescar} onSesionInvalida={vi.fn()}
    inicial={{ sexo: '', nombre: '', categoria: '', areteFrente: '', areteReverso: '', raza: '',
      observaciones: '', coloresIdentificacionTextil: '', ubicacionIdentificacionTextil: '',
      fechaNacimiento: '', nacimientoEstimado: false }}
    validar={(datos): Record<string, string> => datos.sexo ? {} : { sexo: 'Selecciona el sexo.' }}
    preparar={(datos, id) => ({
      animalId: id, operationId: crypto.randomUUID(), sexo: datos.sexo as FormularioAnimal['sexo'] & ('MACHO' | 'HEMBRA'),
      tipo: 'CREAR_ANIMAL', schemaVersion: 2, baseVersion: 0,
    })} />)
  fireEvent.click(screen.getByText('Registrar varios animales'))
  for (let i = 0; i < 2; i++) fireEvent.click(screen.getByRole('button', { name: 'Añadir fila' }))
}

function exito() {
  enviar.mockImplementation(async (_g, s) => ({
    animalId: s.animalId, operationId: s.operationId, resultado: 'APLICADO',
    confirmadoEn: '2026-11-21T00:00:00Z', versionCreada: 1, advertencias: [],
  }))
}

it('requiere vista previa y envía solo las filas válidas sin exigir nombre ni arete', async () => {
  abrir()
  exito()
  fireEvent.change(screen.getAllByLabelText('Sexo *')[0], { target: { value: 'HEMBRA' } })
  expect(enviar).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button', { name: 'Revisar lote' }))
  expect(screen.getByText('Selecciona el sexo.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar 1 fila' }))
  expect(await screen.findByText('Confirmado.')).toBeTruthy()
  expect(enviar).toHaveBeenCalledOnce()
  expect(refrescar).toHaveBeenCalledOnce()
})

it('conserva el payload después de una respuesta incierta y omite la fila confirmada', async () => {
  abrir()
  exito()
  enviar.mockImplementationOnce(async (_g, s) => ({
    animalId: s.animalId, operationId: s.operationId, resultado: 'APLICADO',
    confirmadoEn: '2026-11-21T00:00:00Z', versionCreada: 1, advertencias: [],
  })).mockRejectedValueOnce(new ErrorAnimal('conexion'))
  screen.getAllByLabelText('Sexo *').forEach(select =>
    fireEvent.change(select, { target: { value: 'MACHO' } }))
  fireEvent.click(screen.getByRole('button', { name: 'Revisar lote' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar 2 filas' }))
  await screen.findByText(/Se conservaron los identificadores/)
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar 1 fila' }))
  await screen.findByText(/2 de 2 filas confirmadas/)
  expect(enviar).toHaveBeenCalledTimes(3)
  expect(enviar.mock.calls[2][1]).toEqual(enviar.mock.calls[1][1])
  expect(enviar.mock.calls[0][1].animalId).not.toBe(enviar.mock.calls[1][1].animalId)
})
