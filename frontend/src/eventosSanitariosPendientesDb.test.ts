import { describe, expect, it } from 'vitest'
import {
  AlmacenEventosSanitariosPendientesIndexedDb,
  type AmbitoSanitarioLocal,
} from './eventosSanitariosPendientesDb'
import { AlmacenOperacionesPendientesIndexedDb } from './operacionesPendientesDb'
import { FabricaIndexedDbFalsa } from './test/indexedDbFalso'
import type { SolicitudEventoSanitario } from './historialSanitarioApi'
import { abrirBaseOffline, ALMACEN_EVENTOS_SANITARIOS } from './indexedDbOffline'

const usuario1 = '11111111-1111-4111-8111-111111111111'
const usuario2 = '22222222-2222-4222-8222-222222222222'
const ganaderia1 = '33333333-3333-4333-8333-333333333333'
const ganaderia2 = '44444444-4444-4444-8444-444444444444'
const animal1 = '55555555-5555-4555-8555-555555555555'
const animal2 = '66666666-6666-4666-8666-666666666666'
const operacion1 = '77777777-7777-4777-8777-777777777777'
const operacion2 = '88888888-8888-4888-8888-888888888888'
const evento1 = '99999999-9999-4999-8999-999999999999'
const evento2 = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'

const ambito1: AmbitoSanitarioLocal = { usuarioId: usuario1, ganaderiaId: ganaderia1 }

function payload(
  operationId = operacion1,
  eventId = evento1,
): SolicitudEventoSanitario {
  return {
    operationId,
    eventId,
    schemaVersion: 1,
    tipo: 'DESPARASITACION',
    fecha: '2026-11-20',
    descripcion: 'Hecho informado por el responsable',
  }
}

function crearAlmacen(fabrica: FabricaIndexedDbFalsa) {
  return new AlmacenEventosSanitariosPendientesIndexedDb({
    indexedDB: fabrica.factory,
    nombreBase: 'prueba-sanidad-offline',
    ahora: () => new Date('2026-11-20T12:00:00Z'),
  })
}

