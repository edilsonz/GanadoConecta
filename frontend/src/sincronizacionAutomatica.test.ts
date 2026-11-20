import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ErrorSincronizacion, type ResultadoSincronizacionManual } from './sincronizacionAnimales'
import {
  ControladorSincronizacionAutomatica,
  type ContextoSincronizacionAutomatica,
  type EstadoSincronizacionAutomatica,
} from './sincronizacionAutomatica'

const contextoBase: ContextoSincronizacionAutomatica = {
  sesion: {
    usuarioId: '10000000-0000-4000-8000-000000000001',
    accessToken: 'jwt-en-memoria',
    expiresAt: '2099-01-01T00:00:00Z',
  },
  ganaderiaId: '20000000-0000-4000-8000-000000000002',
  conectado: true,
  hayPendientes: true,
  puedeEscribir: true,
  sincronizacionManualEnCurso: false,
}

function resultado(
  estado: ResultadoSincronizacionManual['operaciones'][number]['estado'] = 'SINCRONIZADA',
  retryAfterMs?: number,
): ResultadoSincronizacionManual {
  return {
    total: 1,
    sincronizadas: estado === 'SINCRONIZADA' ? 1 : 0,
    confirmadasRemotamente: estado === 'SINCRONIZADA' ? 1 : 0,
    fallidas: estado === 'SINCRONIZADA' || estado === 'OCUPADA' ? 0 : 1,
    ocupadas: estado === 'OCUPADA' ? 1 : 0,
    operaciones: [{
      operationId: '30000000-0000-4000-8000-000000000003',
      animalId: '40000000-0000-4000-8000-000000000004',
      estado,
      confirmadaRemotamente: estado === 'SINCRONIZADA',
      ...(retryAfterMs !== undefined && { reintentoDespuesMs: retryAfterMs }),
    }],
  }
}

