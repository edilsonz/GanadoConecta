import { describe, expect, it, vi } from 'vitest'
import { ErrorAnimal, type ResultadoRegistroAnimal } from './animalesApi'
import { ErrorGanaderia } from './ganaderiasApi'
import { AlmacenOperacionesPendientesIndexedDb } from './operacionesPendientesDb'
import {
  ErrorSincronizacion,
  ServicioSincronizacionAnimales,
} from './sincronizacionAnimales'
import { FabricaIndexedDbFalsa } from './test/indexedDbFalso'

const usuario = '11111111-1111-4111-8111-111111111111'
const otroUsuario = '22222222-2222-4222-8222-222222222222'
const ganaderia = '33333333-3333-4333-8333-333333333333'
const otraGanaderia = '44444444-4444-4444-8444-444444444444'
const operacion = '55555555-5555-4555-8555-555555555555'
const otraOperacion = '66666666-6666-4666-8666-666666666666'
const terceraOperacion = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
const animal = '77777777-7777-4777-8777-777777777777'
const otroAnimal = '88888888-8888-4888-8888-888888888888'
const tercerAnimal = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc'
const lease1 = '99999999-9999-4999-8999-999999999999'
const lease2 = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'
const instante = new Date('2026-11-19T18:00:00Z')

const sesion = {
  usuarioId: usuario,
  accessToken: 'jwt-solo-memoria',
  expiresAt: '2099-11-19T18:10:00Z',
}

const payload = {
  operationId: operacion,
  animalId: animal,
  tipo: 'CREAR_ANIMAL' as const,
  schemaVersion: 2 as const,
  baseVersion: 0 as const,
  sexo: 'HEMBRA' as const,
  nombre: 'Pendiente aislado',
  coloresIdentificacionTextil: 'rojo y blanco',
  ubicacionIdentificacionTextil: 'AMBAS' as const,
}

function confirmacion(
  resultado: ResultadoRegistroAnimal['resultado'] = 'APLICADO',
): ResultadoRegistroAnimal {
  return {
    operationId: operacion,
    animalId: animal,
    resultado,
    versionCreada: 1,
    confirmadoEn: '2026-11-19T18:00:01Z',
    advertencias: [],
  }
}

function crearAlmacen(fabrica = new FabricaIndexedDbFalsa()) {
  return new AlmacenOperacionesPendientesIndexedDb({
    indexedDB: fabrica.factory,
    nombreBase: 'sincronizacion-prueba',
    ahora: () => instante,
  })
}

function acceso(rol: 'PROPIETARIO' | 'EDITOR' | 'LECTOR' = 'PROPIETARIO') {
  return {
    ganaderiaId: ganaderia,
    rol,
    estado: 'ACTIVA' as const,
    validadoEn: instante.toISOString(),
  }
}

async function preparar(almacen: AlmacenOperacionesPendientesIndexedDb) {
  await almacen.guardarPendiente({ usuarioId: usuario, ganaderiaId: ganaderia }, payload)
}

