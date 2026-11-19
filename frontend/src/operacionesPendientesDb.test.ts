import { describe, expect, it } from 'vitest'
import type { SolicitudRegistroAnimalLocal } from './operacionesPendientesDb'
import {
  AlmacenOperacionesPendientesIndexedDb,
  ErrorAlmacenamientoLocal,
} from './operacionesPendientesDb'
import { FabricaIndexedDbFalsa } from './test/indexedDbFalso'

const usuario1 = '11111111-1111-4111-8111-111111111111'
const usuario2 = '22222222-2222-4222-8222-222222222222'
const ganaderia1 = '33333333-3333-4333-8333-333333333333'
const ganaderia2 = '44444444-4444-4444-8444-444444444444'
const operacion1 = '55555555-5555-4555-8555-555555555555'
const operacion2 = '66666666-6666-4666-8666-666666666666'
const operacion3 = '77777777-7777-4777-8777-777777777777'
const animal1 = '88888888-8888-4888-8888-888888888888'
const animal2 = '99999999-9999-4999-8999-999999999999'
const animal3 = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'
const lease1 = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
const lease2 = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc'

function payload(
  operationId = operacion1,
  animalId = animal1,
): SolicitudRegistroAnimalLocal {
  return {
    operationId,
    animalId,
    tipo: 'CREAR_ANIMAL',
    schemaVersion: 2,
    baseVersion: 0,
    sexo: 'HEMBRA',
    nombre: 'Pendiente local',
  }
}

function crearAlmacen(
  fabrica: FabricaIndexedDbFalsa,
  ahora: () => Date = () => new Date('2026-11-19T12:00:00Z'),
) {
  return new AlmacenOperacionesPendientesIndexedDb({
    indexedDB: fabrica.factory,
    nombreBase: 'prueba-offline',
    ahora,
  })
}

