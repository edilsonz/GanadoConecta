import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AnimalesPanel } from './AnimalesPanel'
import {
  consultarAnimal,
  ErrorAnimal,
  listarAnimales,
  registrarAnimal,
  type Animal,
} from './animalesApi'
import { consultarAccesoGanaderia, ErrorGanaderia } from './ganaderiasApi'
import {
  bloquearAccesoOffline,
  guardarRegistroPendienteAutorizado,
  listarPendientesAutorizados,
  prepararAccesoOfflineConfirmado,
  requerirRevalidacionOffline,
} from './registroAnimalesOffline'
import {
  ErrorSincronizacion,
  sincronizarPendientesManualmente,
} from './sincronizacionAnimales'
import { useEstadoActividadPwa } from './pwaActividad'
import type { OperacionPendienteLocal } from './operacionesPendientesDb'

vi.mock('./animalesApi', async importOriginal => {
  const original = await importOriginal<typeof import('./animalesApi')>()
  return {
    ...original,
    consultarAnimal: vi.fn(),
    listarAnimales: vi.fn(),
    registrarAnimal: vi.fn(),
  }
})

vi.mock('./ganaderiasApi', async importOriginal => {
  const original = await importOriginal<typeof import('./ganaderiasApi')>()
  return { ...original, consultarAccesoGanaderia: vi.fn() }
})

vi.mock('./registroAnimalesOffline', () => ({
  bloquearAccesoOffline: vi.fn(),
  guardarRegistroPendienteAutorizado: vi.fn(),
  listarPendientesAutorizados: vi.fn(),
  prepararAccesoOfflineConfirmado: vi.fn(),
  requerirRevalidacionOffline: vi.fn(),
}))

vi.mock('./sincronizacionAnimales', () => ({
  ErrorSincronizacion: class ErrorSincronizacion extends Error {
    constructor(public readonly tipo: string) {
      super(tipo)
      this.name = 'ErrorSincronizacion'
    }
  },
  sincronizarPendientesManualmente: vi.fn(),
}))

const ganaderia = {
  id: '123e4567-e89b-42d3-a456-426614174000',
  nombre: 'Los Pinos',
  estado: 'ACTIVA' as const,
  rol: 'PROPIETARIO' as const,
}
const sesion = {
  usuarioId: 'usuario-1',
  correo: 'productora@ejemplo.com',
  estado: 'ACTIVO' as const,
  accessToken: 'jwt-solo-memoria',
  expiresAt: '2099-11-18T22:00:00Z',
}
const accesoPropietario = {
  ganaderiaId: ganaderia.id,
  rol: 'PROPIETARIO' as const,
  estado: 'ACTIVA' as const,
  validadoEn: '2026-11-18T18:00:00Z',
}
const animalUuid = '223e4567-e89b-42d3-a456-426614174000' as `${string}-${string}-${string}-${string}-${string}`
const operacionUuid = '323e4567-e89b-42d3-a456-426614174000' as `${string}-${string}-${string}-${string}-${string}`
const animal = {
  animalId: animalUuid,
  ganaderiaId: ganaderia.id,
  sexo: 'HEMBRA' as const,
  nombre: 'Luna',
  areteFrente: 'A-15',
  areteReverso: null,
  coloresIdentificacionTextil: 'rojo y blanco',
  ubicacionIdentificacionTextil: 'AMBAS' as const,
  raza: 'Brown Swiss',
  fechaNacimiento: '2025-01-10',
  nacimientoEstimado: false,
  categoria: 'VAQUILLA' as const,
  observaciones: 'Registro sintético',
  estado: 'ACTIVO' as const,
  version: 1,
  creadoEn: '2026-11-18T18:01:00Z',
}

function pagina(items: Animal[] = [animal], siguienteCursor: string | null = null) {
  return { items, siguienteCursor, consultadoEn: '2026-11-18T18:02:00Z' }
}

function operacionLocal(
  payload: Parameters<typeof guardarRegistroPendienteAutorizado>[1],
  cambios: Partial<Pick<OperacionPendienteLocal, 'estado' | 'ultimoError' | 'intentos'>> = {},
) {
  return {
    usuarioId: sesion.usuarioId,
    ganaderiaId: ganaderia.id,
    operationId: payload.operationId,
    animalId: payload.animalId,
    payload,
    estado: 'PENDIENTE' as const,
    ultimoError: null,
    creadoEnLocal: '2026-11-19T15:00:00Z',
    actualizadoEnLocal: '2026-11-19T15:00:00Z',
    intentos: 0,
    leaseId: null,
    leaseHasta: null,
    confirmacion: null,
    ...cambios,
  }
}

