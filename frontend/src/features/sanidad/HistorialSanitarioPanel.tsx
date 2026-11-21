import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorSanidad } from './vacunacionesApi'
import {
  listarHistorialSanitario,
  registrarEventoSanitario,
  type EventoSanitario,
  type SolicitudEventoSanitario,
  type TipoEventoSanitario,
} from './historialSanitarioApi'
import {
  guardarEventoSanitarioPendienteAutorizado,
  listarEventosSanitariosPendientesAutorizados,
} from '../../offline/registroSanidadOffline'
import type { EventoSanitarioPendienteLocal } from '../../offline/eventosSanitariosPendientesDb'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'

type Props = {
  usuarioId: string
  ganaderiaId: string
  animalId: string
  accessToken: string
  puedeEscribir: boolean
  revisionLocal?: number
  onPendientesLocalesChange?: () => void
  onSesionInvalida: () => void
}

const etiquetas: Record<TipoEventoSanitario, string> = {
  VACUNACION: 'Vacunación',
  DESPARASITACION: 'Desparasitación',
  TRATAMIENTO_SANITARIO: 'Tratamiento sanitario',
  REVISION_VETERINARIA: 'Revisión veterinaria',
  BANO_ANTIPARASITARIO: 'Baño antiparasitario',
}
const inicial = {
  tipo: 'DESPARASITACION' as SolicitudEventoSanitario['tipo'],
  fecha: '',
  descripcion: '',
  responsable: '',
  producto: '',
  indicaciones: '',
  observaciones: '',
}

