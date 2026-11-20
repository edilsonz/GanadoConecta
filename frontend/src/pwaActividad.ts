import { useEffect, useSyncExternalStore } from 'react'

export type TipoActividadPwa = 'FORMULARIO' | 'SINCRONIZACION'

export type EstadoActividadPwa = {
  formularioEnEdicion: boolean
  sincronizacionEnCurso: boolean
}

export type ResultadoActualizacionPwa =
  | 'APLICADA'
  | 'BLOQUEADA_FORMULARIO'
  | 'BLOQUEADA_SINCRONIZACION'
  | 'BLOQUEADA_OTRA_VENTANA'

export type GestorBloqueosPwa = {
  request<T>(
    nombre: string,
    opciones: { mode: 'shared' | 'exclusive'; ifAvailable?: boolean },
    ejecutar: (bloqueo: Lock | null) => T | Promise<T>,
  ): Promise<T>
}

const NOMBRE_BLOQUEO_ACTUALIZACION = 'ganadoconecta:pwa-actualizacion-segura'
const actividades = new Map<string, TipoActividadPwa>()
const suscriptores = new Set<() => void>()
let estado: EstadoActividadPwa = {
  formularioEnEdicion: false,
  sincronizacionEnCurso: false,
}

function gestorBloqueosNavegador(): GestorBloqueosPwa | undefined {
  if (typeof navigator === 'undefined' || !navigator.locks) return undefined
  return navigator.locks as unknown as GestorBloqueosPwa
}

function recalcularEstado() {
  const siguiente = {
    formularioEnEdicion: [...actividades.values()].includes('FORMULARIO'),
    sincronizacionEnCurso: [...actividades.values()].includes('SINCRONIZACION'),
  }
  if (siguiente.formularioEnEdicion === estado.formularioEnEdicion
      && siguiente.sincronizacionEnCurso === estado.sincronizacionEnCurso) return
  estado = siguiente
  suscriptores.forEach(notificar => notificar())
}

function establecerActividad(clave: string, tipo: TipoActividadPwa, activa: boolean) {
  if (activa) actividades.set(clave, tipo)
  else actividades.delete(clave)
  recalcularEstado()
}

function suscribir(notificar: () => void) {
  suscriptores.add(notificar)
  return () => suscriptores.delete(notificar)
}

export function useEstadoActividadPwa() {
  return useSyncExternalStore(suscribir, () => estado, () => estado)
}

export function useProteccionActualizacionPwa(
  clave: string,
  tipo: TipoActividadPwa,
  activa: boolean,
) {
  useEffect(() => {
    establecerActividad(clave, tipo, activa)
    return () => establecerActividad(clave, tipo, false)
  }, [activa, clave, tipo])

  useEffect(() => {
    const gestor = gestorBloqueosNavegador()
    if (!activa || !gestor) return

    let liberar: (() => void) | undefined
    let cancelada = false
    void gestor.request(
      NOMBRE_BLOQUEO_ACTUALIZACION,
      { mode: 'shared' },
      async bloqueo => {
        if (!bloqueo || cancelada) return
        await new Promise<void>(resolver => { liberar = resolver })
      },
    ).catch(() => undefined)

    return () => {
      cancelada = true
      liberar?.()
    }
  }, [activa])
}

export async function ejecutarActualizacionSeguraPwa(
  actualizar: () => Promise<void>,
  actividad: EstadoActividadPwa,
  gestor: GestorBloqueosPwa | undefined = gestorBloqueosNavegador(),
): Promise<ResultadoActualizacionPwa> {
  if (actividad.sincronizacionEnCurso) return 'BLOQUEADA_SINCRONIZACION'
  if (actividad.formularioEnEdicion) return 'BLOQUEADA_FORMULARIO'
  if (!gestor) {
    await actualizar()
    return 'APLICADA'
  }

  return gestor.request(
    NOMBRE_BLOQUEO_ACTUALIZACION,
    { mode: 'exclusive', ifAvailable: true },
    async bloqueo => {
      if (!bloqueo) return 'BLOQUEADA_OTRA_VENTANA'
      await actualizar()
      return 'APLICADA'
    },
  )
}

export async function ejecutarConProteccionActualizacionPwa<T>(
  accion: () => Promise<T>,
  gestor: GestorBloqueosPwa | undefined = gestorBloqueosNavegador(),
): Promise<T> {
  if (!gestor) return accion()
  return gestor.request(
    NOMBRE_BLOQUEO_ACTUALIZACION,
    { mode: 'shared' },
    bloqueo => {
      if (!bloqueo) throw new Error('BLOQUEO_PWA_NO_DISPONIBLE')
      return accion()
    },
  )
}
