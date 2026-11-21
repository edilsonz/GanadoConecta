import { describe, expect, it, vi } from 'vitest'
import { crearRegistroPwa } from './pwaRegistroBase'

describe('registro del service worker', () => {
  it('solo registra en producción cuando el navegador lo soporta', async () => {
    const actualizar = vi.fn().mockResolvedValue(undefined)
    const registrar = vi.fn().mockReturnValue(actualizar)
    const eventos = {
      onListaOffline: vi.fn(),
      onNuevaVersion: vi.fn(),
      onError: vi.fn(),
    }

    const resultado = crearRegistroPwa(true, true, eventos, registrar)

    expect(resultado).toBe(actualizar)
    expect(registrar).toHaveBeenCalledWith(expect.objectContaining({
      immediate: true,
      onOfflineReady: eventos.onListaOffline,
      onNeedRefresh: eventos.onNuevaVersion,
      onRegisterError: eventos.onError,
    }))
  })

  it.each([
    ['desarrollo', false, true],
    ['navegador incompatible', true, false],
  ] as const)('no registra en %s', async (_caso, produccion, compatible) => {
    const registrar = vi.fn()
    const actualizar = crearRegistroPwa(produccion, compatible, {
      onListaOffline: vi.fn(), onNuevaVersion: vi.fn(), onError: vi.fn(),
    }, registrar)

    await expect(actualizar(true)).resolves.toBeUndefined()
    expect(registrar).not.toHaveBeenCalled()
  })
})
