import { describe, expect, it } from 'vitest'
import { AlmacenOperacionesPendientesIndexedDb } from './operacionesPendientesDb'
import {
  AlmacenContextosOfflineIndexedDb,
  ErrorContextoOffline,
} from './contextosOfflineDb'
import { FabricaIndexedDbFalsa } from '../test/indexedDbFalso'

const usuario1 = '11111111-1111-4111-8111-111111111111'
const usuario2 = '22222222-2222-4222-8222-222222222222'
const ganaderia1 = '33333333-3333-4333-8333-333333333333'
const ganaderia2 = '44444444-4444-4444-8444-444444444444'
const operacion1 = '55555555-5555-4555-8555-555555555555'
const animal1 = '66666666-6666-4666-8666-666666666666'

function crearAlmacen(
  fabrica: FabricaIndexedDbFalsa,
  ahora: () => Date,
  nombreBase = 'contextos-offline-prueba',
) {
  return new AlmacenContextosOfflineIndexedDb({
    indexedDB: fabrica.factory,
    nombreBase,
    ahora,
  })
}

function validacion(
  rol: 'PROPIETARIO' | 'EDITOR' | 'LECTOR' = 'PROPIETARIO',
  usuarioId = usuario1,
  ganaderiaId = ganaderia1,
) {
  return {
    usuarioId,
    ganaderiaId,
    rol,
    estado: 'ACTIVA' as const,
    validadoEn: '2026-11-19T15:00:00Z',
  }
}

describe('contextos de autorización offline en IndexedDB', () => {
  it('autoriza escritura a propietario y editor dentro de 168 horas', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const ahora = () => new Date('2026-11-19T15:01:00Z')
    const almacen = crearAlmacen(fabrica, ahora)
    await almacen.guardarValidacion(validacion('PROPIETARIO'))
    await almacen.guardarValidacion(validacion('EDITOR', usuario1, ganaderia2))

    await expect(almacen.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toMatchObject({ resultado: 'AUTORIZADA_ESCRITURA' })
    await expect(almacen.evaluarEscritura(usuario1, ganaderia2))
      .resolves.toMatchObject({ resultado: 'AUTORIZADA_ESCRITURA' })
  })

  it('conserva el contexto de lector pero no lo autoriza a escribir', async () => {
    const almacen = crearAlmacen(
      new FabricaIndexedDbFalsa(),
      () => new Date('2026-11-19T15:01:00Z'),
    )
    await almacen.guardarValidacion(validacion('LECTOR'))

    await expect(almacen.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toMatchObject({
        resultado: 'SOLO_LECTURA',
        contexto: { rol: 'LECTOR', estado: 'VIGENTE' },
      })
  })

  it('rechaza ausencia, cambio de cuenta y cambio de ganadería', async () => {
    const almacen = crearAlmacen(
      new FabricaIndexedDbFalsa(),
      () => new Date('2026-11-19T15:01:00Z'),
    )
    await almacen.guardarValidacion(validacion())

    await expect(almacen.evaluarEscritura(usuario2, ganaderia1))
      .resolves.toEqual({ resultado: 'AUSENTE', contexto: null })
    await expect(almacen.evaluarEscritura(usuario1, ganaderia2))
      .resolves.toEqual({ resultado: 'AUSENTE', contexto: null })
  })

  it('vence a las 168 horas sin borrar operaciones pendientes ni sus UUID', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    let ahora = new Date('2026-11-19T15:01:00Z')
    const nombreBase = 'vencimiento-con-cola'
    const contextos = crearAlmacen(fabrica, () => ahora, nombreBase)
    const operaciones = new AlmacenOperacionesPendientesIndexedDb({
      indexedDB: fabrica.factory,
      nombreBase,
      ahora: () => ahora,
    })
    await contextos.guardarValidacion(validacion())
    await operaciones.guardarPendiente(
      { usuarioId: usuario1, ganaderiaId: ganaderia1 },
      {
        operationId: operacion1,
        animalId: animal1,
        tipo: 'CREAR_ANIMAL',
        schemaVersion: 2,
        baseVersion: 0,
        sexo: 'HEMBRA',
      },
    )

    ahora = new Date('2026-11-26T15:00:00Z')
    await expect(contextos.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toMatchObject({ resultado: 'VENCIDA' })
    await expect(operaciones.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia1,
    })).resolves.toMatchObject([{
      operationId: operacion1,
      animalId: animal1,
    }])
  })

  it('bloquea de forma conservadora un retroceso del reloj del dispositivo', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    let ahora = new Date('2026-11-19T16:00:00Z')
    const almacen = crearAlmacen(fabrica, () => ahora)
    await almacen.guardarValidacion(validacion())
    await almacen.evaluarEscritura(usuario1, ganaderia1)

    ahora = new Date('2026-11-19T15:59:59Z')
    await expect(almacen.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toMatchObject({ resultado: 'RELOJ_INCONSISTENTE' })
  })

  it('bloquea todos los contextos de la cuenta sin afectar otra cuenta', async () => {
    const almacen = crearAlmacen(
      new FabricaIndexedDbFalsa(),
      () => new Date('2026-11-19T15:01:00Z'),
    )
    await almacen.guardarValidacion(validacion())
    await almacen.guardarValidacion(validacion('EDITOR', usuario1, ganaderia2))
    await almacen.guardarValidacion(validacion('PROPIETARIO', usuario2, ganaderia2))

    await expect(almacen.bloquearUsuario(usuario1)).resolves.toBe(2)
    await expect(almacen.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toMatchObject({ resultado: 'REQUIERE_SESION' })
    await expect(almacen.evaluarEscritura(usuario1, ganaderia2))
      .resolves.toMatchObject({ resultado: 'REQUIERE_SESION' })
    await expect(almacen.evaluarEscritura(usuario2, ganaderia2))
      .resolves.toMatchObject({ resultado: 'AUTORIZADA_ESCRITURA' })
  })

  it('persiste al reabrir sin guardar credenciales ni secretos', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const ahora = () => new Date('2026-11-19T15:01:00Z')
    const primero = crearAlmacen(fabrica, ahora)
    await primero.guardarValidacion(validacion())
    await primero.cerrar()

    const reabierto = crearAlmacen(fabrica, ahora)
    const contexto = await reabierto.obtener(usuario1, ganaderia1)
    expect(contexto).toMatchObject({ usuarioId: usuario1, ganaderiaId: ganaderia1 })
    expect(JSON.stringify(contexto)).not.toMatch(/password|accessToken|refresh|jwt/i)
  })

  it('propaga fallos de IndexedDB y no afirma una autorización guardada', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const almacen = crearAlmacen(
      fabrica,
      () => new Date('2026-11-19T15:01:00Z'),
    )
    fabrica.fallarProximaEscritura()

    await expect(almacen.guardarValidacion(validacion()))
      .rejects.toBeInstanceOf(ErrorContextoOffline)
    await expect(almacen.evaluarEscritura(usuario1, ganaderia1))
      .resolves.toEqual({ resultado: 'AUSENTE', contexto: null })
  })
})
