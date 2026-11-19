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

describe('Panel de animales', () => {
  beforeEach(() => {
    vi.mocked(consultarAccesoGanaderia).mockReset()
    vi.mocked(listarAnimales).mockReset()
    vi.mocked(consultarAnimal).mockReset()
    vi.mocked(registrarAnimal).mockReset()
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
    }))
    expect(solicitud).not.toHaveProperty('usuarioId')
  })

  it('reintenta una respuesta incierta con los mismos identificadores', async () => {
    vi.mocked(listarAnimales).mockResolvedValue(pagina([]))
    vi.mocked(registrarAnimal)
      .mockRejectedValueOnce(new ErrorAnimal('conexion'))
      .mockResolvedValueOnce({
        operationId: operacionUuid, animalId: animalUuid, resultado: 'REPROCESADO',
        versionCreada: 1, confirmadoEn: '2026-11-18T18:02:00Z', advertencias: [],
      })
    vi.spyOn(crypto, 'randomUUID')
      .mockReturnValueOnce(animalUuid)
      .mockReturnValueOnce(operacionUuid)
    render(<AnimalesPanel ganaderia={ganaderia} sesion={sesion} onSesionInvalida={vi.fn()} />)
    await screen.findByText(/Aún no hay animales/)

    fireEvent.change(screen.getByLabelText('Sexo *'), { target: { value: 'MACHO' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar animal' }))
    const reintentar = await screen.findByRole('button', { name: 'Reintentar mismo registro' })
    expect(screen.getByLabelText('Sexo *')).toBeDisabled()
    fireEvent.click(reintentar)

    await waitFor(() => expect(registrarAnimal).toHaveBeenCalledTimes(2))
    expect(vi.mocked(registrarAnimal).mock.calls[1][1])
      .toEqual(vi.mocked(registrarAnimal).mock.calls[0][1])
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