export function HistorialSanitarioPanel(p: Props) {
  const [items, setItems] = useState<EventoSanitario[]>([])
  const [estado, setEstado] = useState<'carga' | 'listo' | 'error'>('carga')
  const [filtro, setFiltro] = useState<'' | TipoEventoSanitario>('')
  const [form, setForm] = useState(inicial)
  const [pendiente, setPendiente] = useState<SolicitudEventoSanitario | null>(null)
  const [pendientesLocales, setPendientesLocales] = useState<EventoSanitarioPendienteLocal[]>([])
  const [enviando, setEnviando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const ocupado = useRef(false)
  const ambito = { usuarioId: p.usuarioId, ganaderiaId: p.ganaderiaId }

  useProteccionActualizacionPwa(
    `evento-sanitario-${p.animalId}`,
    'FORMULARIO',
    enviando || Boolean(pendiente)
      || Object.values(form).some(valor => valor !== '' && valor !== 'DESPARASITACION'),
  )

  async function cargar(tipo = filtro) {
    setEstado('carga')
    try {
      setItems(await listarHistorialSanitario(
        p.ganaderiaId,
        p.animalId,
        p.accessToken,
        tipo || undefined,
      ))
      setEstado('listo')
    } catch (error) {
      setEstado('error')
      if (error instanceof ErrorSanidad && error.tipo === 'sesion') p.onSesionInvalida()
    }
  }

  async function cargarPendientesLocales() {
    try {
      const resultado = await listarEventosSanitariosPendientesAutorizados(ambito)
      setPendientesLocales(resultado.operaciones.filter(item =>
        item.animalId === p.animalId && item.clase === 'EVENTO_SANITARIO'))
    } catch {
      setMensaje('No se pudieron consultar los eventos pendientes de este dispositivo.')
    }
  }

  useEffect(() => { void cargar(filtro) }, [p.ganaderiaId, p.animalId, p.accessToken, filtro, p.revisionLocal])
  useEffect(() => { void cargarPendientesLocales() }, [p.usuarioId, p.ganaderiaId, p.animalId, p.revisionLocal])

  async function guardarLocal(solicitud: SolicitudEventoSanitario): Promise<boolean> {
    try {
      const resultado = await guardarEventoSanitarioPendienteAutorizado(
        ambito,
        p.animalId,
        solicitud,
      )
      if (!resultado.operacion) {
        setMensaje(mensajeAutorizacion(resultado.autorizacion))
        return false
      }
      setPendientesLocales(actuales => {
        const otros = actuales.filter(item => item.operationId !== resultado.operacion?.operationId)
        return [...otros, resultado.operacion!]
      })
      setPendiente(null)
      setForm(inicial)
      setMensaje('Evento guardado en este dispositivo. Pendiente de sincronización.')
      p.onPendientesLocalesChange?.()
      return true
    } catch {
      setMensaje('No se pudo guardar en este dispositivo. Conservamos el formulario para reintentar.')
      return false
    }
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    if (ocupado.current || !p.puedeEscribir) return
    if (!form.fecha || !form.descripcion.trim()) {
      setMensaje('Indica la fecha y una descripción del hecho realizado.')
      return
    }
    const solicitud = pendiente ?? {
      ...form,
      operationId: crypto.randomUUID(),
      eventId: crypto.randomUUID(),
      schemaVersion: 1 as const,
    }
    ocupado.current = true
    setEnviando(true)
    setPendiente(solicitud)
    setMensaje('')
    try {
      if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        await guardarLocal(solicitud)
        return
      }
      await registrarEventoSanitario(p.ganaderiaId, p.animalId, p.accessToken, solicitud)
      setPendiente(null)
      setForm(inicial)
      setMensaje('Evento sanitario registrado correctamente.')
      await cargar()
    } catch (error) {
      const tipo = error instanceof ErrorSanidad ? error.tipo : 'temporal'
      if (tipo === 'conexion') {
        await guardarLocal(solicitud)
      } else {
        setMensaje(tipo === 'sesion'
          ? 'Tu sesión terminó.'
          : tipo === 'permiso'
            ? 'No tienes permiso vigente para registrar este evento.'
            : tipo === 'conflicto'
              ? 'Existe un conflicto; el registro no se sobrescribió.'
              : tipo === 'validacion'
                ? 'Revisa los datos del evento sanitario.'
                : 'No recibimos confirmación. Puedes reintentar el mismo registro.')
        if (tipo === 'validacion') setPendiente(null)
        if (tipo === 'sesion') p.onSesionInvalida()
      }
    } finally {
      ocupado.current = false
      setEnviando(false)
    }
  }

  return <section className="health-history" aria-label="Historial sanitario">
    <div className="animals-list-heading">
      <h4>Historial sanitario</h4>
      <label>Filtrar por tipo <select value={filtro}
        onChange={evento => setFiltro(evento.target.value as typeof filtro)}>
        <option value="">Todos</option>
        {Object.entries(etiquetas).map(([valor, etiqueta]) =>
          <option key={valor} value={valor}>{etiqueta}</option>)}
      </select></label>
    </div>
    {estado === 'carga' && <p role="status">Consultando historial sanitario…</p>}
    {estado === 'error' && <p role="alert">No se pudo cargar el historial sanitario.</p>}
    {estado === 'listo' && items.length === 0
      && <p>Aún no hay eventos sanitarios con este filtro.</p>}
    <ol>{items.map(item => <li key={item.eventId}>
      <strong>{etiquetas[item.tipo]} · {item.fecha}</strong>
      <p>{item.titulo}</p>
      {item.responsable && <small>Responsable: {item.responsable}</small>}
      {item.producto && <p>Producto registrado: {item.producto}</p>}
      {item.indicaciones && <p>Indicaciones registradas: {item.indicaciones}</p>}
      {item.observaciones && <p>{item.observaciones}</p>}
    </li>)}</ol>

    {pendientesLocales.length > 0 && <section aria-label="Eventos sanitarios pendientes">
      <h5>Eventos sanitarios pendientes</h5>
      <p>Se conservan en este dispositivo y todavía no forman parte del historial confirmado.</p>
      <ol>{pendientesLocales.map(item => <li key={item.operationId}>
        <strong>{etiquetas[(item.payload as SolicitudEventoSanitario).tipo]} · {(item.payload as SolicitudEventoSanitario).fecha}</strong>
        <p>{(item.payload as SolicitudEventoSanitario).descripcion}</p>
        <small>{estadoLocal(item)}</small>
      </li>)}</ol>
    </section>}

    {mensaje && <p role="status">{mensaje}</p>}
    {p.puedeEscribir && <form className="animal-form" onSubmit={guardar}>
      <p>Registra un hecho ya realizado. El sistema no genera diagnósticos ni indicaciones.</p>
      <fieldset disabled={enviando || Boolean(pendiente)}>
        <div className="animal-fields-grid">
          <div className="form-field">
            <label htmlFor="evento-tipo">Tipo *</label>
            <select id="evento-tipo" value={form.tipo}
              onChange={evento => setForm(actual => ({
                ...actual,
                tipo: evento.target.value as typeof actual.tipo,
              }))}>
              {Object.entries(etiquetas).filter(([valor]) => valor !== 'VACUNACION' && valor !== 'BANO_ANTIPARASITARIO')
                .map(([valor, etiqueta]) =>
                  <option key={valor} value={valor}>{etiqueta}</option>)}
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="evento-fecha">Fecha *</label>
            <input id="evento-fecha" type="date" value={form.fecha}
              onChange={evento => setForm(actual => ({
                ...actual,
                fecha: evento.target.value,
              }))} />
          </div>
          {([['responsable', 'Responsable', 150], ['producto', 'Producto utilizado', 150]] as const)
            .map(([clave, etiqueta, maximo]) => <div className="form-field" key={clave}>
              <label htmlFor={`evento-${clave}`}>{etiqueta}</label>
              <input id={`evento-${clave}`} maxLength={maximo} value={form[clave]}
                onChange={evento => setForm(actual => ({
                  ...actual,
                  [clave]: evento.target.value,
                }))} />
            </div>)}
        </div>
        <div className="form-field">
          <label htmlFor="evento-descripcion">Descripción del hecho *</label>
          <textarea id="evento-descripcion" maxLength={1000} value={form.descripcion}
            onChange={evento => setForm(actual => ({
              ...actual,
              descripcion: evento.target.value,
            }))} />
        </div>
        <div className="form-field">
          <label htmlFor="evento-indicaciones">Indicaciones registradas por el responsable</label>
          <textarea id="evento-indicaciones" maxLength={1000} value={form.indicaciones}
            onChange={evento => setForm(actual => ({
              ...actual,
              indicaciones: evento.target.value,
            }))} />
        </div>
        <div className="form-field">
          <label htmlFor="evento-observaciones">Observaciones</label>
          <textarea id="evento-observaciones" maxLength={2000} value={form.observaciones}
            onChange={evento => setForm(actual => ({
              ...actual,
              observaciones: evento.target.value,
            }))} />
        </div>
      </fieldset>
      <button className="button button-primary" disabled={enviando}>
        {enviando ? 'Guardando…' : pendiente ? 'Reintentar mismo evento' : 'Guardar evento sanitario'}
      </button>
    </form>}
  </section>
}

function mensajeAutorizacion(estado: string): string {
  if (estado === 'SOLO_LECTURA') return 'Tu acceso vigente es de solo lectura.'
  if (estado === 'REQUIERE_SESION') return 'Recupera tu sesión antes de guardar sin conexión.'
  if (estado === 'BLOQUEADA') return 'El acceso local a esta ganadería está bloqueado.'
  return 'Prepara nuevamente el trabajo sin conexión antes de guardar.'
}

function estadoLocal(item: EventoSanitarioPendienteLocal): string {
  if (item.estado === 'ERROR_REINTENTABLE') return 'Error temporal · pendiente de reintento'
  if (item.estado === 'REQUIERE_SESION') return 'Requiere una sesión vigente'
  if (item.estado === 'RECHAZADO') return item.ultimoError === 'AUTORIZACION'
    ? 'Permiso no vigente' : 'Datos rechazados · requiere revisión'
  if (item.estado === 'CONFLICTO') return 'Conflicto · requiere revisión'
  if (item.estado === 'SINCRONIZANDO') return 'Sincronización en curso'
  return 'Pendiente de sincronización'
}
