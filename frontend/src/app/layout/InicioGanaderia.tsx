import { useEffect, useState, type ReactNode } from 'react'
import { consultarAlertas, type Alertas } from '../../features/analisis/analisisApi'
import { listarAgenda, tiposActividad, type ActividadSanitaria } from '../../features/calendario/agendaSanitariaApi'
import { fechaHoyLima } from '../../features/ganado/formularioAnimal'
import { consultarReporte, type ReporteGanadero } from '../../features/reportes/reportesApi'
import { ErrorSanidad } from '../../features/sanidad/vacunacionesApi'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import type { VistaGanaderia } from './EspacioGanaderia'

type Consulta<T> = { valor: T } | { error: true }
type Resumen = {
  reporte?: Consulta<ReporteGanadero>
  agenda?: Consulta<ActividadSanitaria[]>
  alertas?: Consulta<Alertas>
  permisoDenegado?: true
}

type Props = {
  ganaderiaId: string
  token: string
  conectado: boolean
  accesoVigente: boolean
  navegar: (destino: VistaGanaderia) => void
  onSesionInvalida: () => void
}

const numero = new Intl.NumberFormat('es-PE')
const dia = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', timeZone: 'UTC' })
const fechaBreve = (fecha: string) => dia.format(new Date(fecha + 'T00:00:00Z'))

function periodoReciente() {
  const hasta = fechaHoyLima()
  const desde = new Date(hasta + 'T00:00:00Z')
  desde.setUTCDate(desde.getUTCDate() - 29)
  return { desde: desde.toISOString().slice(0, 10), hasta, animalId: '' }
}

function PanelResumen<T>({ consulta, nombre, children }: {
  consulta: Consulta<T> | undefined
  nombre: string
  children: (datos: T) => ReactNode
}) {
  if (!consulta) return <p className="workspace-state" role="status">Consultando {nombre}…</p>
  if ('error' in consulta) return <p className="workspace-state" role="alert">No se pudo consultar {nombre}.</p>
  return children(consulta.valor)
}