describe('sincronización manual de animales pendientes', () => {
  it('revalida acceso y confirma exactamente el payload e identificadores originales', async () => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const registrar = vi.fn().mockResolvedValue(confirmacion())
    const consultarAcceso = vi.fn().mockResolvedValue(acceso('EDITOR'))
    const servicio = new ServicioSincronizacionAnimales({
      almacen, registrar, consultarAcceso, crearLeaseId: () => lease1,
    })

    const resultado = await servicio.sincronizar(sesion, ganaderia)

    expect(consultarAcceso).toHaveBeenCalledWith(
      ganaderia, sesion.accessToken, undefined,
    )
    expect(registrar).toHaveBeenCalledOnce()
    expect(registrar).toHaveBeenCalledWith(
      ganaderia, payload, sesion.accessToken, expect.any(AbortSignal),
    )
    expect(resultado).toMatchObject({
      total: 1, sincronizadas: 1, confirmadasRemotamente: 1, fallidas: 0,
    })
    await expect(almacen.listarPendientes({ usuarioId: usuario, ganaderiaId: ganaderia }))
      .resolves.toEqual([])
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({
        estado: 'SINCRONIZADO', payload, confirmacion: confirmacion(), intentos: 1,
      })
  })

  it.each([
    ['solo arete', { areteFrente: 'A-LOCAL-1' }],
    ['solo lana/cinta', {
      coloresIdentificacionTextil: 'azul',
      ubicacionIdentificacionTextil: 'IZQUIERDA',
    }],
    ['solo sexo obligatorio', {}],
  ] as const)('no altera el payload de identificación %s', async (_caso, opcionales) => {
    const almacen = crearAlmacen()
    const solicitud = {
      operationId: operacion,
      animalId: animal,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'MACHO' as const,
      ...opcionales,
    }
    await almacen.guardarPendiente(
      { usuarioId: usuario, ganaderiaId: ganaderia }, solicitud,
    )
    const registrar = vi.fn().mockResolvedValue(confirmacion())
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      crearLeaseId: () => lease1,
    })

    await servicio.sincronizar(sesion, ganaderia)

    expect(registrar.mock.calls[0][1]).toEqual(solicitud)
  })

  it('aísla estrictamente usuario y ganadería y no envía operaciones de otro ámbito', async () => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    await almacen.guardarPendiente(
      { usuarioId: otroUsuario, ganaderiaId: ganaderia },
      { ...payload, operationId: otraOperacion, animalId: otroAnimal },
    )
    await almacen.guardarPendiente(
      { usuarioId: usuario, ganaderiaId: otraGanaderia },
      { ...payload, operationId: terceraOperacion, animalId: tercerAnimal },
    )
    const registrar = vi.fn().mockResolvedValue(confirmacion())
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      crearLeaseId: () => lease1,
    })

    await servicio.sincronizar(sesion, ganaderia)

    expect(registrar).toHaveBeenCalledOnce()
    await expect(almacen.listarPendientes({ usuarioId: otroUsuario, ganaderiaId: ganaderia }))
      .resolves.toHaveLength(1)
    await expect(almacen.listarPendientes({ usuarioId: usuario, ganaderiaId: otraGanaderia }))
      .resolves.toHaveLength(1)
  })

  it.each([
    ['LECTOR', acceso('LECTOR'), undefined],
    ['revocada', undefined, new ErrorGanaderia('seguridad')],
  ] as const)('bloquea acceso %s antes de enviar', async (_caso, respuesta, error) => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const registrar = vi.fn()
    const bloquearAcceso = vi.fn().mockResolvedValue(undefined)
    const consultarAcceso = error
      ? vi.fn().mockRejectedValue(error) : vi.fn().mockResolvedValue(respuesta)
    const servicio = new ServicioSincronizacionAnimales({
      almacen, registrar, consultarAcceso, bloquearAcceso,
      crearLeaseId: () => lease1,
    })

    await expect(servicio.sincronizar(sesion, ganaderia))
      .rejects.toEqual(new ErrorSincronizacion('permiso'))
    expect(registrar).not.toHaveBeenCalled()
    expect(bloquearAcceso).toHaveBeenCalledWith({ usuarioId: usuario, ganaderiaId: ganaderia })
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({ estado: 'RECHAZADO', ultimoError: 'AUTORIZACION' })
  })

  it('requiere sesión vigente y conserva la operación sin enviarla', async () => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const registrar = vi.fn()
    const requerirSesion = vi.fn().mockResolvedValue(undefined)
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockRejectedValue(new ErrorGanaderia('sesion')),
      requerirSesion,
      crearLeaseId: () => lease1,
    })

    await expect(servicio.sincronizar(sesion, ganaderia))
      .rejects.toMatchObject({ tipo: 'sesion' })
    expect(registrar).not.toHaveBeenCalled()
    expect(requerirSesion).toHaveBeenCalledOnce()
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({ estado: 'REQUIERE_SESION', ultimoError: 'AUTENTICACION' })
  })

  it('ante respuesta perdida conserva el mismo payload y el reintento REPROCESADO confirma una sola identidad', async () => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const registrar = vi.fn()
      .mockRejectedValueOnce(new ErrorAnimal('conexion'))
      .mockResolvedValueOnce(confirmacion('REPROCESADO'))
    const dependencias = {
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
    }

    const primero = new ServicioSincronizacionAnimales({
      ...dependencias, crearLeaseId: () => lease1,
    })
    expect(await primero.sincronizar(sesion, ganaderia)).toMatchObject({
      sincronizadas: 0, fallidas: 1,
    })
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({ estado: 'ERROR_REINTENTABLE', ultimoError: 'RED', payload })

    const segundo = new ServicioSincronizacionAnimales({
      ...dependencias, crearLeaseId: () => lease2,
    })
    expect(await segundo.sincronizar(sesion, ganaderia)).toMatchObject({
      sincronizadas: 1, confirmadasRemotamente: 1,
    })
    expect(registrar).toHaveBeenCalledTimes(2)
    expect(registrar.mock.calls[0][1]).toEqual(payload)
    expect(registrar.mock.calls[1][1]).toEqual(payload)
  })

  it.each([
    ['validacion', 'RECHAZADO', 'VALIDACION'],
    ['conflicto', 'CONFLICTO', 'CONFLICTO'],
    ['servicio', 'ERROR_REINTENTABLE', 'SERVIDOR'],
  ] as const)('clasifica de forma segura el fallo %s', async (tipo, estado, codigo) => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar: vi.fn().mockRejectedValue(new ErrorAnimal(tipo)),
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      crearLeaseId: () => lease1,
    })

    await servicio.sincronizar(sesion, ganaderia)

    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({ estado, ultimoError: codigo, payload })
  })

  it.each([
    ['sesion', 'REQUIERE_SESION', 'AUTENTICACION'],
    ['permiso', 'RECHAZADO', 'AUTORIZACION'],
  ] as const)('detiene el lote tras un error HTTP de %s', async (tipo, estado, codigo) => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    await almacen.guardarPendiente(
      { usuarioId: usuario, ganaderiaId: ganaderia },
      { ...payload, operationId: otraOperacion, animalId: otroAnimal },
    )
    const registrar = vi.fn().mockRejectedValue(new ErrorAnimal(tipo))
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      crearLeaseId: () => lease1,
      requerirSesion: vi.fn().mockResolvedValue(undefined),
      bloquearAcceso: vi.fn().mockResolvedValue(undefined),
    })

    await servicio.sincronizar(sesion, ganaderia)

    expect(registrar).toHaveBeenCalledOnce()
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, operacion))
      .resolves.toMatchObject({ estado, ultimoError: codigo })
    await expect(almacen.obtener({ usuarioId: usuario, ganaderiaId: ganaderia }, otraOperacion))
      .resolves.toMatchObject({ estado: 'PENDIENTE', intentos: 0 })
  })

  it('evita el doble envío entre dos pestañas mediante bloqueo y lease compartidos', async () => {
    const almacen = crearAlmacen()
    await preparar(almacen)
    const colas = new Map<string, Promise<unknown>>()
    const conBloqueo = async <T,>(nombre: string, accion: () => Promise<T>) => {
      const anterior = colas.get(nombre) ?? Promise.resolve()
      const actual = anterior.then(accion, accion)
      colas.set(nombre, actual)
      return actual
    }
    let resolver: ((valor: ResultadoRegistroAnimal) => void) | undefined
    const registrar = vi.fn().mockReturnValue(new Promise<ResultadoRegistroAnimal>(resolve => {
      resolver = resolve
    }))
    const base = {
      almacen,
      registrar,
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      conBloqueo,
    }
    const primera = new ServicioSincronizacionAnimales({ ...base, crearLeaseId: () => lease1 })
    const segunda = new ServicioSincronizacionAnimales({ ...base, crearLeaseId: () => lease2 })

    const intento1 = primera.sincronizar(sesion, ganaderia)
    const intento2 = segunda.sincronizar(sesion, ganaderia)
    await vi.waitFor(() => expect(registrar).toHaveBeenCalledOnce())
    resolver?.(confirmacion())

    await expect(intento1).resolves.toMatchObject({ sincronizadas: 1 })
    await expect(intento2).resolves.toMatchObject({ sincronizadas: 0, ocupadas: 1 })
    expect(registrar).toHaveBeenCalledOnce()
  })

  it('no declara éxito local si la respuesta remota llegó pero falló guardar su recibo', async () => {
    const almacenReal = crearAlmacen()
    await preparar(almacenReal)
    const finalizarSincronizacion = vi.fn().mockRejectedValue(new Error('cuota'))
    const almacen = {
      listarPendientes: almacenReal.listarPendientes.bind(almacenReal),
      actualizarEstado: almacenReal.actualizarEstado.bind(almacenReal),
      reclamarParaSincronizar: almacenReal.reclamarParaSincronizar.bind(almacenReal),
      finalizarSincronizacion,
      fallarSincronizacion: almacenReal.fallarSincronizacion.bind(almacenReal),
    }
    const servicio = new ServicioSincronizacionAnimales({
      almacen,
      registrar: vi.fn().mockResolvedValue(confirmacion()),
      consultarAcceso: vi.fn().mockResolvedValue(acceso()),
      crearLeaseId: () => lease1,
    })

    const resultado = await servicio.sincronizar(sesion, ganaderia)

    expect(resultado).toMatchObject({
      sincronizadas: 0, confirmadasRemotamente: 1, fallidas: 1,
    })
    expect(resultado.operaciones[0]).toMatchObject({
      estado: 'ERROR_LOCAL', confirmadaRemotamente: true,
    })
    await expect(almacenReal.obtener(
      { usuarioId: usuario, ganaderiaId: ganaderia }, operacion,
    )).resolves.toMatchObject({ estado: 'SINCRONIZANDO', leaseId: lease1 })
  })
})