describe('eventos sanitarios pendientes en IndexedDB', () => {
  it('conserva UUID, operationId y payload después de reabrir', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const almacen = crearAlmacen(fabrica)
    const guardada = await almacen.guardarPendiente(ambito1, animal1, payload())
    await almacen.cerrar()

    const reabierto = crearAlmacen(fabrica)
    await expect(reabierto.listarPendientes(ambito1)).resolves.toEqual([guardada])
  })

  it('aísla las operaciones por usuario y ganadería', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    await almacen.guardarPendiente(ambito1, animal1, payload())
    await almacen.guardarPendiente(
      { usuarioId: usuario2, ganaderiaId: ganaderia2 },
      animal2,
      payload(operacion2, evento2),
    )

    await expect(almacen.listarPendientes(ambito1)).resolves.toHaveLength(1)
    await expect(almacen.listarPendientes({
      usuarioId: usuario1,
      ganaderiaId: ganaderia2,
    })).resolves.toEqual([])
  })

  it('es idempotente y rechaza reutilizar operationId con otro contenido', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const primera = await almacen.guardarPendiente(ambito1, animal1, payload())
    const mismoContenidoOtroOrden = {
      descripcion: 'Hecho informado por el responsable',
      fecha: '2026-11-20',
      tipo: 'DESPARASITACION',
      schemaVersion: 1,
      eventId: evento1,
      operationId: operacion1,
    } as SolicitudEventoSanitario

    await expect(almacen.guardarPendiente(ambito1, animal1, mismoContenidoOtroOrden))
      .resolves.toEqual(primera)
    await expect(almacen.guardarPendiente(ambito1, animal1, {
      ...payload(), descripcion: 'Contenido distinto',
    })).rejects.toMatchObject({ codigo: 'OPERACION_CONFLICTIVA' })
    await expect(almacen.listarPendientes(ambito1)).resolves.toHaveLength(1)
  })

  it('no altera la cola de animales al ampliar la misma base', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const nombreBase = 'prueba-almacenes-compartidos'
    const animales = new AlmacenOperacionesPendientesIndexedDb({
      indexedDB: fabrica.factory,
      nombreBase,
    })
    await animales.guardarPendiente(ambito1, {
      operationId: operacion2,
      animalId: animal2,
      tipo: 'CREAR_ANIMAL',
      schemaVersion: 2,
      baseVersion: 0,
      sexo: 'HEMBRA',
    })
    const sanitarios = new AlmacenEventosSanitariosPendientesIndexedDb({
      indexedDB: fabrica.factory,
      nombreBase,
    })
    await sanitarios.guardarPendiente(ambito1, animal1, payload())

    await expect(animales.listarPendientes(ambito1)).resolves.toHaveLength(1)
    await expect(sanitarios.listarPendientes(ambito1)).resolves.toHaveLength(1)
  })

  it('propaga el fallo de escritura sin declarar guardado el registro', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    fabrica.fallarProximaEscritura()
    const almacen = crearAlmacen(fabrica)
    await expect(almacen.guardarPendiente(ambito1, animal1, payload()))
      .rejects.toMatchObject({ codigo: 'TRANSACCION_FALLIDA' })
    await expect(almacen.listarPendientes(ambito1)).resolves.toEqual([])
  })

  it('normaliza registros creados por IndexedDB V3 sin borrar el payload anterior', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    const db = await abrirBaseOffline(fabrica.factory, 'prueba-sanidad-offline')
    const tx = db.transaction(ALMACEN_EVENTOS_SANITARIOS, 'readwrite')
    tx.objectStore(ALMACEN_EVENTOS_SANITARIOS).add({
      ...ambito1,
      animalId: animal1,
      operationId: operacion1,
      eventId: evento1,
      payload: payload(),
      estado: 'PENDIENTE',
      creadoEnLocal: '2026-11-19T12:00:00Z',
      actualizadoEnLocal: '2026-11-19T12:00:00Z',
      clave: `${usuario1}:${ganaderia1}:${operacion1}`,
      contextoClave: `${usuario1}:${ganaderia1}`,
    })
    await new Promise<void>((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
    db.close()

    await expect(crearAlmacen(fabrica).listarPendientes(ambito1)).resolves
      .toEqual([expect.objectContaining({
        clase: 'EVENTO_SANITARIO',
        payload: payload(),
        estado: 'PENDIENTE',
        intentos: 0,
        leaseId: null,
        confirmacion: null,
      })])
  })

  it('guarda vacunaciones y conserva lease y comprobante idempotente', async () => {
    const almacen = crearAlmacen(new FabricaIndexedDbFalsa())
    const vacunacion = {
      operationId: operacion1,
      eventId: evento1,
      schemaVersion: 1 as const,
      nombreVacuna: 'Aplicación informada',
      fechaAplicacion: '2026-11-20',
    }
    await almacen.guardarPendiente(ambito1, animal1, vacunacion)
    const lease = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
    await expect(almacen.reclamarParaSincronizar(
      ambito1, operacion1, lease,
    )).resolves.toMatchObject({
      clase: 'VACUNACION', estado: 'SINCRONIZANDO', intentos: 1, leaseId: lease,
    })
    const confirmacion = {
      operationId: operacion1,
      eventId: evento1,
      resultado: 'REPROCESADO' as const,
      confirmadoEn: '2026-11-20T12:00:01Z',
    }
    await almacen.finalizarSincronizacion(ambito1, operacion1, lease, confirmacion)

    await expect(almacen.listarPendientes(ambito1)).resolves.toEqual([])
    await expect(almacen.obtener(ambito1, operacion1)).resolves.toMatchObject({
      estado: 'SINCRONIZADO', payload: vacunacion, confirmacion,
    })
  })

  it('recupera un lease vencido sin cambiar UUID ni payload', async () => {
    const fabrica = new FabricaIndexedDbFalsa()
    let ahora = new Date('2026-11-20T12:00:00Z')
    const almacen = new AlmacenEventosSanitariosPendientesIndexedDb({
      indexedDB: fabrica.factory,
      nombreBase: 'prueba-sanidad-offline',
      ahora: () => ahora,
    })
    await almacen.guardarPendiente(ambito1, animal1, payload())
    const lease1 = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
    const lease2 = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc'
    await almacen.reclamarParaSincronizar(ambito1, operacion1, lease1)
    await expect(almacen.reclamarParaSincronizar(ambito1, operacion1, lease2))
      .resolves.toBeNull()
    ahora = new Date('2026-11-20T12:01:01Z')
    await expect(almacen.reclamarParaSincronizar(ambito1, operacion1, lease2))
      .resolves.toMatchObject({
        operationId: operacion1, eventId: evento1, payload: payload(),
        leaseId: lease2, intentos: 2,
      })
  })
})
