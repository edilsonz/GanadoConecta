import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { Animal } from './animalesApi'
import { completarActividad, listarAgenda, programarActividad, tiposActividad,
  type ActividadSanitaria, type ConfirmacionCampana, type SolicitudActividad, type TipoActividad } from './agendaSanitariaApi'
import { ErrorSanidad } from './vacunacionesApi'
import { useProteccionActualizacionPwa } from './pwaActividad'

type Props = {
  ganaderiaId: string; animales: Animal[]; accessToken: string; puedeEscribir: boolean
  animalInicialId?: string; onSesionInvalida: () => void; onAtencionesConfirmadas?: () => void
}
const hoyLima = () => new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date())
const estados = { PROXIMA: 'Programada', PENDIENTE: 'Pendiente', VENCIDA: 'Vencida', COMPLETADA: 'Realizada' }
const esCampana = (tipo: TipoActividad) => tipo.startsWith('CAMPANA_')
const marcarSeleccion = (ids: string[], id: string, agregar: boolean) =>
  agregar ? [...ids, id] : ids.filter(actual => actual !== id)

export function AgendaSanitariaPanel(p: Props) {
  const [items, setItems] = useState<ActividadSanitaria[]>([])
  const [estado, setEstado] = useState<'carga'|'listo'|'error'>('carga')
  const [filtro, setFiltro] = useState('')
  const [revision, setRevision] = useState(0)
  const [form, setForm] = useState({ animalId: '', tipo: 'VACUNACION' as TipoActividad,
    fechaPrevista: '', descripcion: '', observaciones: '' })
  const [colectiva, setColectiva] = useState(false)
  const [participantes, setParticipantes] = useState<string[]>([])
  const [pendiente, setPendiente] = useState<SolicitudActividad>()
  const [campana, setCampana] = useState<string>()
  const [confirmacion, setConfirmacion] = useState<ConfirmacionCampana>({ atendidos: [], fechaAtencion: '' })
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const ocupado = useRef(false)
  const grupo = colectiva || esCampana(form.tipo)

  useEffect(() => {
    if (p.animalInicialId) {
      setFiltro(p.animalInicialId)
      setForm(f => ({ ...f, animalId: p.animalInicialId! }))
    }
  }, [p.animalInicialId])
  useEffect(() => {
    let vigente = true
    setEstado('carga')
    void listarAgenda(p.ganaderiaId, p.accessToken, filtro ? { animalId: filtro } : {}).then(datos => {
      if (vigente) { setItems(datos); setEstado('listo') }
    }).catch(error => {
      if (!vigente) return
      setEstado('error')
      if (error instanceof ErrorSanidad && error.tipo === 'sesion') p.onSesionInvalida()
    })
    return () => { vigente = false }
  }, [p.ganaderiaId, p.accessToken, filtro, revision, p.onSesionInvalida])
  useProteccionActualizacionPwa(`agenda-${p.ganaderiaId}`, 'FORMULARIO',
    enviando || Boolean(pendiente || campana || form.fechaPrevista || form.descripcion || form.observaciones || participantes.length))

  function registrarError(error: unknown) {
    const tipo = error instanceof ErrorSanidad ? error.tipo : 'temporal'
    if (tipo === 'sesion') p.onSesionInvalida()
    setMensaje({
      sesion: 'Tu sesión terminó.', permiso: 'No tienes permiso para modificar el calendario.',
      validacion: 'Revisa los animales, las fechas y los datos de la actividad.',
      conflicto: 'La actividad ya cambió. Consulta el calendario antes de volver a intentar.',
      seguridad: 'No se pudo validar la solicitud.', conexion: 'No se confirmó la operación. Puedes reintentar.',
      temporal: 'No se confirmó la operación. Puedes reintentar.',
    }[tipo])
    return tipo
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    if (ocupado.current) return
    if (!(grupo ? participantes.length : form.animalId) || !form.fechaPrevista || !form.descripcion.trim()) {
      setMensaje('Selecciona animales y completa la fecha y descripción.')
      return
    }
    const { animalId, ...datos } = form
    const solicitud = pendiente ?? { ...datos, actividadId: crypto.randomUUID(),
      ...(grupo ? { participantes } : { animalId }) }
    ocupado.current = true
    setEnviando(true)
    setPendiente(solicitud)
    try {
      await programarActividad(p.ganaderiaId, p.accessToken, solicitud)
      setPendiente(undefined)
      setParticipantes([])
      setForm(f => ({ ...f, fechaPrevista: '', descripcion: '', observaciones: '' }))
      setMensaje('Actividad programada.')
      setRevision(r => r + 1)
    } catch (error) {
      if (registrarError(error) === 'validacion') setPendiente(undefined)
    } finally { ocupado.current = false; setEnviando(false) }
  }

  async function completar(id: string, datos?: ConfirmacionCampana) {
    if (ocupado.current) return
    ocupado.current = true
    setEnviando(true)
    try {
      await completarActividad(p.ganaderiaId, id, p.accessToken, datos)
      if (datos) p.onAtencionesConfirmadas?.()
      setCampana(undefined)
      setMensaje(datos ? 'Campaña realizada. Se registraron las atenciones confirmadas.'
        : 'Actividad marcada como completada. No se creó un evento clínico.')
      setRevision(r => r + 1)
    } catch (error) {
      if (registrarError(error) === 'conflicto') {
        setCampana(undefined)
        setRevision(r => r + 1)
      }
    } finally { ocupado.current = false; setEnviando(false) }
  }

  const nombre = (id: string) => {
    const animal = p.animales.find(a => a.animalId === id)
    return animal?.nombre || animal?.areteFrente || (animal ? 'Animal sin nombre' : id)
  }
  const avisos = items.filter(a => a.estadoVisible === 'PENDIENTE' || a.estadoVisible === 'VENCIDA')
  return <section className="health-agenda" id="agenda-sanitaria" aria-labelledby="agenda-title">
    <div className="animals-list-heading"><div><h3 id="agenda-title">Calendario y campañas</h3>
      <p>Actividades sanitarias, reproductivas y productivas.</p></div>
      <label className="form-field">Filtrar por animal<select value={filtro} onChange={e => setFiltro(e.target.value)}>
        <option value="">Todos</option>{p.animales.map(a => <option key={a.animalId} value={a.animalId}>{nombre(a.animalId)}</option>)}
      </select></label>
    </div>
    {estado === 'carga' && <p role="status">Consultando calendario…</p>}
    {estado === 'error' && <p role="alert">No se pudo cargar el calendario.</p>}
    {estado === 'listo' && !items.length && <p>No hay actividades con este filtro.</p>}
    {estado === 'listo' && avisos.length > 0 && <aside aria-label="Recordatorios internos">
      <strong>Recordatorios internos</strong>
      <ul>{avisos.map(a => <li key={a.actividadId}>{estados[a.estadoVisible]} · {a.fechaPrevista} · {a.descripcion}</li>)}</ul>
    </aside>}
    {estado === 'listo' && <div className="agenda-list">{items.map(a => <article key={a.actividadId}
      className={`agenda-item state-${a.estadoVisible.toLowerCase()}`}><div>
      <span className="access-badge">{estados[a.estadoVisible]}</span> <strong>{tiposActividad[a.tipo]} · {a.fechaPrevista}</strong>
      <p>{a.animalId ? nombre(a.animalId) : `${a.participantes?.length ?? 0} participantes`} · {a.descripcion}</p>
      {a.observaciones && <p>{a.observaciones}</p>}
      {a.fechaAtencion && <p>Fecha de atención {a.fechaAtencion}. Atendidos: {a.atendidos?.map(nombre).join(', ')}.</p>}
      {campana === a.actividadId && <form onSubmit={e => { e.preventDefault(); void completar(a.actividadId, confirmacion) }}>
        <fieldset disabled={enviando}><legend>Atenciones realmente realizadas</legend>
          {a.participantes?.map(id => <label className="checkbox-field" key={id}><input type="checkbox"
            checked={confirmacion.atendidos.includes(id)} onChange={e => {
              const seleccionado = e.target.checked
              setConfirmacion(c => ({ ...c, atendidos: marcarSeleccion(c.atendidos, id, seleccionado) }))
            }} />{nombre(id)}</label>)}
          <label className="form-field">Fecha de atención<input required type="date" max={hoyLima()} value={confirmacion.fechaAtencion}
            onChange={e => setConfirmacion({ ...confirmacion, fechaAtencion: e.target.value })} /></label>
          {a.tipo === 'CAMPANA_VACUNACION' && <label className="form-field">Nombre de la vacuna<input required maxLength={150}
            value={confirmacion.nombreVacuna ?? ''} onChange={e => setConfirmacion({ ...confirmacion, nombreVacuna: e.target.value })} /></label>}
          <button className="button button-primary" disabled={!confirmacion.atendidos.length}>Confirmar atenciones</button>
          <button className="button button-outline" type="button" onClick={() => setCampana(undefined)}>Cancelar confirmación</button>
        </fieldset>
      </form>}
      </div>{p.puedeEscribir && a.estado === 'PENDIENTE' && campana !== a.actividadId &&
        <button className="button button-outline" type="button" disabled={enviando} onClick={() => {
          if (esCampana(a.tipo)) {
            setConfirmacion({ atendidos: [], fechaAtencion: hoyLima() })
            setCampana(a.actividadId)
          } else void completar(a.actividadId)
        }}>{esCampana(a.tipo) ? 'Seleccionar atendidos' : 'Marcar realizada'}</button>}
    </article>)}</div>}
    {mensaje && <p role="status">{mensaje}</p>}
    {p.puedeEscribir && <form className="animal-form" onSubmit={guardar}>
      <h4>Programar actividad</h4>
      <fieldset disabled={enviando || Boolean(pendiente)}>
        <div className="animal-fields-grid">
          <label className="form-field">Tipo *<select value={form.tipo}
            onChange={e => setForm({ ...form, tipo: e.target.value as TipoActividad })}>
            {Object.entries(tiposActividad).map(([tipo, etiqueta]) => <option key={tipo} value={tipo}>{etiqueta}</option>)}
          </select></label>
          <label className="form-field">Fecha prevista *<input type="date" min={hoyLima()} value={form.fechaPrevista}
            onChange={e => setForm({ ...form, fechaPrevista: e.target.value })} /></label>
        </div>
        {!esCampana(form.tipo) && <label className="checkbox-field"><input type="checkbox" checked={colectiva}
          onChange={e => setColectiva(e.target.checked)} />Actividad colectiva</label>}
        {grupo ? <fieldset><legend>Animales participantes</legend>{p.animales.map(a => <label className="checkbox-field" key={a.animalId}>
          <input type="checkbox" checked={participantes.includes(a.animalId)} disabled={participantes.length >= 100 && !participantes.includes(a.animalId)}
            onChange={e => setParticipantes(marcarSeleccion(participantes, a.animalId, e.target.checked))} />{nombre(a.animalId)}</label>)}</fieldset>
          : <label className="form-field">Animal *<select value={form.animalId} onChange={e => setForm({ ...form, animalId: e.target.value })}>
            <option value="">Selecciona</option>{p.animales.map(a => <option key={a.animalId} value={a.animalId}>{nombre(a.animalId)}</option>)}
          </select></label>}
        <label className="form-field">Descripción *<textarea maxLength={1000} value={form.descripcion}
          onChange={e => setForm({ ...form, descripcion: e.target.value })} /></label>
        <label className="form-field">Observaciones<textarea maxLength={2000} value={form.observaciones}
          onChange={e => setForm({ ...form, observaciones: e.target.value })} /></label>
      </fieldset>
      <button className="button button-primary" disabled={enviando}>{pendiente ? 'Reintentar misma actividad' : 'Programar actividad'}</button>
    </form>}
  </section>
}
