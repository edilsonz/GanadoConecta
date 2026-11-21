import { useRef, useState } from 'react'
import { ErrorSolicitudPrivada, solicitudPrivada } from './solicitudPrivada'

type SesionProps = { token: string; onSesionInvalida: () => void }
type Rol = 'EDITOR' | 'LECTOR'
type Resumen = {
  integrantes: { usuarioId: string; correo: string; rol: Rol | 'PROPIETARIO'; estado: string }[]
  invitaciones: { id: string; correo: string; rol: Rol; estado: string; venceEn: string }[]
}

function mensajeError(error: unknown, expiro: () => void) {
  if (error instanceof ErrorSolicitudPrivada) {
    if (error.estado === 401) { expiro(); return 'Tu sesión terminó.' }
    if (error.estado === 403) return 'No tienes autorización para esta acción.'
    if (error.estado === 409) return 'La invitación o membresía ya no está disponible.'
    if (error.estado === 400 || error.estado === 422) return 'Revisa los datos ingresados.'
  }
  return 'No se pudo confirmar la operación. Actualiza antes de intentarlo nuevamente.'
}

export function AceptarInvitacion({ token, onSesionInvalida, actualizar }: SesionProps & { actualizar: () => void }) {
  const [codigo, setCodigo] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  return <details>
    <summary>Aceptar invitación familiar</summary>
    <form className="animal-form" onSubmit={async evento => {
      evento.preventDefault()
      if (ocupado) return
      setOcupado(true)
      setMensaje('')
      try {
        await solicitudPrivada('/api/v1/invitaciones/aceptar', token, 'POST', { codigo: codigo.trim() })
        setCodigo('')
        setMensaje('Invitación aceptada.')
        actualizar()
      } catch (error) { setMensaje(mensajeError(error, onSesionInvalida)) }
      finally { setOcupado(false) }
    }}>
      <label className="form-field">Código de invitación
        <input required maxLength={43} autoComplete="off" value={codigo}
          onChange={e => setCodigo(e.target.value)} />
      </label>
      <button className="button button-outline" disabled={ocupado}>Aceptar invitación</button>
      {mensaje && <p role="status">{mensaje}</p>}
    </form>
  </details>
}

export function AccesoFamiliarPanel({ ganaderiaId, token, onSesionInvalida }: SesionProps & { ganaderiaId: string }) {
  const [resumen, setResumen] = useState<Resumen>()
  const [correo, setCorreo] = useState('')
  const [rol, setRol] = useState<Rol>('LECTOR')
  const [codigo, setCodigo] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const enCurso = useRef(false)
  const rutaGanaderia = `/api/v1/ganaderias/${encodeURIComponent(ganaderiaId)}`
  const ruta = `${rutaGanaderia}/familia`
  async function ejecutar(trabajo?: () => Promise<void>) {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true)
    setMensaje('')
    try {
      await trabajo?.()
      setResumen(await solicitudPrivada<Resumen>(ruta, token))
    } catch (error) {
      setResumen(undefined)
      setMensaje(mensajeError(error, onSesionInvalida))
    } finally { enCurso.current = false; setOcupado(false) }
  }

  return <details onToggle={e => {
    if (e.currentTarget.open) void ejecutar()
    else { setResumen(undefined); setCodigo(''); setMensaje('') }
  }}>
    <summary>Acceso familiar</summary>
    <section className="animal-form">
      <form onSubmit={e => {
        e.preventDefault()
        setCodigo('')
        void ejecutar(async () => {
          const emitida = await solicitudPrivada<{ codigo: string }>(`${ruta}/invitaciones`, token, 'POST', { correo, rol })
          setCodigo(emitida.codigo)
          setCorreo('')
        })
      }}>
        <div className="animal-fields-grid">
          <label className="form-field">Correo del familiar<input required type="email" maxLength={254}
            value={correo} onChange={e => setCorreo(e.target.value)} /></label>
          <label className="form-field">Permiso de la invitación<select value={rol} onChange={e => setRol(e.target.value as Rol)}>
            <option value="LECTOR">Lector</option><option value="EDITOR">Editor</option>
          </select></label>
        </div>
        <button className="button button-primary" disabled={ocupado}>Generar invitación</button>
      </form>
      {codigo && <label className="form-field">Copia este código y compártelo directamente con tu familiar.
        <input readOnly value={codigo} onFocus={e => e.currentTarget.select()} />
      </label>}
      {mensaje && <p role="status">{mensaje}</p>}
      <button className="button button-outline" disabled={ocupado} onClick={() => void ejecutar()}>Actualizar integrantes</button>
      {resumen && <>
        <h5>Integrantes</h5>
        {resumen.integrantes.map(i => <div className="form-field" key={i.usuarioId}>
          <p style={{ overflowWrap: 'anywhere' }}>{i.correo} · {i.rol} · {i.estado}</p>
          {i.rol !== 'PROPIETARIO' && i.estado === 'ACTIVA' && <>
            <label>Permiso de {i.correo}<select disabled={ocupado} value={i.rol} onChange={e => {
              const nuevoRol = e.target.value
              void ejecutar(() => solicitudPrivada(`${ruta}/integrantes/${i.usuarioId}`, token, 'PUT', { rol: nuevoRol }))
            }}><option value="EDITOR">Editor</option><option value="LECTOR">Lector</option></select></label>
            <button className="button button-outline" disabled={ocupado} onClick={() => {
              if (window.confirm(`¿Revocar el acceso de ${i.correo}?`)) {
                void ejecutar(() => solicitudPrivada(`${rutaGanaderia}/membresias/${i.usuarioId}`, token, 'DELETE'))
              }
            }}>Revocar acceso</button>
          </>}
        </div>)}
        <h5>Invitaciones</h5>
        {resumen.invitaciones.length === 0 && <p>Aún no hay invitaciones.</p>}
        {resumen.invitaciones.map(i => <div className="form-field" key={i.id}>
          <p style={{ overflowWrap: 'anywhere' }}>{i.correo} · {i.rol} · {i.estado} · Vence {new Date(i.venceEn).toLocaleString('es-PE', { timeZone: 'America/Lima' })}</p>
          {i.estado === 'PENDIENTE' && <button className="button button-outline" disabled={ocupado}
            onClick={() => void ejecutar(() => solicitudPrivada(`${ruta}/invitaciones/${i.id}`, token, 'DELETE'))}>Cancelar invitación</button>}
        </div>)}
      </>}
    </section>
  </details>
}
