import { FormEvent, useEffect, useRef, useState } from 'react'
import {
  cerrarSesionRemota,
  ErrorSesion,
  iniciarSesion,
  renovarSesionControlada,
  type SesionActiva,
} from './sesionApi'
import { bloquearContextosOfflineUsuario } from '../../offline/registroAnimalesOffline'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'

const MARCA_CIERRE_PENDIENTE = 'ganadoconecta:salida-remota-pendiente'

const mensajesError = {
  credenciales: 'El correo o la contraseña no son válidos.',
  limite: 'Se alcanzó el límite temporal de intentos. Espera unos minutos antes de volver a intentar.',
  seguridad: 'No pudimos validar la seguridad de la solicitud. Actualiza la página e inténtalo otra vez.',
  conexion: 'No fue posible conectar con el servicio. Revisa tu conexión e inténtalo nuevamente.',
  sesion: 'Tu sesión terminó. Vuelve a iniciar sesión para continuar.',
  servicio: 'El acceso no está disponible en este momento. Inténtalo más tarde.',
} as const

function leerCierrePendiente() {
  try {
    return localStorage.getItem(MARCA_CIERRE_PENDIENTE) === '1'
  } catch {
    return false
  }
}

function guardarCierrePendiente(pendiente: boolean) {
  try {
    if (pendiente) localStorage.setItem(MARCA_CIERRE_PENDIENTE, '1')
    else localStorage.removeItem(MARCA_CIERRE_PENDIENTE)
  } catch {
    // La sesión sigue cerrada localmente aunque el almacenamiento no esté disponible.
  }
}

type AccesoCuentaProps = {
  sesion: SesionActiva | null
  onSesionChange: (sesion: SesionActiva | null) => void
}