describe('almacenamiento IndexedDB de operaciones pendientes', () => {
  it('guarda y recupera el mismo sobre después de cerrar y reabrir la base', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const primero = crearAlmacen(fabrica)
    const guardada = await primero.guardarPendiente(
      { usuarioId: usuario1, ganaderiaId: ganaderia1 },
      payload(),
    )
    await primero.cerrar()

    const reabierto = crearAlmacen(fabrica)
    const recuperadas = await reabierto.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia1,
    })

    expect(recuperadas).toHaveLength(1)
    expect(recuperadas[0]).toEqual(guardada)
    expect(recuperadas[0].operationId).toBe(operacion1)
    expect(recuperadas[0].animalId).toBe(animal1)
    expect(recuperadas[0].payload).toEqual(payload())
  })

  it('aísla las consultas por usuario y ganadería', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    await almacen.guardarPendiente(
      { usuarioId: usuario1, ganaderiaId: ganaderia1 },
      payload(),
    )
    await almacen.guardarPendiente(
      { usuarioId: usuario1, ganaderiaId: ganaderia2 },
      payload(operacion2, animal2),
    )
    await almacen.guardarPendiente(
      { usuarioId: usuario2, ganaderiaId: ganaderia1 },
      payload(operacion3, animal3),
    )

    await expect(almacen.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia1,
    })).resolves.toMatchObject([{ operationId: operacion1 }])
    await expect(almacen.obtener(
      { usuarioId: usuario2, ganaderiaId: ganaderia1 },
      operacion1,
    )).resolves.toBeNull()
  })

  it('hace idempotente el guardado local y rechaza cambios de identidad o cuerpo', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    const primera = await almacen.guardarPendiente(ambito, payload())
    const mismoContenidoConOtroOrden = {
      nombre: 'Pendiente local',
      sexo: 'HEMBRA',
      baseVersion: 0,
      schemaVersion: 2,
      tipo: 'CREAR_ANIMAL',
      animalId: animal1,
      operationId: operacion1,
    } as SolicitudRegistroAnimalLocal
    const repetida = await almacen.guardarPendiente(
      ambito,
      mismoContenidoConOtroOrden,
    )

    expect(repetida).toEqual(primera)
    await expect(almacen.guardarPendiente(ambito, {
      ...payload(),
      nombre: 'Otro contenido',
    })).rejects.toMatchObject({ codigo: 'OPERACION_CONFLICTIVA' })
    await expect(almacen.guardarPendiente(
      ambito,
      payload(operacion2, animal1),
    )).rejects.toMatchObject({ codigo: 'OPERACION_CONFLICTIVA' })
    await expect(almacen.listarPendientes(ambito)).resolves.toHaveLength(1)
  })

  it('actualiza solo estado y conserva pendientes después de 168 horas', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    let ahora = new Date('2026-11-19T12:00:00Z')
    const almacen = crearAlmacen(fabrica, () => ahora)
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    await almacen.guardarPendiente(ambito, payload())

    ahora = new Date('2026-11-26T13:00:00Z')
    const actualizada = await almacen.actualizarEstado(
      ambito,
      operacion1,
      'REQUIERE_SESION',
      'AUTENTICACION',
    )

    expect(actualizada.payload).toEqual(payload())
    expect(actualizada.estado).toBe('REQUIERE_SESION')
    expect(actualizada.ultimoError).toBe('AUTENTICACION')
    await expect(almacen.listarPendientes(ambito)).resolves.toHaveLength(1)

    await almacen.actualizarEstado(ambito, operacion1, 'SINCRONIZADO')
    await expect(almacen.listarPendientes(ambito)).resolves.toEqual([])
    await expect(almacen.obtener(ambito, operacion1)).resolves.toMatchObject({
      operationId: operacion1,
      estado: 'SINCRONIZADO',
    })
  })

  it('reclama atómicamente con lease y recupera un envío abandonado al vencer', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    let ahora = new Date('2026-11-19T12:00:00Z')
    const almacen = crearAlmacen(fabrica, () => ahora)
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    await almacen.guardarPendiente(ambito, payload())

    await expect(almacen.reclamarParaSincronizar(ambito, operacion1, lease1))
      .resolves.toMatchObject({
        estado: 'SINCRONIZANDO', leaseId: lease1, intentos: 1,
      })
    await expect(almacen.reclamarParaSincronizar(ambito, operacion1, lease2))
      .resolves.toBeNull()

    ahora = new Date('2026-11-19T12:01:01Z')
    await expect(almacen.reclamarParaSincronizar(ambito, operacion1, lease2))
      .resolves.toMatchObject({
        estado: 'SINCRONIZANDO', leaseId: lease2, intentos: 2,
      })
  })

  it('conserva el recibo idempotente y la evidencia local al confirmar', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    await almacen.guardarPendiente(ambito, payload())
    await almacen.reclamarParaSincronizar(ambito, operacion1, lease1)
    const confirmacion = {
      operationId: operacion1,
      animalId: animal1,
      resultado: 'REPROCESADO' as const,
      versionCreada: 1 as const,
      confirmadoEn: '2026-11-19T12:00:01Z',
      advertencias: [],
    }

    await expect(almacen.finalizarSincronizacion(
      ambito, operacion1, lease1, confirmacion,
    )).resolves.toMatchObject({
      estado: 'SINCRONIZADO', confirmacion, leaseId: null,
    })
    await expect(almacen.listarPendientes(ambito)).resolves.toEqual([])
    await expect(almacen.obtener(ambito, operacion1)).resolves.toMatchObject({
      payload: payload(), confirmacion,
    })
  })

  it('libera el lease con un error seguro sin cambiar cuerpo ni IDs', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    await almacen.guardarPendiente(ambito, payload())
    await almacen.reclamarParaSincronizar(ambito, operacion1, lease1)

    await expect(almacen.fallarSincronizacion(
      ambito,
      operacion1,
      lease1,
      'ERROR_REINTENTABLE',
      'RED',
    )).resolves.toMatchObject({
      estado: 'ERROR_REINTENTABLE', ultimoError: 'RED',
      operationId: operacion1, animalId: animal1, payload: payload(),
      leaseId: null, leaseHasta: null,
    })
  })

  it('rechaza campos desconocidos para no persistir credenciales', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const inseguro = {
      ...payload(),
      accessToken: 'no-debe-guardarse',
      password: 'no-debe-guardarse',
    } as unknown as SolicitudRegistroAnimalLocal

    await expect(almacen.guardarPendiente(
      { usuarioId: usuario1, ganaderiaId: ganaderia1 },
      inseguro,
    )).rejects.toBeInstanceOf(ErrorAlmacenamientoLocal)
    await expect(almacen.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia1,
    })).resolves.toEqual([])
  })

  it('propaga el fallo de almacenamiento y no presenta la operación como guardada', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const almacen = crearAlmacen(fabrica)
    const ambito = { usuarioId: usuario1, ganaderiaId: ganaderia1 }
    fabrica.fallarProximaEscritura()

    await expect(almacen.guardarPendiente(ambito, payload()))
      .rejects.toMatchObject({ codigo: 'TRANSACCION_FALLIDA' })
    await expect(almacen.listarPendientes(ambito)).resolves.toEqual([])
  })

  it('informa de forma segura cuando IndexedDB no está disponible', async () => {
    const almacen = new AlmacenOperacionesPendientesIndexedDb()
    await expect(almacen.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia1,
    })).rejects.toMatchObject({ codigo: 'INDEXEDDB_NO_DISPONIBLE' })
  })
})
