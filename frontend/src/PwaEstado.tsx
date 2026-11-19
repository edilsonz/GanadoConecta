import { useEffect, useState } from 'react'
import { registrarPwa, type ActualizadorPwa } from './pwaRegistro'

type EventoInstalacion = Event & {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

export function useEstadoConexion() {
  const [conectado, setConectado] = useState(
    () => typeof navigator === 'undefined' || navigator.onLine !== false,
  )
  useEffect(() => {
    const online = () => setConectado(true)
    const offline = () => setConectado(false)
    window.addEventListener('online', online)
    window.addEventListener('offline', offline)
    return () => {
      window.removeEventListener('online', online)
      window.removeEventListener('offline', offline)
    }
  }, [])
  return conectado
}

type PwaEstadoProps = {
  conectado: boolean
  sesionActiva: boolean
}

export function PwaEstado({ conectado, sesionActiva }: PwaEstadoProps) {
  const [instalacion, setInstalacion] = useState<EventoInstalacion | null>(null)
  const [listaOffline, setListaOffline] = useState(false)
  const [nuevaVersion, setNuevaVersion] = useState(false)
  const [errorPreparacion, setErrorPreparacion] = useState(false)
  const [instalando, setInstalando] = useState(false)
  const [actualizando, setActualizando] = useState(false)
  const [actualizar, setActualizar] = useState<ActualizadorPwa | null>(null)

  useEffect(() => {
    const capturar = (evento: Event) => {
      evento.preventDefault()
      setInstalacion(evento as EventoInstalacion)
    }
    const instalada = () => setInstalacion(null)
    window.addEventListener('beforeinstallprompt', capturar)
    window.addEventListener('appinstalled', instalada)
    return () => {
      window.removeEventListener('beforeinstallprompt', capturar)
      window.removeEventListener('appinstalled', instalada)
    }
  }, [])

  useEffect(() => {
    const ejecutarActualizacion = registrarPwa({
      onListaOffline: () => setListaOffline(true),
      onNuevaVersion: () => setNuevaVersion(true),
      onError: () => setErrorPreparacion(true),
    })
    setActualizar(() => ejecutarActualizacion)
  }, [])

  async function instalar() {
    if (!instalacion || instalando) return
    setInstalando(true)
    try {
      await instalacion.prompt()
      await instalacion.userChoice
      setInstalacion(null)
    } finally {
      setInstalando(false)
    }
  }

  async function confirmarActualizacion() {
    if (!actualizar || actualizando) return
    setActualizando(true)
    try {
      await actualizar(true)
    } catch {
      setActualizando(false)
      setErrorPreparacion(true)
    }
  }

  if (conectado && !instalacion && !listaOffline && !nuevaVersion && !errorPreparacion) {
    return null
  }

  return (
    <aside className={`pwa-status ${conectado ? 'online' : 'offline'}`}
      aria-label="Estado de la aplicación" aria-live="polite">
      {!conectado && (
        <div className="pwa-status-message">
          <strong>Aplicación sin conexión</strong>
          <span>{sesionActiva
            ? 'La estructura está disponible localmente. Solo usa funciones offline previamente preparadas.'
            : 'La estructura está disponible, pero debes recuperar conexión e iniciar sesión para acceder a datos privados.'}</span>
        </div>
      )}
      {listaOffline && conectado && (
        <div className="pwa-status-message">
          <strong>Recursos básicos disponibles sin conexión</strong>
          <span>Esto no sustituye el inicio de sesión ni los permisos de cada ganadería.</span>
          <button type="button" onClick={() => setListaOffline(false)}>Entendido</button>
        </div>
      )}
      {instalacion && conectado && (
        <div className="pwa-status-message">
          <strong>GanadoConecta se puede instalar</strong>
          <span>La instalación permite abrir la estructura básica; los datos privados conservan sus controles de acceso.</span>
          <button type="button" onClick={() => void instalar()} disabled={instalando}>
            {instalando ? 'Instalando…' : 'Instalar aplicación'}
          </button>
        </div>
      )}
      {nuevaVersion && (
        <div className="pwa-status-message">
          <strong>Nueva versión disponible</strong>
          <span>Termina cualquier formulario o sincronización antes de actualizar. No se recargará automáticamente.</span>
          <button type="button" onClick={() => void confirmarActualizacion()}
            disabled={actualizando}>
            {actualizando ? 'Actualizando…' : 'Actualizar ahora'}
          </button>
        </div>
      )}
      {errorPreparacion && (
        <div className="pwa-status-message pwa-status-error">
          <strong>No se prepararon los recursos offline</strong>
          <span>Continúa con conexión. Tus operaciones de IndexedDB no se eliminaron.</span>
          <button type="button" onClick={() => setErrorPreparacion(false)}>Cerrar</button>
        </div>
      )}
    </aside>
  )
}

export function AccesoOfflineSeguro() {
  return (
    <section className="offline-safe" aria-labelledby="offline-safe-title">
      <p className="eyebrow"><span className="small-rule" /> MODO SEGURO SIN CONEXIÓN</p>
      <h2 id="offline-safe-title">La aplicación está disponible.</h2>
      <p>No encontramos una sesión activa en memoria. Por seguridad no mostraremos
        ganaderías, animales ni operaciones locales hasta recuperar conexión e
        iniciar sesión con la cuenta autorizada.</p>
      <p className="offline-safe-note">Instalar la aplicación y abrir su estructura
        sin internet no concede autorización para trabajar con datos privados.</p>
    </section>
  )
}
