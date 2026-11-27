const {
  crearSolicitud, fechaHoyLima, formularioAnimalModificado, formularioInicial, validarFormulario,
} = require('../../src/features/ganado/formularioAnimal')

afterEach(() => jest.useRealTimers())

test('exige sexo y rechaza una categoría incompatible', () => {
  expect(validarFormulario(formularioInicial)).toHaveProperty('sexo')
  expect(validarFormulario({ ...formularioInicial, sexo: 'HEMBRA', categoria: 'TORO' }))
    .toHaveProperty('categoria')
  expect(validarFormulario({ ...formularioInicial, sexo: 'MACHO', categoria: 'TORO' })).toEqual({})
})

test('evalúa la fecha de nacimiento en Lima, incluso si en UTC ya es el día siguiente', () => {
  jest.useFakeTimers({ now: new Date('2026-11-28T03:00:00Z') })
  expect(fechaHoyLima()).toBe('2026-11-27')
  expect(validarFormulario({ ...formularioInicial, sexo: 'HEMBRA', fechaNacimiento: '2026-11-28' }))
    .toHaveProperty('fechaNacimiento')
  expect(validarFormulario({ ...formularioInicial, sexo: 'HEMBRA', fechaNacimiento: '2026-11-27' }))
    .toEqual({})
})

test.each([
  [{ coloresIdentificacionTextil: 'Rojo' }, 'ubicacionIdentificacionTextil'],
  [{ ubicacionIdentificacionTextil: 'IZQUIERDA' }, 'coloresIdentificacionTextil'],
  [{ nombre: '🟢'.repeat(101) }, 'nombre'],
  [{ raza: 'a\u200Bb' }, 'raza'],
])('comprueba el campo inválido de la ficha (%s)', (cambio, campo) => {
  expect(validarFormulario({ ...formularioInicial, sexo: 'HEMBRA', ...cambio })).toHaveProperty(campo)
})

test('conserva la identidad del animal, omite campos vacíos y normaliza texto', () => {
  const animalId = 'e7e43753-3a26-4f75-a866-ef5189e49b12'
  const formulario = { ...formularioInicial, sexo: 'HEMBRA', nombre: '  Jose\u0301 ',
    raza: ' ', observaciones: 'Nota\r\nSintética' }
  expect(formularioAnimalModificado(formularioInicial)).toBe(false)
  expect(formularioAnimalModificado(formulario)).toBe(true)
  expect(crearSolicitud(formulario, animalId)).toEqual({
    animalId, operationId: expect.stringMatching(/^[0-9a-f-]+-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/),
    tipo: 'CREAR_ANIMAL', schemaVersion: 2, baseVersion: 0, sexo: 'HEMBRA',
    nombre: 'José', observaciones: 'Nota\nSintética',
  })
})
