import { registerSW } from 'virtual:pwa-register'
import {
  crearRegistroPwa,
  type ActualizadorPwa,
  type EventosRegistroPwa,
} from './pwaRegistroBase'

export type { ActualizadorPwa, EventosRegistroPwa } from './pwaRegistroBase'

export function registrarPwa(eventos: EventosRegistroPwa): ActualizadorPwa {
  return crearRegistroPwa(
    import.meta.env.PROD,
    typeof navigator !== 'undefined' && 'serviceWorker' in navigator,
    eventos,
    registerSW,
  )
}