function InicioEnLinea({ ganaderiaId, token, navegar, onSesionInvalida }:
  Pick<Props, 'ganaderiaId' | 'token' | 'navegar' | 'onSesionInvalida'>) {
  const [periodo] = useState(periodoReciente)
  const [revision, cambiarRevision] = useState(0)
  const [respuesta, guardar] = useState<Resumen & { token: string; revision: number }>()
  const datos = respuesta?.token === token && respuesta.revision === revision ? respuesta : undefined

  useEffect(() => {
    let vigente = true
    let sesionInvalidada = false
    const registrar = (cambio: Resumen) => {
      if (vigente) guardar(anterior => ({
        ...(anterior?.token === token && anterior.revision === revision ? anterior : {}),
        token, revision, ...cambio,
      }))
    }
    const registrarFallo = (error: unknown, cambio: Resumen) => {
      if (!vigente) return
      const estado = error instanceof ErrorSolicitudPrivada ? error.estado
        : error instanceof ErrorSanidad && error.tipo === 'sesion' ? 401
        : error instanceof ErrorSanidad && error.tipo === 'permiso' ? 403 : undefined
      if (estado === 401 && !sesionInvalidada) {
        sesionInvalidada = true
        onSesionInvalida()
      }
      registrar(estado === 403 ? { permisoDenegado: true } : cambio)
    }
    void consultarReporte(ganaderiaId, token, periodo)
      .then(valor => registrar({ reporte: { valor } }))
      .catch(error => registrarFallo(error, { reporte: { error: true } }))
    void listarAgenda(ganaderiaId, token, { estado: 'PENDIENTE' })
      .then(actividades => registrar({ agenda: { valor: actividades.filter(a => a.estadoVisible === 'PROXIMA')
        .sort((a, b) => a.fechaPrevista.localeCompare(b.fechaPrevista)).slice(0, 4) } }))
      .catch(error => registrarFallo(error, { agenda: { error: true } }))
    void consultarAlertas(ganaderiaId, token)
      .then(valor => registrar({ alertas: { valor } }))
      .catch(error => registrarFallo(error, { alertas: { error: true } }))
    return () => { vigente = false }
  }, [ganaderiaId, token, periodo, revision, onSesionInvalida])

  if (datos?.permisoDenegado) {
    return <p className="workspace-state" role="alert">Ya no tienes acceso al resumen de esta ganadería.</p>
  }

  return <>
    <section className="dashboard-metrics" aria-labelledby="dashboard-metrics-title">
      <div className="workspace-heading">
        <div>
          <h3 id="dashboard-metrics-title">Registros de los últimos 30 días</h3>
          <p>{fechaBreve(periodo.desde)} – {fechaBreve(periodo.hasta)} · Lima</p>
        </div>
        {[datos?.reporte, datos?.agenda, datos?.alertas].some(valor => valor && 'error' in valor) &&
          <button className="button button-outline" type="button" onClick={() => cambiarRevision(n => n + 1)}>
            Actualizar resumen
          </button>}
      </div>
      <PanelResumen consulta={datos?.reporte} nombre="los indicadores">{reporte =>
        <dl className="workspace-metrics">
          <div><dt>Eventos sanitarios</dt><dd>{numero.format(reporte.sanidad.total)}</dd></div>
          <div><dt>Evaluaciones de gestación confirmadas</dt><dd>{numero.format(reporte.reproduccion.confirmadas)}</dd></div>
          <div><dt>Partos registrados</dt><dd>{numero.format(reporte.reproduccion.partos)}</dd></div>
        </dl>}
      </PanelResumen>
    </section>

    <div className="dashboard-activities">
      <section className="workspace-card" aria-labelledby="dashboard-agenda-title">
        <div className="workspace-heading">
          <h3 id="dashboard-agenda-title">Próximas actividades</h3>
          <button className="button button-outline" type="button" onClick={() => navegar('calendario')}>Abrir agenda</button>
        </div>
        <PanelResumen consulta={datos?.agenda} nombre="la agenda">{agenda => agenda.length
          ? <ul className="workspace-timeline">{agenda.map(a =>
            <li key={a.actividadId}><time dateTime={a.fechaPrevista}>{fechaBreve(a.fechaPrevista)}</time>
              <div><strong>{a.descripcion}</strong><p>{tiposActividad[a.tipo]}</p></div>
            </li>)}</ul>
          : <p className="workspace-state">No hay actividades futuras programadas.</p>}
        </PanelResumen>
      </section>
      <section className="workspace-card" aria-labelledby="dashboard-alertas-title">
        <div className="workspace-heading">
          <h3 id="dashboard-alertas-title">Alertas de seguimiento</h3>
          <button className="button button-outline" type="button" onClick={() => navegar('analisis')}>Ver alertas</button>
        </div>
        <PanelResumen consulta={datos?.alertas} nombre="las alertas">{alertas => alertas.avisos.length
          ? <ul className="workspace-timeline workspace-timeline-urgent">{alertas.avisos.slice(0, 4).map(a =>
            <li key={a.actividadId}><time dateTime={a.fechaPrevista}>{fechaBreve(a.fechaPrevista)}</time>
              <div><strong>{a.descripcion}</strong><p>{a.estado === 'VENCIDA' ? 'Vencida' : 'Para hoy'} · {tiposActividad[a.tipo]}</p></div>
            </li>)}</ul>
          : <p className="workspace-state">No hay actividades de hoy o vencidas por atender.</p>}
        </PanelResumen>
      </section>
    </div>
  </>
}

export function InicioGanaderia({ conectado, accesoVigente, ...props }: Props) {
  return <section className="producer-dashboard" aria-labelledby="producer-home-title">
    <div className="workspace-heading dashboard-header">
      <h2 id="producer-home-title">Resumen de la ganadería</h2>
      <div className="workspace-actions">
        <button className="button button-primary" type="button" onClick={() => props.navegar('ganado')}>Ver animales</button>
        <button className="button button-outline" type="button" onClick={() => props.navegar('calendario')}>Ver calendario</button>
        <button className="button button-outline" type="button" onClick={() => props.navegar('reportes')}>Ver reportes</button>
      </div>
    </div>
    {conectado && accesoVigente
      ? <InicioEnLinea key={props.ganaderiaId} {...props} />
      : <p className="workspace-state">{conectado ? 'Los indicadores requieren acceso vigente a esta ganadería.'
        : 'Conéctate a internet para consultar los indicadores y la agenda confirmada.'}</p>}
  </section>
}
