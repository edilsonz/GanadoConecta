import { describe, expect, it, vi } from 'vitest'
import {
  ServicioAutorizacionOffline,
} from './autorizacionOffline'
import { AlmacenContextosOfflineIndexedDb } from './contextosOfflineDb'
import { ErrorGanaderia, type AccesoGanaderia } from './ganaderiasApi'
import { FabricaIndexedDbFalsa } from './test/indexedDbFalso'

const usuario = '11111111-1111-4111-8111-111111111111'
const ganaderia = '33333333-3333-4333-8333-333333333333'
const sesion = { usuarioId: usuario, accessToken: 'jwt-solo-en-memoria' }
type ConsultarAcceso = (
  ganaderiaId: string,
  accessToken: string,
) => Promise<AccesoGanaderia>

function preparar(
  consultarAcceso: ConsultarAcceso,
  ahora = () => new Date('2026-11-19T15:01:00Z'),
) {
  const almacen = new AlmacenContextosOfflineIndexedDb({
    indexedDB: new FabricaIndexedDbFalsa().factory,
    nombreBase: 'servicio-autorizacion-prueba',
    ahora,
  })
  return {
    almacen,
    servicio: new ServicioAutorizacionOffline({
      almacen,
      consultarAcceso,
    }),
  }
}

function acceso(rol: 'PROPIETARIO' | 'EDITOR' | 'LECTOR' = 'PROPIETARIO') {
  return {
    ganaderiaId: ganaderia,
    rol,
    estado: 'ACTIVA' as const,
    validadoEn: '2026-11-19T15:00:00Z',
  }
}

describe('servicio de autorización offline', () => {
  it.each(['PROPIETARIO', 'EDITOR'] as const)(
    'prepara autorización online para %s sin persistir el JWT',
    async rol => {
      const consultar = vi.fn<ConsultarAcceso>().mockResolvedValue(acceso(rol))
      const { almacen, servicio } = preparar(consultar)

      await expect(servicio.revalidarOnline(sesion, ganaderia))
        .resolves.toMatchObject({ estado: 'AUTORIZADA_ESCRITURA' })
      expect(consultar).toHaveBeenCalledWith(ganaderia, sesion.accessToken)
      expect(JSON.stringify(await almacen.obtener(usuario, ganaderia)))
        .not.toContain(sesion.accessToken)
    },
  )

  it('mantiene al lector como solo lectura', async () => {
    const { servicio } = preparar(
      vi.fn<ConsultarAcceso>().mockResolvedValue(acceso('LECTOR')),
    )
    await expect(servicio.revalidarOnline(sesion, ganaderia))
      .resolves.toMatchObject({ estado: 'SOLO_LECTURA' })
    await expect(servicio.evaluarEscrituraLocal(usuario, ganaderia))
      .resolves.toMatchObject({ estado: 'SOLO_LECTURA' })
  })

  it('exige sesión tras un 401 y bloquea tras un 403 seguro', async () => {
    const consultar = vi.fn<ConsultarAcceso>()
      .mockResolvedValueOnce(acceso())
      .mockRejectedValueOnce(new ErrorGanaderia('sesion'))
      .mockResolvedValueOnce(acceso())
      .mockRejectedValueOnce(new ErrorGanaderia('seguridad'))
    const { servicio } = preparar(consultar)
    await servicio.revalidarOnline(sesion, ganaderia)

    await expect(servicio.revalidarOnline(sesion, ganaderia))
      .resolves.toMatchObject({ estado: 'REQUIERE_SESION' })
    await servicio.revalidarOnline(sesion, ganaderia)
    await expect(servicio.revalidarOnline(sesion, ganaderia))
      .resolves.toMatchObject({ estado: 'BLOQUEADA' })
  })

  it('distingue fallo de red y servicio sin renovar ni borrar el contexto', async () => {
    const consultar = vi.fn<ConsultarAcceso>()
      .mockResolvedValueOnce(acceso())
      .mockRejectedValueOnce(new ErrorGanaderia('conexion'))
      .mockRejectedValueOnce(new ErrorGanaderia('servicio'))
    const { almacen, servicio } = preparar(consultar)
    await servicio.revalidarOnline(sesion, ganaderia)
    const original = await almacen.obtener(usuario, ganaderia)

    await expect(servicio.revalidarOnline(sesion, ganaderia))
      .resolves.toMatchObject({ estado: 'RED_NO_DISPONIBLE' })
    await expect(servicio.revalidarOnline(sesion, ganaderia))
      .resolves.toMatchObject({ estado: 'SERVICIO_NO_DISPONIBLE' })
    expect(await almacen.obtener(usuario, ganaderia)).toEqual(original)
  })

  it('bloquea los contextos al cerrar sesión o cambiar de cuenta', async () => {
    const { servicio } = preparar(
      vi.fn<ConsultarAcceso>().mockResolvedValue(acceso()),
    )
    await servicio.revalidarOnline(sesion, ganaderia)

    await expect(servicio.bloquearPorCierreOCambioDeCuenta(usuario))
      .resolves.toBe(1)
    await expect(servicio.evaluarEscrituraLocal(usuario, ganaderia))
      .resolves.toMatchObject({ estado: 'REQUIERE_SESION' })
  })
})
