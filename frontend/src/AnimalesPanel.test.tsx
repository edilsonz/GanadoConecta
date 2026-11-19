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

function operacionLocal(payload: Parameters<typeof guardarRegistroPendienteAutorizado>[1]) {
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
  }
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
})
