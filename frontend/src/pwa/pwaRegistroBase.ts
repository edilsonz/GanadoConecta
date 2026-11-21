export type EventosRegistroPwa = {
  onListaOffline: () => void
  onNuevaVersion: () => void
  onError: () => void
}

type OpcionesRegistro = {
  immediate: boolean
  onOfflineReady: () => void
  onNeedRefresh: () => void
  onRegisterError: () => void
}

type RegistrarServiceWorker = (
  opciones: OpcionesRegistro,
) => (recargar?: boolean) => Promise<void>

export type ActualizadorPwa = (recargar?: boolean) => Promise<void>

export function crearRegistroPwa(
  esProduccion: boolean,
  soportaServiceWorker: boolean,
  eventos: EventosRegistroPwa,
  registrar: RegistrarServiceWorker,
): ActualizadorPwa {
  if (!esProduccion || !soportaServiceWorker) {
    return async () => undefined
  }
  return registrar({
    immediate: true,
    onOfflineReady: eventos.onListaOffline,
    onNeedRefresh: eventos.onNuevaVersion,
    onRegisterError: eventos.onError,
  })
}