async function vaciarPromesas() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('sincronización automática controlada en primer plano', () => {
  beforeEach(() => vi.useFakeTimers())

  it('permanece desactivada por defecto y requiere activación explícita', async () => {
    const sincronizar = vi.fn().mockResolvedValue(resultado())
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      alCambiarEstado: estado => estados.push(estado),
    })

    controlador.actualizarContexto(contextoBase)
    await vaciarPromesas()
    expect(sincronizar).not.toHaveBeenCalled()

    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(sincronizar).toHaveBeenCalledOnce()
    expect(estados.at(-1)).toMatchObject({ tipo: 'SINCRONIZADA', sincronizadas: 1 })
  })

  it('espera una reconexión y no confía en el evento para omitir la verificación del motor', async () => {
    const sincronizar = vi.fn()
      .mockRejectedValueOnce(new ErrorSincronizacion('conexion'))
      .mockResolvedValueOnce(resultado())
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      aleatorio: () => 0,
      alCambiarEstado: estado => estados.push(estado),
    })
    controlador.activar({ ...contextoBase, conectado: false })
    expect(estados.at(-1)?.tipo).toBe('ESPERANDO_RECONEXION')

    controlador.actualizarContexto(contextoBase)
    await vaciarPromesas()
    expect(sincronizar).toHaveBeenCalledOnce()
    expect(estados.at(-1)).toMatchObject({
      tipo: 'REINTENTO_PROGRAMADO', reintentoEnMs: 2_000,
    })
    await vi.advanceTimersByTimeAsync(2_000)
    expect(sincronizar).toHaveBeenCalledTimes(2)
    expect(estados.at(-1)?.tipo).toBe('SINCRONIZADA')
  })

  it('respeta Retry-After y limita el ciclo a cinco intentos sin borrar pendientes', async () => {
    const sincronizar = vi.fn().mockResolvedValue(resultado('ERROR_REINTENTABLE', 8_000))
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      aleatorio: () => 0,
      alCambiarEstado: estado => estados.push(estado),
    })
    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(estados.at(-1)?.reintentoEnMs).toBe(8_000)

    for (const demora of [8_000, 8_000, 15_000, 30_000]) {
      await vi.advanceTimersByTimeAsync(demora)
      await vaciarPromesas()
    }
    expect(sincronizar).toHaveBeenCalledTimes(5)
    expect(estados.at(-1)?.tipo).toBe('ACCION_REQUERIDA')
    expect(vi.getTimerCount()).toBe(0)
  })

  it.each([
    ['RECHAZADA'], ['CONFLICTO'], ['ERROR_LOCAL'],
  ] as const)('no reintenta un resultado permanente %s', async estado => {
    const sincronizar = vi.fn().mockResolvedValue(resultado(estado))
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      alCambiarEstado: valor => estados.push(valor),
    })
    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(estados.at(-1)?.tipo).toBe('ACCION_REQUERIDA')
    expect(vi.getTimerCount()).toBe(0)
  })

  it.each([
    ['sesion', 'SESION_EXPIRADA'],
    ['permiso', 'PERMISO_INSUFICIENTE'],
  ] as const)('detiene reintentos ante %s', async (tipo, estado) => {
    const sincronizar = vi.fn().mockRejectedValue(new ErrorSincronizacion(tipo))
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      alCambiarEstado: valor => estados.push(valor),
    })
    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(estados.at(-1)?.tipo).toBe(estado)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('bloquea LECTOR y espera que termine una sincronización manual', async () => {
    const sincronizar = vi.fn().mockResolvedValue(resultado())
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      alCambiarEstado: valor => estados.push(valor),
    })
    controlador.activar({ ...contextoBase, puedeEscribir: false })
    expect(estados.at(-1)?.tipo).toBe('PERMISO_INSUFICIENTE')
    expect(sincronizar).not.toHaveBeenCalled()

    controlador.desactivar()
    controlador.activar({ ...contextoBase, sincronizacionManualEnCurso: true })
    expect(sincronizar).not.toHaveBeenCalled()
    controlador.actualizarContexto(contextoBase)
    await vaciarPromesas()
    expect(sincronizar).toHaveBeenCalledOnce()
  })

  it('desactivar cancela la espera y no interrumpe inseguramente una solicitud enviada', async () => {
    const sincronizar = vi.fn()
      .mockResolvedValueOnce(resultado('ERROR_REINTENTABLE'))
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      aleatorio: () => 0,
    })
    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(vi.getTimerCount()).toBe(1)
    controlador.desactivar()
    await vi.runAllTimersAsync()
    expect(sincronizar).toHaveBeenCalledOnce()

    let resolver: ((valor: ResultadoSincronizacionManual) => void) | undefined
    sincronizar.mockReturnValueOnce(new Promise(resolve => { resolver = resolve }))
    controlador.activar(contextoBase)
    await vaciarPromesas()
    controlador.desactivar()
    resolver?.(resultado('ERROR_REINTENTABLE'))
    await vaciarPromesas()
    expect(sincronizar).toHaveBeenCalledTimes(2)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('pausa un reintento programado si se pierde la conexión y lo retoma al reconectar', async () => {
    const sincronizar = vi.fn()
      .mockResolvedValueOnce(resultado('ERROR_REINTENTABLE'))
      .mockResolvedValueOnce(resultado())
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      aleatorio: () => 0,
      alCambiarEstado: valor => estados.push(valor),
    })
    controlador.activar(contextoBase)
    await vaciarPromesas()
    expect(vi.getTimerCount()).toBe(1)

    controlador.actualizarContexto({ ...contextoBase, conectado: false })
    expect(vi.getTimerCount()).toBe(0)
    expect(estados.at(-1)?.tipo).toBe('ESPERANDO_RECONEXION')
    controlador.actualizarContexto(contextoBase)
    await vaciarPromesas()
    expect(sincronizar).toHaveBeenCalledTimes(2)
    expect(estados.at(-1)?.tipo).toBe('SINCRONIZADA')
  })

  it('cambiar de cuenta o ganadería desactiva el contexto anterior', async () => {
    const sincronizar = vi.fn().mockResolvedValue(resultado())
    const estados: EstadoSincronizacionAutomatica[] = []
    const controlador = new ControladorSincronizacionAutomatica({
      sincronizar,
      alCambiarEstado: valor => estados.push(valor),
    })
    controlador.activar({ ...contextoBase, hayPendientes: false })
    controlador.actualizarContexto({
      ...contextoBase,
      ganaderiaId: '50000000-0000-4000-8000-000000000005',
    })
    expect(controlador.estaActivada()).toBe(false)
    expect(estados.at(-1)?.tipo).toBe('DESACTIVADA')
    expect(sincronizar).not.toHaveBeenCalled()
  })
})