export function AccesoCuenta({ sesion, onSesionChange }: AccesoCuentaProps) {
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [restaurando, setRestaurando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [mensajeError, setMensajeError] = useState<string | null>(null)
  const [errorCorreo, setErrorCorreo] = useState<string | null>(null)
  const envioEnCurso = useRef(false)
  useProteccionActualizacionPwa(
    'formulario-inicio-sesion',
    'FORMULARIO',
    Boolean(enviando || (!sesion && (correo || contrasena))),
  )

  useEffect(() => {
    let activo = true
    async function restaurar() {
      if (leerCierrePendiente()) {
        try {
          await cerrarSesionRemota()
          guardarCierrePendiente(false)
        } catch {
          if (activo) setMensajeError('El cierre remoto sigue pendiente. Tus datos permanecen bloqueados en este dispositivo.')
        } finally {
          if (activo) setRestaurando(false)
        }
        return
      }

      try {
        const restaurada = await renovarSesionControlada()
        if (activo) onSesionChange(restaurada)
      } catch (error) {
        if (activo && error instanceof ErrorSesion && error.tipo !== 'sesion') {
          setMensajeError('No pudimos restaurar la sesión. Puedes iniciar sesión nuevamente.')
        }
      } finally {
        if (activo) setRestaurando(false)
      }
    }
    void restaurar()
    return () => { activo = false }
  }, [onSesionChange])

  useEffect(() => {
    if (!sesion) return
    const demora = Math.min(
      2_147_000_000,
      Math.max(0, Date.parse(sesion.expiresAt) - Date.now() - 60_000),
    )
    const temporizador = window.setTimeout(async () => {
      try {
        const renovada = await renovarSesionControlada()
        onSesionChange({ ...renovada, correo: sesion.correo })
      } catch {
        onSesionChange(null)
        try {
          await bloquearContextosOfflineUsuario(sesion.usuarioId)
          setMensajeError('No pudimos renovar la sesión de forma segura. Vuelve a iniciar sesión.')
        } catch {
          setMensajeError('La sesión terminó y no pudimos actualizar el almacenamiento offline. Recupera la conexión e inicia sesión antes de continuar.')
        }
      }
    }, demora)
    return () => window.clearTimeout(temporizador)
  }, [sesion?.correo, sesion?.expiresAt, onSesionChange])

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (envioEnCurso.current) return
    const normalizado = correo.trim()
    if (!normalizado || !normalizado.includes('@')) {
      setErrorCorreo('Ingresa un correo electrónico válido.')
      return
    }
    if (!contrasena) {
      setMensajeError('Ingresa tu contraseña para continuar.')
      return
    }

    envioEnCurso.current = true
    setEnviando(true)
    setErrorCorreo(null)
    setMensajeError(null)
    try {
      const nuevaSesion = await iniciarSesion(correo, contrasena)
      guardarCierrePendiente(false)
      onSesionChange(nuevaSesion)
      setContrasena('')
    } catch (error) {
      const fallo = error instanceof ErrorSesion ? error : new ErrorSesion('servicio')
      setMensajeError(mensajesError[fallo.tipo])
    } finally {
      envioEnCurso.current = false
      setEnviando(false)
    }
  }

  async function cerrar() {
    if (envioEnCurso.current) return
    envioEnCurso.current = true
    const usuarioId = sesion?.usuarioId
    onSesionChange(null)
    setEnviando(true)
    setMensajeError(null)
    let falloBloqueoLocal = false
    if (usuarioId) {
      try {
        await bloquearContextosOfflineUsuario(usuarioId)
      } catch {
        falloBloqueoLocal = true
      }
    }
    try {
      await cerrarSesionRemota()
      guardarCierrePendiente(false)
      if (falloBloqueoLocal) {
        setMensajeError('La sesión se cerró, pero no pudimos actualizar el almacenamiento offline. No uses este dispositivo sin conexión hasta volver a iniciar sesión.')
      }
    } catch {
      guardarCierrePendiente(true)
      setMensajeError(falloBloqueoLocal
        ? 'El cierre remoto y el bloqueo del almacenamiento offline siguen pendientes. No uses este dispositivo sin conexión hasta volver a iniciar sesión.'
        : 'Cierre remoto pendiente. La sesión está bloqueada en este dispositivo y se reintentará al volver.')
    } finally {
      envioEnCurso.current = false
      setEnviando(false)
    }
  }

  return (
    <section className={sesion ? 'login-section login-section-active' : 'login-section'}
      id="iniciar-sesion" aria-labelledby="login-title">
      <div className="login-intro">
        <h2 id="login-title">{sesion ? 'Mi cuenta' : 'Vuelve a tu cuenta.'}</h2>
        {!sesion && <p>Administra las ganaderías a las que tienes acceso.</p>}
      </div>

      <div className="login-card">
        {restaurando ? (
          <div className="session-loading" role="status" aria-live="polite">
            <span className="status-dot" aria-hidden="true" /> Comprobando tu sesión…
          </div>
        ) : sesion ? (
          <div className="authenticated-view" aria-live="polite">
            {sesion.correo
              ? <p className="session-email">{sesion.correo}</p>
              : <p className="session-email">Cuenta verificada</p>}
            <button className="button button-outline logout-button" type="button" onClick={cerrar} disabled={enviando}>
              {enviando ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </div>
        ) : (
          <form onSubmit={enviar} noValidate aria-busy={enviando}>
            {mensajeError && <div className="form-notice error-notice" role="alert">{mensajeError}</div>}
            <div className="form-field">
              <label htmlFor="login-correo">Correo electrónico</label>
              <input
                id="login-correo"
                name="correo"
                type="email"
                autoComplete="username"
                inputMode="email"
                value={correo}
                onChange={event => setCorreo(event.target.value)}
                aria-invalid={Boolean(errorCorreo)}
                aria-describedby={errorCorreo ? 'login-error-correo' : undefined}
                disabled={enviando}
                placeholder="nombre@correo.com"
              />
              {errorCorreo && <span className="field-error" id="login-error-correo">{errorCorreo}</span>}
            </div>
            <div className="form-field">
              <label htmlFor="login-contrasena">Contraseña</label>
              <input
                id="login-contrasena"
                name="contrasena"
                type="password"
                autoComplete="current-password"
                value={contrasena}
                onChange={event => setContrasena(event.target.value)}
                disabled={enviando}
              />
            </div>
            <button className="button button-primary submit-button" type="submit" disabled={enviando}>
              {enviando ? 'Iniciando sesión…' : 'Iniciar sesión'} <span aria-hidden="true">→</span>
            </button>
            <p className="privacy-note">La contraseña y el JWT de acceso no se guardan en localStorage, sessionStorage ni IndexedDB.</p>
          </form>
        )}
        {!sesion && restaurando && mensajeError && <div className="form-notice error-notice" role="alert">{mensajeError}</div>}
      </div>
    </section>
  )
}
