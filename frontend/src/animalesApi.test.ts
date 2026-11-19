import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  consultarAnimal,
  ErrorAnimal,
  listarAnimales,
  registrarAnimal,
  type SolicitudRegistroAnimal,
} from './animalesApi'

function respuesta(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: vi.fn().mockResolvedValue(body),
  } as unknown as Response
}

const ganaderiaId = '123e4567-e89b-42d3-a456-426614174000'
const animalId = '223e4567-e89b-42d3-a456-426614174000'
const operationId = '323e4567-e89b-42d3-a456-426614174000'
const animal = {
  animalId,
  ganaderiaId,
  sexo: 'HEMBRA',
  nombre: 'Luna',
  areteFrente: 'A-15',
  areteReverso: null,
  coloresIdentificacionTextil: 'rojo y blanco',
  ubicacionIdentificacionTextil: 'AMBAS',
  raza: 'Brown Swiss',
  fechaNacimiento: '2025-01-10',
  nacimientoEstimado: false,
  categoria: 'VAQUILLA',
  observaciones: null,
  estado: 'ACTIVO',
  version: 1,
  creadoEn: '2026-11-18T18:00:00Z',
} as const
const solicitud: SolicitudRegistroAnimal = {
  operationId,
  animalId,
  tipo: 'CREAR_ANIMAL',
  schemaVersion: 2,
  baseVersion: 0,
  sexo: 'HEMBRA',
  nombre: 'Luna',
}

describe('API de animales', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('lista por cursor con JWT en memoria y valida el ámbito', async () => {
    const pagina = { items: [animal], siguienteCursor: 'cursor-opaco', consultadoEn: '2026-11-18T18:01:00Z' }
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, pagina))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listarAnimales(ganaderiaId, 'jwt-memoria', 'cursor anterior/1')).resolves.toEqual(pagina)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('cursor=cursor+anterior%2F1'),
      expect.objectContaining({
        method: 'GET',
        cache: 'no-store',
        headers: expect.objectContaining({ Authorization: 'Bearer jwt-memoria' }),
      }),
    )
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('rechaza una respuesta que contiene animales de otra ganadería', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(200, {
      items: [{ ...animal, ganaderiaId: '423e4567-e89b-42d3-a456-426614174000' }],
      siguienteCursor: null,
      consultadoEn: '2026-11-18T18:01:00Z',
    })))

    await expect(listarAnimales(ganaderiaId, 'jwt')).rejects.toBeInstanceOf(ErrorAnimal)
  })

  it('consulta una ficha individual sin persistir el JWT', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, animal))
    vi.stubGlobal('fetch', fetchMock)

    await expect(consultarAnimal(ganaderiaId, animalId, 'jwt-memoria')).resolves.toEqual(animal)
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/v1/ganaderias/${ganaderiaId}/animales/${animalId}`,
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer jwt-memoria' }) }),
    )
    expect(localStorage.length).toBe(0)
  })

  it('registra con CSRF y conserva el mismo sobre al renovar el token', async () => {
    const recibo = {
      operationId,
      animalId,
      resultado: 'APLICADO',
      versionCreada: 1,
      confirmadoEn: '2026-11-18T18:02:00Z',
      advertencias: [],
    }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-anterior' }))
      .mockResolvedValueOnce(respuesta(403, { codigo: 'CSRF_INVALIDO' }))
      .mockResolvedValueOnce(respuesta(200, { token: 'csrf-nuevo' }))
      .mockResolvedValueOnce(respuesta(201, recibo))
    vi.stubGlobal('fetch', fetchMock)

    await expect(registrarAnimal(ganaderiaId, solicitud, 'jwt-memoria')).resolves.toEqual(recibo)
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ body: JSON.stringify(solicitud) }))
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({
      body: JSON.stringify(solicitud),
      headers: expect.objectContaining({
        Authorization: 'Bearer jwt-memoria',
        'X-CSRF-TOKEN': 'csrf-nuevo',
      }),
    }))
  })

  it('clasifica validación, sesión y permisos con errores seguros', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(respuesta(422, { erroresPorCampo: { sexo: 'inválido' } }))
      .mockResolvedValueOnce(respuesta(401, { codigo: 'AUTENTICACION_REQUERIDA' }))
      .mockResolvedValueOnce(respuesta(403, { codigo: 'MEMBRESIA_REVOCADA' })))

    await expect(listarAnimales(ganaderiaId, 'jwt')).rejects.toEqual(expect.objectContaining({ tipo: 'validacion' }))
    await expect(consultarAnimal(ganaderiaId, animalId, 'jwt')).rejects.toEqual(expect.objectContaining({ tipo: 'sesion' }))
    await expect(consultarAnimal(ganaderiaId, animalId, 'jwt')).rejects.toEqual(expect.objectContaining({ tipo: 'permiso' }))
  })

  it('clasifica como conexión una caída real de fetch durante el registro', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(registrarAnimal(ganaderiaId, solicitud, 'jwt'))
      .rejects.toEqual(expect.objectContaining({ tipo: 'conexion' }))
  })
})