function EstadoActividadPwa() {
  const estado = useEstadoActividadPwa()
  return <output aria-label="Actividad PWA">
    {estado.formularioEnEdicion ? 'formulario-activo' : 'sin-formulario'}
  </output>
}

describe('Panel de animales', () => {
  beforeEach(() => {
    vi.mocked(consultarAccesoGanaderia).mockReset()
    vi.mocked(listarAnimales).mockReset()
    vi.mocked(consultarAnimal).mockReset()
    vi.mocked(registrarAnimal).mockReset()
    vi.mocked(bloquearAccesoOffline).mockReset().mockResolvedValue()
    vi.mocked(requerirRevalidacionOffline).mockReset().mockResolvedValue()
    vi.mocked(prepararAccesoOfflineConfirmado).mockReset().mockResolvedValue({
      estado: 'AUTORIZADA_ESCRITURA', contexto: null,
    })
    vi.mocked(listarPendientesAutorizados).mockReset().mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null, operaciones: [],
    })
    vi.mocked(guardarRegistroPendienteAutorizado).mockReset()
    vi.mocked(sincronizarPendientesManualmente).mockReset()
    vi.mocked(consultarAccesoGanaderia).mockResolvedValue(accesoPropietario)
    vi.mocked(listarAnimales).mockResolvedValue(pagina())
  })

  it('muestra listado, consulta la ficha real y conserva los campos aprobados', async () => {
    vi.mocked(consultarAnimal).mockResolvedValue(animal)
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: /Luna/ }))
    expect(await screen.findByRole('heading', { name: 'Luna' })).toBeInTheDocument()
    expect(screen.getByText('Brown Swiss')).toBeInTheDocument()
    expect(screen.getByText('A-15')).toBeInTheDocument()
    expect(screen.getAllByText('rojo y blanco').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ambas orejas')).toHaveLength(2)
    expect(consultarAnimal).toHaveBeenCalledWith(ganaderia.id, animal.animalId, sesion.accessToken)
  })

  it('representa el listado vacío y exige sexo para registrar', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText(/Aún no hay animales/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))
    expect(await screen.findByText('Selecciona MACHO o HEMBRA.')).toBeInTheDocument()
    expect(registrarAnimal).not.toHaveBeenCalled()
  })

  it('protege una actualización mientras el formulario de animal tiene cambios', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    render(<>
      <AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />
      <EstadoActividadPwa />
    </>)

    const nombre = await screen.findByLabelText('Nombre')
    expect(screen.getByLabelText('Actividad PWA')).toHaveTextContent('sin-formulario')

    fireEvent.change(nombre, { target: { value: 'Borrador local' } })
    expect(await screen.findByText('formulario-activo')).toBeInTheDocument()

    fireEvent.change(nombre, { target: { value: '' } })
    expect(await screen.findByText('sin-formulario')).toBeInTheDocument()
  })

  it('prepara offline solo tras una decisión explícita en un dispositivo de confianza', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    const boton = await screen.findByRole('button', { name: 'Preparar trabajo sin conexión' })
    expect(prepararAccesoOfflineConfirmado).not.toHaveBeenCalled()

    fireEvent.click(boton)

    await waitFor(() => expect(prepararAccesoOfflineConfirmado)
      .toHaveBeenCalledWith(sesion.usuarioId, accesoPropietario))
    expect(await screen.findByText(/máximo de 168 horas/)).toBeInTheDocument()
    expect(screen.getByText('Trabajo sin conexión preparado')).toBeInTheDocument()
    expect(screen.getByText('El navegador informa: con conexión')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Preparar trabajo sin conexión' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    expect(screen.queryByText(/máximo de 168 horas/)).not.toBeInTheDocument()
    expect(screen.getByText('Trabajo sin conexión preparado')).toBeInTheDocument()
  })

  it('muestra el estado de red del navegador sin convertirlo por sí solo en guardado local', async () => {
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA',
      contexto: {
        usuarioId: sesion.usuarioId,
        ganaderiaId: ganaderia.id,
        rol: 'PROPIETARIO',
        estado: 'VIGENTE',
        motivo: null,
        validadoEnServidor: accesoPropietario.validadoEn,
        autorizadoHastaServidor: '2026-11-26T18:00:00Z',
        observadoEnCliente: accesoPropietario.validadoEn,
        autorizadoHastaCliente: '2026-11-26T18:00:00Z',
        ultimoComprobadoEnCliente: accesoPropietario.validadoEn,
      },
      operaciones: [],
    })
    vi.mocked(registrarAnimal).mockResolvedValue({
      operationId: operacionUuid,
      animalId: animalUuid,
      resultado: 'APLICADO',
      versionCreada: 1,
      confirmadoEn: '2026-11-19T15:00:00Z',
      advertencias: [],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    expect(await screen.findByText('Trabajo sin conexión preparado')).toBeInTheDocument()

    window.dispatchEvent(new Event('offline'))
    expect(await screen.findByText('El navegador informa: sin conexión')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(/Animal registrado correctamente/)).toBeInTheDocument()
    expect(guardarRegistroPendienteAutorizado).not.toHaveBeenCalled()
    window.dispatchEvent(new Event('online'))
  })

  it('aplica los límites por caracteres Unicode y no por unidades UTF-16', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    const nombre = screen.getByLabelText('Nombre')
    expect(nombre).not.toHaveAttribute('maxlength')
    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.change(nombre, { target: { value: '🐄'.repeat(101) } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText('Usa como máximo 100 caracteres.')).toBeInTheDocument()
    expect(registrarAnimal).not.toHaveBeenCalled()
  })

  it('registra con UUID técnicos, actualiza la lista y no envía usuarioId', async () => {
    vi.mocked(listarAnimales)
      .mockResolvedValueOnce(pagina([]))
      .mockResolvedValueOnce(pagina([animal]))
    vi.mocked(registrarAnimal).mockResolvedValue({
      operationId: '323e4567-e89b-42d3-a456-426614174000',
      animalId: animal.animalId,
      resultado: 'APLICADO',
      versionCreada: 1,
      confirmadoEn: '2026-11-18T18:02:00Z',
      advertencias: [],
    })
    vi.spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce(animal.animalId)
      .mockReturnValueOnce(operacionUuid)
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: ' Luna ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(/Animal registrado correctamente/)).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Luna/ })).toBeInTheDocument()
    const solicitud = vi.mocked(registrarAnimal).mock.calls[0][1]
    expect(solicitud).toEqual(expect.objectContaining({
      animalId: animal.animalId,
      operationId: operacionUuid,
      sexo: 'HEMBRA',
      nombre: 'Luna',
      schemaVersion: 2,
    }))
    expect(solicitud).not.toHaveProperty('usuarioId')
    expect(guardarRegistroPendienteAutorizado).not.toHaveBeenCalled()
  })

  it('registra solo identificación de lana o cinta con sus dos datos', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockResolvedValue({
      operationId: operacionUuid,
      animalId: animalUuid,
      resultado: 'APLICADO',
      versionCreada: 1,
      confirmadoEn: '2026-11-19T15:00:00Z',
      advertencias: [],
    })
    vi.spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce(animalUuid)
      .mockReturnValueOnce(operacionUuid)
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.change(screen.getByLabelText('Colores de la lana o cinta'), {
      target: { value: ' rojo y blanco ' },
    })
    fireEvent.change(screen.getByLabelText('Ubicación'), { target: { value: 'AMBAS' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    await waitFor(() => expect(registrarAnimal).toHaveBeenCalledOnce())
    expect(vi.mocked(registrarAnimal).mock.calls[0][1]).toEqual(expect.objectContaining({
      schemaVersion: 2,
      coloresIdentificacionTextil: 'rojo y blanco',
      ubicacionIdentificacionTextil: 'AMBAS',
    }))
    expect(vi.mocked(registrarAnimal).mock.calls[0][1]).not.toHaveProperty('areteFrente')
  })

  it('rechaza identificación textil incompleta antes de enviar', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'MACHO' } })
    fireEvent.change(screen.getByLabelText('Colores de la lana o cinta'), {
      target: { value: 'azul' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(/Selecciona la oreja/)).toBeInTheDocument()
    expect(registrarAnimal).not.toHaveBeenCalled()
  })

  it('guarda una caída real de red con los mismos identificadores y la muestra separada', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockRejectedValueOnce(new ErrorAnimal('conexion'))
    vi.mocked(guardarRegistroPendienteAutorizado).mockImplementation(async (_ambito, payload) => ({
      autorizacion: 'AUTORIZADA_ESCRITURA', operacion: operacionLocal(payload),
    }))
    vi.spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce(animalUuid)
      .mockReturnValueOnce(operacionUuid)
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'MACHO' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText('Guardado en este dispositivo. Pendiente de sincronización.'))
      .toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Pendientes en este dispositivo' })).toBeInTheDocument()
    expect(screen.getByText('Pendiente de sincronización')).toBeInTheDocument()
    expect(guardarRegistroPendienteAutorizado).toHaveBeenCalledWith(
      { usuarioId: sesion.usuarioId, ganaderiaId: ganaderia.id },
      vi.mocked(registrarAnimal).mock.calls[0][1],
    )
    expect(screen.getByLabelText('Sexo *')).toHaveValue('')
  })

  it.each([
    ['servicio', 'servicio'],
    ['limite', 'límite'],
    ['validacion', 'campos'],
    ['seguridad', 'seguridad'],
  ] as const)('no convierte el error HTTP %s en un registro offline', async (tipo, texto) => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockRejectedValueOnce(new ErrorAnimal(tipo))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(new RegExp(texto, 'i'))).toBeInTheDocument()
    expect(guardarRegistroPendienteAutorizado).not.toHaveBeenCalled()
  })

  it('conserva el formulario si IndexedDB falla', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockRejectedValueOnce(new ErrorAnimal('conexion'))
    vi.mocked(guardarRegistroPendienteAutorizado).mockRejectedValueOnce(new Error('cuota'))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Luna local' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(/No se pudo guardar en este dispositivo/)).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre')).toHaveValue('Luna local')
    expect(screen.getByRole('button', { name: 'Reintentar mismo registro' })).toBeInTheDocument()
  })

  it.each([
    ['AUSENTE', /no tiene una autorización offline vigente/i],
    ['VENCIDA', /autorización offline venció/i],
    ['SOLO_LECTURA', /permiso verificado es de lectura/i],
  ] as const)('no guarda localmente cuando la autorización queda %s', async (estado, mensaje) => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockRejectedValueOnce(new ErrorAnimal('conexion'))
    vi.mocked(guardarRegistroPendienteAutorizado).mockResolvedValueOnce({
      autorizacion: estado, operacion: null,
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    expect(await screen.findByText(mensaje)).toBeInTheDocument()
    expect(screen.queryByText('Pendiente de sincronización')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Sexo *')).toHaveValue('HEMBRA')
  })

  it('recupera pendientes del mismo usuario y ganadería con contexto vigente', async () => {
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
      areteFrente: 'LOCAL-7',
    }
    vi.mocked(consultarAccesoGanaderia).mockRejectedValueOnce(new ErrorGanaderia('conexion'))
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA',
      contexto: {
        usuarioId: sesion.usuarioId,
        ganaderiaId: ganaderia.id,
        rol: 'PROPIETARIO',
        estado: 'VIGENTE',
        motivo: null,
        validadoEnServidor: accesoPropietario.validadoEn,
        autorizadoHastaServidor: '2026-11-26T18:00:00Z',
        observadoEnCliente: accesoPropietario.validadoEn,
        autorizadoHastaCliente: '2026-11-26T18:00:00Z',
        ultimoComprobadoEnCliente: accesoPropietario.validadoEn,
      },
      operaciones: [operacionLocal(payload)],
    })

    const vista = render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    expect(await screen.findByText('LOCAL-7')).toBeInTheDocument()
    expect(screen.getByText(/Trabajas con la autorización local vigente/)).toBeInTheDocument()
    expect(listarAnimales).not.toHaveBeenCalled()

    vista.unmount()
    vi.mocked(consultarAccesoGanaderia).mockRejectedValueOnce(new ErrorGanaderia('conexion'))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    expect(await screen.findByText('LOCAL-7')).toBeInTheDocument()
  })

  it('evita doble envío y un único fallo de red produce una sola operación local', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    let rechazar: ((error: unknown) => void) | undefined
    vi.mocked(registrarAnimal).mockReturnValueOnce(new Promise((_, reject) => { rechazar = reject }))
    vi.mocked(guardarRegistroPendienteAutorizado).mockImplementation(async (_ambito, payload) => ({
      autorizacion: 'AUTORIZADA_ESCRITURA', operacion: operacionLocal(payload),
    }))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)
    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'MACHO' } })

    const boton = screen.getByRole('button', { name: 'Registrar animal' })
    fireEvent.click(boton)
    fireEvent.submit(boton.closest('form')!)
    expect(registrarAnimal).toHaveBeenCalledOnce()
    rechazar?.(new ErrorAnimal('conexion'))

    await waitFor(() => expect(guardarRegistroPendienteAutorizado).toHaveBeenCalledOnce())
    expect(await screen.findByText('Pendiente de sincronización')).toBeInTheDocument()
  })

  it('conserva en el pendiente un animal identificado solo por arete', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal).mockRejectedValueOnce(new ErrorAnimal('conexion'))
    vi.mocked(guardarRegistroPendienteAutorizado).mockImplementation(async (_ambito, payload) => ({
      autorizacion: 'AUTORIZADA_ESCRITURA', operacion: operacionLocal(payload),
    }))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)
    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'HEMBRA' } })
    fireEvent.change(screen.getByLabelText('Arete frontal'), { target: { value: 'A-LOCAL' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))

    await waitFor(() => expect(guardarRegistroPendienteAutorizado).toHaveBeenCalledOnce())
    const payload = vi.mocked(guardarRegistroPendienteAutorizado).mock.calls[0][1]
    expect(payload).toEqual(expect.objectContaining({ sexo: 'HEMBRA', areteFrente: 'A-LOCAL' }))
    expect(payload).not.toHaveProperty('coloresIdentificacionTextil')
  })

  it('pagina por cursor y deduplica por animalId', async () => {
    const otro = { ...animal, animalId: '423e4567-e89b-42d3-a456-426614174000', nombre: 'Sol' }
    vi.mocked(listarAnimales)
      .mockResolvedValueOnce(pagina([animal], 'cursor-opaco'))
      .mockResolvedValueOnce(pagina([animal, otro]))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Cargar más' }))
    expect(await screen.findByRole('button', { name: /Sol/ })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /Luna/ })).toHaveLength(1)
    expect(listarAnimales).toHaveBeenLastCalledWith(ganaderia.id, sesion.accessToken, 'cursor-opaco')
  })

  it('permite consultar a LECTOR pero no muestra el formulario de alta', async () => {
    vi.mocked(consultarAccesoGanaderia).mockResolvedValue({ ...accesoPropietario, rol: 'LECTOR' })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText('Acceso de lectura')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Luna/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar animal' })).not.toBeInTheDocument()
    expect(screen.getByRole('checkbox', {
      name: 'Sincronización automática al recuperar conexión',
    })).toBeDisabled()
  })

  it('permite registrar a EDITOR tras revalidar su acceso', async () => {
    vi.mocked(consultarAccesoGanaderia).mockResolvedValue({ ...accesoPropietario, rol: 'EDITOR' })
    render(<AnimalesPanel ganaderia={{ ...ganaderia, rol: 'EDITOR' }}
      sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByRole('button', { name: 'Registrar animal' })).toBeInTheDocument()
    expect(screen.getByText('Acceso: editor')).toBeInTheDocument()
  })

  it('bloquea una membresía revocada e invalida una sesión vencida', async () => {
    vi.mocked(consultarAccesoGanaderia).mockRejectedValueOnce(new ErrorGanaderia('seguridad'))
    const invalidar = vi.fn()
    const vista = render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={invalidar} />)
    expect(await screen.findByText(/No hay acceso vigente/)).toBeInTheDocument()
    expect(listarAnimales).not.toHaveBeenCalled()

    vi.mocked(consultarAccesoGanaderia).mockResolvedValueOnce(accesoPropietario)
    vi.mocked(listarAnimales).mockRejectedValueOnce(new ErrorAnimal('sesion'))
    vista.rerender(<AnimalesPanel ganaderia={{ ...ganaderia, id: 'otra-ganaderia' }}
      sesion={sesion} onSesionInvalida={invalidar} />)
    await waitFor(() => expect(invalidar).toHaveBeenCalledOnce())
  })

  it('sincroniza pendientes solo por acción explícita y actualiza el listado confirmado', async () => {
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
      nombre: 'Offline local',
    }
    vi.mocked(listarPendientesAutorizados)
      .mockResolvedValueOnce({
        autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
        operaciones: [operacionLocal(payload)],
      })
      .mockResolvedValueOnce({
        autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null, operaciones: [],
      })
    vi.mocked(listarAnimales).mockReset()
      .mockResolvedValueOnce(pagina([]))
      .mockResolvedValueOnce(pagina([animal]))
    vi.mocked(sincronizarPendientesManualmente).mockResolvedValue({
      total: 1,
      sincronizadas: 1,
      confirmadasRemotamente: 1,
      fallidas: 0,
      ocupadas: 0,
      operaciones: [{
        operationId: operacionUuid,
        animalId: animalUuid,
        estado: 'SINCRONIZADA',
        confirmadaRemotamente: true,
        resultadoRemoto: 'APLICADO',
      }],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    const boton = await screen.findByRole('button', { name: 'Sincronizar pendientes' })
    expect(screen.getByText('Offline local')).toBeInTheDocument()
    expect(sincronizarPendientesManualmente).not.toHaveBeenCalled()

    fireEvent.click(boton)

    await waitFor(() => expect(sincronizarPendientesManualmente).toHaveBeenCalledOnce())
    expect(sincronizarPendientesManualmente).toHaveBeenCalledWith(
      sesion, ganaderia.id, expect.any(AbortSignal),
    )
    expect(await screen.findByText('1 registro pendiente sincronizado correctamente.'))
      .toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Luna/ })).toBeInTheDocument()
    expect(screen.queryByText('Offline local')).not.toBeInTheDocument()
  })

  it('mantiene la sincronización automática desactivada hasta la aceptación explícita', async () => {
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
      nombre: 'Pendiente automático',
    }
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload)],
    })
    vi.mocked(sincronizarPendientesManualmente).mockResolvedValue({
      total: 1, sincronizadas: 1, confirmadasRemotamente: 1, fallidas: 0, ocupadas: 0,
      operaciones: [{
        operationId: operacionUuid, animalId: animalUuid,
        estado: 'SINCRONIZADA', confirmadaRemotamente: true,
      }],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    const opcion = await screen.findByRole('checkbox', {
      name: 'Sincronización automática al recuperar conexión',
    })
    expect(opcion).not.toBeChecked()
    expect(screen.getByText('Desactivada.')).toBeInTheDocument()
    expect(sincronizarPendientesManualmente).not.toHaveBeenCalled()

    fireEvent.click(opcion)
    await waitFor(() => expect(sincronizarPendientesManualmente).toHaveBeenCalledOnce())
  })

  it('espera offline y usa el mismo motor al recibir una reconexión en primer plano', async () => {
    const descriptor = Object.getOwnPropertyDescriptor(navigator, 'onLine')
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false })
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'MACHO' as const,
    }
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload)],
    })
    vi.mocked(sincronizarPendientesManualmente).mockResolvedValue({
      total: 0, sincronizadas: 0, confirmadasRemotamente: 0, fallidas: 0, ocupadas: 1,
      operaciones: [{
        operationId: operacionUuid, animalId: animalUuid,
        estado: 'OCUPADA', confirmadaRemotamente: false,
      }],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    fireEvent.click(await screen.findByRole('checkbox', {
      name: 'Sincronización automática al recuperar conexión',
    }))
    expect(await screen.findByText('Sin conexión. Esperando reconexión.')).toBeInTheDocument()
    expect(sincronizarPendientesManualmente).not.toHaveBeenCalled()

    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    fireEvent(window, new Event('online'))
    await waitFor(() => expect(sincronizarPendientesManualmente).toHaveBeenCalledOnce())

    if (descriptor) Object.defineProperty(navigator, 'onLine', descriptor)
    else Reflect.deleteProperty(navigator, 'onLine')
  })

  it('conserva visible un pendiente ante un fallo de red y permite reintento manual', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'MACHO' as const,
      nombre: 'Pendiente seguro',
    }
    vi.mocked(listarPendientesAutorizados)
      .mockResolvedValueOnce({
        autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
        operaciones: [operacionLocal(payload)],
      })
      .mockResolvedValue({
        autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
        operaciones: [operacionLocal(payload, {
          estado: 'ERROR_REINTENTABLE', ultimoError: 'RED', intentos: 1,
        })],
      })
    vi.mocked(sincronizarPendientesManualmente)
      .mockRejectedValue(new ErrorSincronizacion('conexion'))
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Sincronizar pendientes' }))

    expect(await screen.findByText(/Las operaciones permanecen guardadas/)).toBeInTheDocument()
    expect(screen.getByText('Pendiente seguro')).toBeInTheDocument()
    expect(screen.getByText(/red no permitió confirmar/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sincronizar pendientes' })).toBeEnabled()
  })

  it('no oculta una operación local sin recibo aunque el animal ya aparezca confirmado', async () => {
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
      nombre: 'Luna pendiente de recibo',
    }
    vi.mocked(listarAnimales).mockResolvedValue(pagina([animal]))
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload, {
        estado: 'ERROR_REINTENTABLE', ultimoError: 'RED', intentos: 1,
      })],
    })

    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText('Luna pendiente de recibo')).toBeInTheDocument()
    expect(screen.getByText(/misma operación/)).toBeInTheDocument()
  })

  it.each([
    ['CONFLICTO', 'CONFLICTO', /conflicto 409/],
    ['RECHAZADO', 'VALIDACION', /servidor rechazó los datos/],
    ['RECHAZADO', 'PROTOCOLO', /seguridad o protocolo/],
  ] as const)('explica %s/%s y bloquea reintentos indebidos', async (estado, codigo, texto) => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
      nombre: 'Revisión requerida',
    }
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload, { estado, ultimoError: codigo, intentos: 1 })],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText(texto)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sin operaciones recuperables' })).toBeDisabled()
    fireEvent.click(screen.getByRole('checkbox', {
      name: 'Sincronización automática al recuperar conexión',
    }))
    expect(sincronizarPendientesManualmente).not.toHaveBeenCalled()
  })

  it.each([
    ['REQUIERE_SESION', 'AUTENTICACION', /sesión venció/],
    ['RECHAZADO', 'AUTORIZACION', /membresía fue revocada/],
  ] as const)('muestra la recuperación exigida para %s', async (estado, codigo, texto) => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'MACHO' as const,
      nombre: 'Acceso por recuperar',
    }
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload, { estado, ultimoError: codigo })],
    })
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)

    expect(await screen.findByText(texto)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sincronizar pendientes' })).toBeEnabled()
  })

  it.each([
    ['AUTENTICACION', 'REQUIERE_SESION', 'sesion'],
    ['AUTORIZACION', 'RECHAZADA', 'permiso'],
  ] as const)('aplica el bloqueo seguro cuando el envío devuelve %s', async (
    codigoError, estado, efecto,
  ) => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
    }
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload)],
    })
    vi.mocked(sincronizarPendientesManualmente).mockResolvedValue({
      total: 1, sincronizadas: 0, confirmadasRemotamente: 0, fallidas: 1, ocupadas: 0,
      operaciones: [{
        operationId: operacionUuid,
        animalId: animalUuid,
        estado,
        confirmadaRemotamente: false,
        codigoError,
      }],
    })
    const invalidar = vi.fn()
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={invalidar} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Sincronizar pendientes' }))

    if (efecto === 'sesion') {
      await waitFor(() => expect(invalidar).toHaveBeenCalledOnce())
      expect(requerirRevalidacionOffline).toHaveBeenCalled()
      expect(screen.getByText(/Tu sesión terminó/)).toBeInTheDocument()
    } else {
      expect(await screen.findByText(/No hay acceso vigente/)).toBeInTheDocument()
      expect(bloquearAccesoOffline).toHaveBeenCalled()
      expect(screen.getByText(/Tu acceso a esta ganadería cambió/)).toBeInTheDocument()
    }
  })

  it('invalida la vista al fallar la revalidación de sesión durante sincronización', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    const payload = {
      operationId: operacionUuid,
      animalId: animalUuid,
      tipo: 'CREAR_ANIMAL' as const,
      schemaVersion: 2 as const,
      baseVersion: 0 as const,
      sexo: 'HEMBRA' as const,
    }
    vi.mocked(listarPendientesAutorizados).mockResolvedValue({
      autorizacion: 'AUTORIZADA_ESCRITURA', contexto: null,
      operaciones: [operacionLocal(payload)],
    })
    vi.mocked(sincronizarPendientesManualmente)
      .mockRejectedValue(new ErrorSincronizacion('sesion'))
    const invalidar = vi.fn()
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={invalidar} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Sincronizar pendientes' }))

    await waitFor(() => expect(invalidar).toHaveBeenCalledOnce())
    expect(requerirRevalidacionOffline).toHaveBeenCalledWith({
      usuarioId: sesion.usuarioId, ganaderiaId: ganaderia.id,
    })
  })
})
