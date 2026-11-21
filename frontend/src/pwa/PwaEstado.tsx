import { useEffect, useState } from 'react'
import { registrarPwa, type ActualizadorPwa } from './pwaRegistro'
import {
  ejecutarActualizacionSeguraPwa,
  useEstadoActividadPwa,
} from './pwaActividad'

const MARCA_ACTUALIZACION_APLICADA = 'ganadoconecta:pwa-actualizacion-aplicada'

function marcarActualizacionAplicada(pendiente: boolean) {
  try {
    if (pendiente) sessionStorage.setItem(MARCA_ACTUALIZACION_APLICADA, '1')
    else sessionStorage.removeItem(MARCA_ACTUALIZACION_APLICADA)
  } catch {
    // El aviso es auxiliar y nunca sustituye el estado del Service Worker.
  }
}

function consumirActualizacionAplicada() {
  try {
    const aplicada = sessionStorage.getItem(MARCA_ACTUALIZACION_APLICADA) === '1'
    sessionStorage.removeItem(MARCA_ACTUALIZACION_APLICADA)
    return aplicada
  } catch {
    return false
  }
}

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
  const [bloqueoOtraVentana, setBloqueoOtraVentana] = useState(false)
  const [actualizacionAplicada, setActualizacionAplicada] = useState(
    consumirActualizacionAplicada,
  )
  const actividad = useEstadoActividadPwa()

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
      onNuevaVersion: () => {
        setNuevaVersion(true)
        setBloqueoOtraVentana(false)
      },
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
    setBloqueoOtraVentana(false)
    try {
      const resultado = await ejecutarActualizacionSeguraPwa(
        async () => {
          marcarActualizacionAplicada(true)
          await actualizar(true)
        },
        actividad,
      )
      if (resultado === 'BLOQUEADA_OTRA_VENTANA') {
        marcarActualizacionAplicada(false)
        setBloqueoOtraVentana(true)
        setActualizando(false)
        return
      }
      if (resultado !== 'APLICADA') {
        setActualizando(false)
        return
      }
      setNuevaVersion(false)
      setActualizacionAplicada(true)
      setActualizando(false)
    } catch {
      marcarActualizacionAplicada(false)
      setActualizando(false)
      setErrorPreparacion(true)
    }
  }

  const actualizacionBloqueada = actividad.formularioEnEdicion
    || actividad.sincronizacionEnCurso

  if (conectado && !instalacion && !listaOffline && !nuevaVersion
      && !errorPreparacion && !actualizacionAplicada) {
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
            : 'Se necesita conexión para verificar la sesión. Las operaciones locales permanecen conservadas y ocultas hasta iniciar sesión con la cuenta autorizada.'}</span>
        </div>
      )}
      {listaOffline && conectado && (
        <div className="pwa-status-message">
          <strong>Aplicación disponible sin conexión</strong>
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
          <span>{actividad.sincronizacionEnCurso
            ? 'Hay una sincronización manual en curso. Espera a que termine para actualizar.'
            : actividad.formularioEnEdicion
              ? 'Hay un formulario con cambios sin guardar. Complétalo o vacíalo antes de actualizar.'
              : bloqueoOtraVentana
                ? 'Otra ventana tiene un formulario o una sincronización en curso. Ciérrala o termina esa tarea y vuelve a intentar.'
                : 'Puedes actualizar ahora. No se recargará automáticamente sin tu confirmación.'}</span>
          <button type="button" onClick={() => void confirmarActualizacion()}
            disabled={actualizando || actualizacionBloqueada}>
            {actualizando ? 'Actualizando…' : 'Actualizar ahora'}
          </button>
        </div>
      )}
      {actualizacionAplicada && (
        <div className="pwa-status-message">
          <strong>Actualización aplicada correctamente</strong>
          <span>La estructura está actualizada y las operaciones locales se conservaron.</span>
          <button type="button" onClick={() => {
            marcarActualizacionAplicada(false)
            setActualizacionAplicada(false)
          }}>Entendido</button>
        </div>
      )}
      {errorPreparacion && (
        <div className="pwa-status-message pwa-status-error">
          <strong>Actualización no disponible o fallida</strong>
          <span>La versión anterior sigue disponible. Tus operaciones de IndexedDB no se eliminaron.</span>
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
        sin internet no concede autorización para trabajar con datos privados.
        Las operaciones pendientes se conservan, pero permanecen ocultas.</p>
    </section>
  )
}
