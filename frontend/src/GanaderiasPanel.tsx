import { FormEvent, useEffect, useRef, useState } from 'react'
import { AnimalesPanel } from './AnimalesPanel'
import {
  crearGanaderia,
  ErrorGanaderia,
  listarGanaderias,
  type Ganaderia,
} from './ganaderiasApi'
import type { SesionActiva } from './sesionApi'
import { useProteccionActualizacionPwa } from './pwaActividad'

type GanaderiasPanelProps = {
  sesion: SesionActiva | null
  onSesionInvalida: () => void
}

const mensajesError = {
  validacion: 'Revisa el nombre. Debe tener entre 1 y 100 caracteres.',
  conflicto: 'No pudimos crear la ganadería porque existe un conflicto. Actualiza la lista e inténtalo otra vez.',
  seguridad: 'No pudimos validar la seguridad de la solicitud. Actualiza la página e inténtalo otra vez.',
  sesion: 'Tu sesión terminó. Vuelve a iniciar sesión para continuar.',
  conexion: 'No fue posible conectar con el servicio. Revisa tu conexión.',
  servicio: 'Las ganaderías no están disponibles en este momento. Inténtalo más tarde.',
} as const

function etiquetaRol(rol: Ganaderia['rol']) {
  return rol === 'PROPIETARIO' ? 'Propietario' : rol === 'EDITOR' ? 'Editor' : 'Lector'
}

export function GanaderiasPanel({ sesion, onSesionInvalida }: GanaderiasPanelProps) {
  const [ganaderias, setGanaderias] = useState<Ganaderia[]>([])
  const [seleccionada, setSeleccionada] = useState<string | null>(null)
  const [nombre, setNombre] = useState('')
  const [cargando, setCargando] = useState(false)
  const [creando, setCreando] = useState(false)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [errorNombre, setErrorNombre] = useState<string | null>(null)
  const envioEnCurso = useRef(false)
  useProteccionActualizacionPwa(
    'formulario-crear-ganaderia',
    'FORMULARIO',
    Boolean(nombre || creando),
  )

  useEffect(() => {
    if (!sesion) {
      setGanaderias([])
      setSeleccionada(null)
      setMensaje(null)
      return
    }
    let activo = true
    setCargando(true)
    setMensaje(null)
    listarGanaderias(sesion.accessToken)
      .then(resultado => {
        if (activo) {
          setGanaderias(resultado)
          setSeleccionada(actual => resultado.some(item => item.id === actual) ? actual : null)
        }
      })
      .catch(error => {
        if (!activo) return
        const fallo = error instanceof ErrorGanaderia ? error : new ErrorGanaderia('servicio')
        setMensaje(mensajesError[fallo.tipo])
        if (fallo.tipo === 'sesion') onSesionInvalida()
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => { activo = false }
  }, [sesion, onSesionInvalida])

  async function crear(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!sesion || envioEnCurso.current) return
    const normalizado = nombre.trim()
    const longitud = Array.from(normalizado).length
    if (longitud < 1 || longitud > 100) {
      setErrorNombre('Ingresa un nombre de 1 a 100 caracteres.')
      return
    }

    envioEnCurso.current = true
    setCreando(true)
    setErrorNombre(null)
    setMensaje(null)
    try {
      const creada = await crearGanaderia(normalizado, sesion.accessToken)
      setGanaderias(actuales => {
        const sinDuplicado = actuales.filter(item => item.id !== creada.id)
        return [...sinDuplicado, creada].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
      })
      setSeleccionada(creada.id)
      setNombre('')
      setMensaje('Ganadería creada y seleccionada correctamente.')
    } catch (error) {
      const fallo = error instanceof ErrorGanaderia ? error : new ErrorGanaderia('servicio')
      setMensaje(mensajesError[fallo.tipo])
      if (fallo.tipo === 'sesion') onSesionInvalida()
    } finally {
      envioEnCurso.current = false
      setCreando(false)
    }
  }

  return (
    <section className="farms-section" id="mis-ganaderias" aria-labelledby="farms-title">
      <div className="farms-heading">
        <p className="eyebrow"><span className="small-rule" /> MIS GANADERÍAS</p>
        <h2 id="farms-title">Elige dónde trabajar.</h2>
        <p>Selecciona una ganadería para consultar y registrar sus animales según tu permiso vigente.</p>
      </div>

      {!sesion ? (
        <div className="farms-empty"><p>Inicia sesión para crear y consultar tus ganaderías autorizadas.</p></div>
      ) : (
        <div className="farms-workspace">
          <form className="farm-create" onSubmit={crear} noValidate aria-busy={creando}>
            <div className="form-field">
              <label htmlFor="ganaderia-nombre">Nombre de la ganadería</label>
              <input
                id="ganaderia-nombre"
                name="nombre"
                value={nombre}
                onChange={event => setNombre(event.target.value)}
                maxLength={100}
                autoComplete="organization"
                disabled={creando}
                aria-invalid={Boolean(errorNombre)}
                aria-describedby={errorNombre ? 'ganaderia-error-nombre' : undefined}
                placeholder="Ej. Los Pinos"
              />
              {errorNombre && <span className="field-error" id="ganaderia-error-nombre">{errorNombre}</span>}
            </div>
            <button className="button button-primary" type="submit" disabled={creando}>
              {creando ? 'Creando ganadería…' : 'Crear ganadería'}
            </button>
          </form>

          {mensaje && <div className="form-notice" role="status">{mensaje}</div>}
          {cargando ? (
            <p className="farms-loading" role="status">Consultando tus ganaderías…</p>
          ) : ganaderias.length === 0 ? (
            <div className="farms-empty"><p>Aún no tienes ganaderías disponibles.</p></div>
          ) : (
            <div className="farm-list" aria-label="Ganaderías disponibles">
              {ganaderias.map(ganaderia => (
                <button
                  className="farm-option"
                  type="button"
                  key={ganaderia.id}
                  aria-pressed={seleccionada === ganaderia.id}
                  onClick={() => setSeleccionada(ganaderia.id)}
                >
                  <span><strong>{ganaderia.nombre}</strong><small>{etiquetaRol(ganaderia.rol)}</small></span>
                  <span aria-hidden="true">{seleccionada === ganaderia.id ? '✓' : '→'}</span>
                </button>
              ))}
            </div>
          )}
          {seleccionada && (
            <p className="farm-selection" role="status">
              Ganadería seleccionada. El servidor volverá a verificar tu membresía antes de cada operación.
            </p>
          )}
        </div>
      )}
      {sesion && seleccionada && (() => {
        const ganaderia = ganaderias.find(item => item.id === seleccionada)
        return ganaderia
          ? <AnimalesPanel key={ganaderia.id} ganaderia={ganaderia}
              sesion={sesion} onSesionInvalida={onSesionInvalida} />
          : null
      })()}
    </section>
  )
}
