import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorSanidad, listarVacunaciones, registrarVacunacion, type SolicitudVacunacion, type Vacunacion } from './vacunacionesApi'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'
import {
  guardarEventoSanitarioPendienteAutorizado,
  listarEventosSanitariosPendientesAutorizados,
} from '../../offline/registroSanidadOffline'
import type { EventoSanitarioPendienteLocal } from '../../offline/eventosSanitariosPendientesDb'

type Props = { usuarioId: string; ganaderiaId: string; animalId: string; accessToken: string; puedeEscribir: boolean; revisionLocal?: number; onPendientesLocalesChange?: () => void; onSesionInvalida: () => void }
const inicial = { nombreVacuna: '', fechaAplicacion: '', responsable: '', lote: '', observaciones: '', proximaFecha: '' }
const mensajes = {
  sesion: 'Tu sesión terminó. Inicia sesión para continuar.',
  permiso: 'No tienes permiso vigente para registrar vacunaciones.',
  seguridad: 'No se pudo validar la seguridad de la solicitud. Actualiza la página.',
  validacion: 'Revisa el nombre, las fechas y los datos enviados.',
  conflicto: 'El registro presenta un conflicto. Conserva los datos y solicita revisión.',
  conexion: 'No hay conexión. Conserva los datos y vuelve a intentarlo al recuperar la red.',
  temporal: 'No recibimos confirmación. Reintenta el mismo registro para evitar duplicados.',
}
export function VacunacionesPanel({ usuarioId, ganaderiaId, animalId, accessToken, puedeEscribir, revisionLocal, onPendientesLocalesChange, onSesionInvalida }: Props) {
  const [items, setItems] = useState<Vacunacion[]>([])
  const [estado, setEstado] = useState<'carga' | 'listo' | 'error'>('carga')
  const [formulario, setFormulario] = useState(inicial)
  const [pendiente, setPendiente] = useState<SolicitudVacunacion | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [bloqueado, setBloqueado] = useState(false)
  const [pendientesLocales, setPendientesLocales] = useState<EventoSanitarioPendienteLocal[]>([])
  const ocupado = useRef(false)
  const ambito = { usuarioId, ganaderiaId }
  useProteccionActualizacionPwa(`vacunacion-${animalId}`, 'FORMULARIO', enviando || Boolean(pendiente) || Object.values(formulario).some(Boolean))
  async function cargar() {
    setEstado('carga')
    try { setItems(await listarVacunaciones(ganaderiaId, animalId, accessToken)); setEstado('listo') }
    catch (e) { setEstado('error'); if (e instanceof ErrorSanidad && e.tipo === 'sesion') onSesionInvalida() }
  }
  async function cargarPendientes() {
    try {
      const resultado = await listarEventosSanitariosPendientesAutorizados(ambito)
      setPendientesLocales(resultado.operaciones.filter(item =>
        item.animalId === animalId && item.clase === 'VACUNACION'))
    } catch {
      setMensaje('No se pudieron consultar las vacunaciones pendientes de este dispositivo.')
    }
  }
  useEffect(() => { let vigente = true
    setEstado('carga')
    listarVacunaciones(ganaderiaId, animalId, accessToken).then(datos => {
      if (vigente) { setItems(datos); setEstado('listo') }
    }).catch(e => { if (vigente) { setEstado('error'); if (e instanceof ErrorSanidad && e.tipo === 'sesion') onSesionInvalida() } })
    return () => { vigente = false }
  }, [ganaderiaId, animalId, accessToken, onSesionInvalida, revisionLocal])
  useEffect(() => { void cargarPendientes() }, [usuarioId, ganaderiaId, animalId, revisionLocal])
  async function guardarLocal(solicitud: SolicitudVacunacion) {
    try {
      const resultado = await guardarEventoSanitarioPendienteAutorizado(
        ambito, animalId, solicitud,
      )
      if (!resultado.operacion) {
        setMensaje(resultado.autorizacion === 'SOLO_LECTURA'
          ? 'Tu acceso vigente es de solo lectura.'
          : 'Recupera una sesión y autorización offline válidas antes de guardar.')
        return false
      }
      setPendientesLocales(actuales => [
        ...actuales.filter(item => item.operationId !== resultado.operacion?.operationId),
        resultado.operacion!,
      ])
      setPendiente(null)
      setFormulario(inicial)
      setMensaje('Vacunación guardada en este dispositivo. Pendiente de sincronización.')
      onPendientesLocalesChange?.()
      return true
    } catch {
      setMensaje('No se pudo guardar en este dispositivo. Conservamos el formulario para reintentar.')
      return false
    }
  }
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
      if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        await guardarLocal(solicitud)
        return
      }
      await registrarVacunacion(ganaderiaId, animalId, accessToken, solicitud)
      setPendiente(null); setFormulario(inicial); setMensaje('Vacunación registrada correctamente.'); await cargar()
    } catch (e) {
      const tipo = e instanceof ErrorSanidad ? e.tipo : 'temporal'
      if (tipo === 'conexion') await guardarLocal(solicitud)
      else {
        setMensaje(mensajes[tipo])
        if (tipo === 'validacion') setPendiente(null)
        if (tipo === 'permiso' || tipo === 'conflicto' || tipo === 'seguridad') setBloqueado(true)
        if (tipo === 'sesion') onSesionInvalida()
      }
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
    {pendientesLocales.length > 0 && <section aria-label="Vacunaciones pendientes">
      <h5>Vacunaciones pendientes</h5>
      <p>Se conservan en este dispositivo y todavía no forman parte del historial confirmado.</p>
      <ul>{pendientesLocales.map(item => {
        const datos = item.payload as SolicitudVacunacion
        return <li key={item.operationId}>
          <strong>{datos.nombreVacuna}</strong> · {datos.fechaAplicacion}
          <small>{estadoVacunacionLocal(item)}</small>
        </li>
      })}</ul>
    </section>}
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

function estadoVacunacionLocal(item: EventoSanitarioPendienteLocal) {
  if (item.estado === 'ERROR_REINTENTABLE') return 'Error temporal · pendiente de reintento'
  if (item.estado === 'REQUIERE_SESION') return 'Requiere una sesión vigente'
  if (item.estado === 'RECHAZADO') return item.ultimoError === 'AUTORIZACION'
    ? 'Permiso no vigente' : 'Datos rechazados · requiere revisión'
  if (item.estado === 'CONFLICTO') return 'Conflicto · requiere revisión'
  if (item.estado === 'SINCRONIZANDO') return 'Sincronización en curso'
  return 'Pendiente de sincronización'
}
