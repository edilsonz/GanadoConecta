import { FormEvent, useRef, useState } from 'react'
import simboloMarca from '../../assets/marca/ganadoconecta-simbolo.png'
import { ErrorRegistro, registrarCuenta } from './authApi'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'

type ErroresFormulario = Partial<Record<'correo' | 'contrasena' | 'confirmacion', string>>

function contarCaracteres(texto: string) {
  return Array.from(texto).length
}

function validar(correo: string, contrasena: string, confirmacion: string): ErroresFormulario {
  const errores: ErroresFormulario = {}
  const correoNormalizado = correo.trim()
  const arroba = correoNormalizado.indexOf('@')
  const correoValido = arroba > 0
    && arroba === correoNormalizado.lastIndexOf('@')
    && arroba < correoNormalizado.length - 1
    && contarCaracteres(correoNormalizado) <= 254
    && !/[\p{White_Space}\p{Cc}]/u.test(correoNormalizado)

  if (!correoValido) errores.correo = 'Ingresa un correo electrónico válido.'
  const longitud = contarCaracteres(contrasena)
  if (longitud < 15 || longitud > 128) {
    errores.contrasena = 'La contraseña debe tener entre 15 y 128 caracteres.'
  }
  if (confirmacion !== contrasena) {
    errores.confirmacion = 'Las contraseñas no coinciden.'
  }
  return errores
}

const mensajesError = {
  duplicado: 'No pudimos completar el registro. El correo podría estar registrado.',
  limite: 'Se alcanzó el límite temporal de intentos. Espera unos minutos antes de volver a intentar.',
  seguridad: 'No pudimos validar la seguridad de la solicitud. Actualiza la página e inténtalo otra vez.',
  conexion: 'No fue posible conectar con el servicio. Revisa tu conexión e inténtalo nuevamente.',
  servicio: 'El registro no está disponible en este momento. Inténtalo más tarde.',
  validacion: 'Revisa los datos indicados antes de continuar.',
} as const

export function RegistroCuenta() {
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [errores, setErrores] = useState<ErroresFormulario>({})
  const [mensajeError, setMensajeError] = useState<string | null>(null)
  const [correoCreado, setCorreoCreado] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const envioEnCurso = useRef(false)
  useProteccionActualizacionPwa(
    'formulario-registro-cuenta',
    'FORMULARIO',
    Boolean(correo || contrasena || confirmacion || enviando),
  )

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (envioEnCurso.current) return

    const erroresLocales = validar(correo, contrasena, confirmacion)
    setErrores(erroresLocales)
    setMensajeError(null)
    setCorreoCreado(null)
    if (Object.keys(erroresLocales).length > 0) return

    envioEnCurso.current = true
    setEnviando(true)
    try {
      const cuenta = await registrarCuenta(correo, contrasena)
      setCorreoCreado(cuenta.correo)
      setCorreo('')
      setContrasena('')
      setConfirmacion('')
    } catch (error) {
      const fallo = error instanceof ErrorRegistro ? error : new ErrorRegistro('servicio')
      const erroresServidor: ErroresFormulario = {}
      if (fallo.erroresPorCampo.correo) erroresServidor.correo = fallo.erroresPorCampo.correo
      if (fallo.erroresPorCampo.contrasena) erroresServidor.contrasena = fallo.erroresPorCampo.contrasena
      setErrores(erroresServidor)
      setMensajeError(mensajesError[fallo.tipo])
    } finally {
      envioEnCurso.current = false
      setEnviando(false)
    }
  }

  const longitudContrasena = contarCaracteres(contrasena)

  return (
    <section className="register-section" id="crear-cuenta" aria-labelledby="register-title">
      <div className="register-intro">
        <img className="account-emblem" src={simboloMarca} alt="" width="288" height="112" />
        <h2 id="register-title">Crea tu cuenta.</h2>
        <p>Después de crear tu cuenta, inicia sesión para crear una ganadería o aceptar una invitación familiar.</p>
      </div>

      <div className="register-card">
        <div className="register-card-heading">
          <div><h3>Datos de tu cuenta</h3><p>Todos los campos son obligatorios.</p></div>
        </div>

        {correoCreado && (
          <div className="form-notice success-notice" role="status">
            <strong>Cuenta creada correctamente.</strong>
            <span>{correoCreado}</span>
            <p>Tu sesión no se ha iniciado. Ya puedes usar el formulario de inicio de sesión.</p>
          </div>
        )}
        {mensajeError && <div className="form-notice error-notice" role="alert">{mensajeError}</div>}

        <form onSubmit={enviar} noValidate aria-busy={enviando}>
          <div className="form-field">
            <label htmlFor="registro-correo">Correo electrónico</label>
            <input
              id="registro-correo"
              name="correo"
              type="email"
              autoComplete="email"
              inputMode="email"
              value={correo}
              aria-invalid={Boolean(errores.correo)}
              aria-describedby={errores.correo ? 'error-correo' : undefined}
              onChange={event => setCorreo(event.target.value)}
              disabled={enviando}
              placeholder="nombre@correo.com"
            />
            {errores.correo && <span className="field-error" id="error-correo">{errores.correo}</span>}
          </div>

          <div className="form-field">
            <label htmlFor="registro-contrasena">Contraseña</label>
            <input
              id="registro-contrasena"
              name="contrasena"
              type="password"
              autoComplete="new-password"
              value={contrasena}
              aria-invalid={Boolean(errores.contrasena)}
              aria-describedby="ayuda-contrasena"
              onChange={event => setContrasena(event.target.value)}
              disabled={enviando}
            />
            <span className={`field-help ${errores.contrasena ? 'field-error' : ''}`} id="ayuda-contrasena">
              {errores.contrasena ?? `Entre 15 y 128 caracteres · ${longitudContrasena}/128`}
            </span>
          </div>

          <div className="form-field">
            <label htmlFor="registro-confirmacion">Confirma tu contraseña</label>
            <input
              id="registro-confirmacion"
              name="confirmacion"
              type="password"
              autoComplete="new-password"
              value={confirmacion}
              aria-invalid={Boolean(errores.confirmacion)}
              aria-describedby={errores.confirmacion ? 'error-confirmacion' : undefined}
              onChange={event => setConfirmacion(event.target.value)}
              disabled={enviando}
            />
            {errores.confirmacion && <span className="field-error" id="error-confirmacion">{errores.confirmacion}</span>}
          </div>

          <button className="button button-primary submit-button" type="submit" disabled={enviando}>
            {enviando ? 'Creando cuenta…' : 'Crear mi cuenta'} <span aria-hidden="true">→</span>
          </button>
          <p className="privacy-note">No guardamos tu contraseña en este navegador.</p>
        </form>
      </div>
    </section>
  )
}
