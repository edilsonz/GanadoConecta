import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorSanidad, listarVacunaciones, registrarVacunacion, type SolicitudVacunacion, type Vacunacion } from './vacunacionesApi'
import { useProteccionActualizacionPwa } from './pwaActividad'

type Props = { ganaderiaId: string; animalId: string; accessToken: string; puedeEscribir: boolean; onSesionInvalida: () => void }
const inicial = { nombreVacuna: '', fechaAplicacion: '', responsable: '', lote: '', observaciones: '', proximaFecha: '' }
const mensajes = {
  sesion: 'Tu sesión terminó. Inicia sesión para continuar.',
  permiso: 'No tienes permiso vigente para registrar vacunaciones.',
  seguridad: 'No se pudo validar la seguridad de la solicitud. Actualiza la página.',
  validacion: 'Revisa el nombre, las fechas y los datos enviados.',
  conflicto: 'El registro presenta un conflicto. Conserva los datos y solicita revisión.',
  temporal: 'No recibimos confirmación. Reintenta el mismo registro para evitar duplicados.',
}
export function VacunacionesPanel({ ganaderiaId, animalId, accessToken, puedeEscribir, onSesionInvalida }: Props) {
  const [items, setItems] = useState<Vacunacion[]>([])
  const [estado, setEstado] = useState<'carga' | 'listo' | 'error'>('carga')
  const [formulario, setFormulario] = useState(inicial)
  const [pendiente, setPendiente] = useState<SolicitudVacunacion | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [bloqueado, setBloqueado] = useState(false)
  const ocupado = useRef(false)
  useProteccionActualizacionPwa(`vacunacion-${animalId}`, 'FORMULARIO', enviando || Boolean(pendiente) || Object.values(formulario).some(Boolean))
  async function cargar() {
    setEstado('carga')
    try { setItems(await listarVacunaciones(ganaderiaId, animalId, accessToken)); setEstado('listo') }
    catch (e) { setEstado('error'); if (e instanceof ErrorSanidad && e.tipo === 'sesion') onSesionInvalida() }
  }
  useEffect(() => { let vigente = true
    setEstado('carga')
    listarVacunaciones(ganaderiaId, animalId, accessToken).then(datos => {
      if (vigente) { setItems(datos); setEstado('listo') }
    }).catch(e => { if (vigente) { setEstado('error'); if (e instanceof ErrorSanidad && e.tipo === 'sesion') onSesionInvalida() } })
    return () => { vigente = false }
  }, [ganaderiaId, animalId, accessToken, onSesionInvalida])
  async function guardar(event: FormEvent) {
    event.preventDefault()
    if (ocupado.current || bloqueado || !puedeEscribir) return
    if (!formulario.nombreVacuna.trim() || !formulario.fechaAplicacion
        || (formulario.proximaFecha && formulario.proximaFecha < formulario.fechaAplicacion)) {
      setMensaje(mensajes.validacion); return
    }
    const solicitud = pendiente ?? {
      ...formulario, proximaFecha: formulario.proximaFecha || null,
      eventId: crypto.randomUUID(), operationId: crypto.randomUUID(), schemaVersion: 1 as const,
    }
    ocupado.current = true; setEnviando(true); setPendiente(solicitud); setMensaje('')
    try {
      await registrarVacunacion(ganaderiaId, animalId, accessToken, solicitud)
      setPendiente(null); setFormulario(inicial); setMensaje('Vacunación registrada correctamente.'); await cargar()
    } catch (e) {
      const tipo = e instanceof ErrorSanidad ? e.tipo : 'temporal'
      setMensaje(mensajes[tipo])
      if (tipo === 'validacion') setPendiente(null)
      if (tipo === 'permiso' || tipo === 'conflicto' || tipo === 'seguridad') setBloqueado(true)
      if (tipo === 'sesion') onSesionInvalida()
    } finally { ocupado.current = false; setEnviando(false) }
  }
  return <section className="vaccinations-panel" aria-label="Vacunaciones">
    <div className="animals-list-heading"><h4>Vacunaciones</h4><button type="button" className="button button-outline" onClick={() => void cargar()} disabled={estado === 'carga'}>Actualizar vacunaciones</button></div>
    {estado === 'carga' && <p role="status">Consultando vacunaciones…</p>}
    {estado === 'error' && <p role="alert">No se pudieron cargar las vacunaciones. Los datos visibles podrían estar desactualizados.</p>}
    {estado === 'listo' && items.length === 0 && <p>Aún no hay vacunaciones registradas.</p>}
    <ul>{items.map(v => <li key={v.eventId}><strong>{v.nombreVacuna}</strong> · {v.fechaAplicacion}
      {v.responsable && <p>Responsable: {v.responsable}</p>}{v.lote && <p>Lote: {v.lote}</p>}
      {v.observaciones && <p>{v.observaciones}</p>}{v.proximaFecha && <p>Próxima fecha prevista: {v.proximaFecha} (indicada por el usuario)</p>}
    </li>)}</ul>
    {mensaje && <p role="status">{mensaje}</p>}
    {puedeEscribir && <form onSubmit={guardar} className="animal-form">
      <p>Registra únicamente una aplicación realizada. La próxima fecha es opcional y no se calcula automáticamente.</p>
      <fieldset disabled={enviando || Boolean(pendiente) || bloqueado}>
        <div className="animal-fields-grid">{([
          ['nombreVacuna', 'Nombre de vacuna *', 'text', 150], ['fechaAplicacion', 'Fecha de aplicación *', 'date', undefined],
          ['responsable', 'Responsable o veterinario', 'text', 150], ['lote', 'Número de lote', 'text', 100],
          ['proximaFecha', 'Próxima fecha prevista', 'date', undefined],
        ] as const).map(([campo, etiqueta, tipo, max]) => <div className="form-field" key={campo}>
          <label htmlFor={`vacuna-${campo}`}>{etiqueta}</label><input id={`vacuna-${campo}`} type={tipo} maxLength={max}
            required={campo === 'nombreVacuna' || campo === 'fechaAplicacion'} value={formulario[campo]}
            onChange={e => setFormulario(f => ({ ...f, [campo]: e.target.value }))} />
        </div>)}</div>
        <div className="form-field"><label htmlFor="vacuna-observaciones">Observaciones de vacunación</label><textarea id="vacuna-observaciones" maxLength={2000} value={formulario.observaciones} onChange={e => setFormulario(f => ({ ...f, observaciones: e.target.value }))} /></div>
      </fieldset>
      <button className="button button-primary" disabled={enviando || bloqueado}>{enviando ? 'Guardando…' : pendiente ? 'Reintentar misma vacunación' : 'Guardar vacunación'}</button>
    </form>}
  </section>
}
