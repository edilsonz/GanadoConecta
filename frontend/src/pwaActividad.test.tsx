import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  ejecutarConProteccionActualizacionPwa,
  ejecutarActualizacionSeguraPwa,
  type GestorBloqueosPwa,
  type TipoActividadPwa,
  useEstadoActividadPwa,
  useProteccionActualizacionPwa,
} from './pwaActividad'

function Actividad({ activa, tipo = 'FORMULARIO' }: {
  activa: boolean
  tipo?: TipoActividadPwa
}) {
  useProteccionActualizacionPwa(`prueba-${tipo}`, tipo, activa)
  return null
}

function Estado() {
  const estado = useEstadoActividadPwa()
  return <output>{`${estado.formularioEnEdicion}:${estado.sincronizacionEnCurso}`}</output>
}

describe('coordinación segura de actualizaciones PWA', () => {
  it('publica formularios y sincronizaciones activas y libera el bloqueo al desmontar', async () => {
    const vista = render(<><Actividad activa /><Estado /></>)
    expect(await screen.findByText('true:false')).toBeInTheDocument()

    vista.rerender(<><Actividad activa tipo="SINCRONIZACION" /><Estado /></>)
    expect(await screen.findByText('false:true')).toBeInTheDocument()

    vista.unmount()
    render(<Estado />)
    expect(screen.getByText('false:false')).toBeInTheDocument()
  })

  it('mantiene un bloqueo compartido de navegador mientras existe actividad', async () => {
    const descriptorOriginal = Object.getOwnPropertyDescriptor(navigator, 'locks')
    const gestor: GestorBloqueosPwa = {
      request: vi.fn(async (_nombre, opciones, ejecutar) => {
        expect(opciones).toMatchObject({ mode: 'shared' })
        return ejecutar({ name: 'pwa', mode: 'shared' } as Lock)
      }),
    }
    Object.defineProperty(navigator, 'locks', { configurable: true, value: gestor })

    const vista = render(<Actividad activa />)
    await waitFor(() => expect(gestor.request).toHaveBeenCalledOnce())
    vista.unmount()

    if (descriptorOriginal) Object.defineProperty(navigator, 'locks', descriptorOriginal)
    else Reflect.deleteProperty(navigator, 'locks')
  })

  it.each([
    ['formulario', { formularioEnEdicion: true, sincronizacionEnCurso: false }, 'BLOQUEADA_FORMULARIO'],
    ['sincronización', { formularioEnEdicion: false, sincronizacionEnCurso: true }, 'BLOQUEADA_SINCRONIZACION'],
  ] as const)('no actualiza mientras existe %s local', async (_caso, estado, esperado) => {
    const actualizar = vi.fn().mockResolvedValue(undefined)

    await expect(ejecutarActualizacionSeguraPwa(actualizar, estado, undefined))
      .resolves.toBe(esperado)
    expect(actualizar).not.toHaveBeenCalled()
  })

  it('detecta actividad en otra ventana mediante un bloqueo compartido', async () => {
    const actualizar = vi.fn().mockResolvedValue(undefined)
    const gestor: GestorBloqueosPwa = {
      request: vi.fn(async (_nombre, opciones, ejecutar) => {
        expect(opciones).toMatchObject({ mode: 'exclusive', ifAvailable: true })
        return ejecutar(null)
      }),
    }

    await expect(ejecutarActualizacionSeguraPwa(actualizar, {
      formularioEnEdicion: false,
      sincronizacionEnCurso: false,
    }, gestor)).resolves.toBe('BLOQUEADA_OTRA_VENTANA')
    expect(actualizar).not.toHaveBeenCalled()
  })

  it('aplica la actualización bajo bloqueo exclusivo cuando todas las ventanas están libres', async () => {
    const actualizar = vi.fn().mockResolvedValue(undefined)
    const gestor: GestorBloqueosPwa = {
      request: vi.fn(async (_nombre, opciones, ejecutar) => {
        expect(opciones).toMatchObject({ mode: 'exclusive', ifAvailable: true })
        return ejecutar({ name: 'pwa', mode: 'exclusive' } as Lock)
      }),
    }

    await expect(ejecutarActualizacionSeguraPwa(actualizar, {
      formularioEnEdicion: false,
      sincronizacionEnCurso: false,
    }, gestor)).resolves.toBe('APLICADA')
    await waitFor(() => expect(actualizar).toHaveBeenCalledOnce())
  })

  it('protege trabajo en primer plano con el mismo bloqueo compartido', async () => {
    const accion = vi.fn().mockResolvedValue('confirmada')
    const gestor: GestorBloqueosPwa = {
      request: vi.fn(async (_nombre, opciones, ejecutar) => {
        expect(opciones).toMatchObject({ mode: 'shared' })
        return ejecutar({ name: 'pwa', mode: 'shared' } as Lock)
      }),
    }

    await expect(ejecutarConProteccionActualizacionPwa(accion, gestor))
      .resolves.toBe('confirmada')
    expect(accion).toHaveBeenCalledOnce()
  })
})
