import { useEffect, useState } from 'react'
import { ModeracionPanel } from '../catalogo/ModeracionPanel'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { consultarEstadisticasPlataforma, listarUsuarios,
  type EstadisticasPlataforma, type FiltrosUsuarios, type PaginaUsuarios } from './administracionApi'
import './administracion.css'

const inicial: FiltrosUsuarios = { buscar: '', estado: '' }
const fechaAlta = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeZone: 'America/Lima' })

export function AdministracionPanel({ token, onSesionInvalida, onPermisoPerdido }: {
  token: string; onSesionInvalida: () => void; onPermisoPerdido: () => void
}) {
  const [filtro, setFiltro] = useState(inicial)
  const [consulta, setConsulta] = useState(inicial)
  const [pagina, setPagina] = useState<PaginaUsuarios>()
  const [estadisticas, setEstadisticas] = useState<EstadisticasPlataforma>()
  const [retorno, setRetorno] = useState<(string | undefined)[]>([])
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let vigente = true
    setPagina(undefined)
    setError('')
    listarUsuarios(token, consulta).then(datos => {
      if (vigente) setPagina(datos)
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 401) onSesionInvalida()
      else if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 403) onPermisoPerdido()
      else setError(fallo instanceof ErrorSolicitudPrivada && fallo.estado === 400
        ? 'Busca por UUID o correo completo.' : 'No se pudo consultar la lista de cuentas.')
    })
    return () => { vigente = false }
  }, [consulta, token, revision, onSesionInvalida, onPermisoPerdido])

  const [errorEstadisticas, setErrorEstadisticas] = useState(false)
  useEffect(() => {
    let vigente = true
    setEstadisticas(undefined)
    setErrorEstadisticas(false)
    consultarEstadisticasPlataforma(token).then(datos => {
      if (vigente) setEstadisticas(datos)
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 401) onSesionInvalida()
      else if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 403) onPermisoPerdido()
      else setErrorEstadisticas(true)
    })
    return () => { vigente = false }
  }, [token, revision, onSesionInvalida, onPermisoPerdido])

  return (
    <section className="project-section administracion" id="administracion" aria-labelledby="titulo-administracion">
      <h1 id="titulo-administracion">Administración</h1>
      <section className="admin-stats" aria-label="Estadísticas de plataforma">
        {estadisticas ? <dl className="administracion-indicadores workspace-metrics">
          <div><dt>Cuentas totales</dt><dd>{estadisticas.cuentas}</dd></div>
          <div><dt>Cuentas activas</dt><dd>{estadisticas.cuentasActivas}</dd></div>
          <div><dt>Ganaderías totales</dt><dd>{estadisticas.ganaderias}</dd></div>
          <div><dt>Ganaderías activas</dt><dd>{estadisticas.ganaderiasActivas}</dd></div>
        </dl> : errorEstadisticas ? <p role="alert">No se pudieron cargar las estadísticas.</p>
          : <p role="status">Cargando estadísticas…</p>}
      </section>

      <section className="workspace-card admin-directory" aria-labelledby="admin-directory-title">
      <h2 id="admin-directory-title">Cuentas de usuario</h2>
      <form className="administracion-filtros" onSubmit={e => {
        e.preventDefault()
        setRetorno([])
        setConsulta({ ...filtro, buscar: filtro.buscar.trim() })
      }}>
        <label className="form-field">UUID o correo completo
          <input value={filtro.buscar} maxLength={254} onChange={e => setFiltro({ ...filtro, buscar: e.target.value })} />
        </label>
        <label className="form-field">Estado de cuenta
          <select value={filtro.estado} onChange={e => setFiltro({ ...filtro, estado: e.target.value as FiltrosUsuarios['estado'] })}>
            <option value="">Todos</option><option value="ACTIVO">Activa</option><option value="INACTIVO">Inactiva</option>
          </select>
        </label>
        <button className="button button-primary">Buscar cuentas</button>
      </form>

      {error && <p role="alert">{error}</p>}
      {(error || errorEstadisticas) && <button className="button button-outline" type="button"
        onClick={() => setRevision(n => n + 1)}>Reintentar consultas</button>}
      {pagina ? (pagina.items.length ? <ul className="administracion-lista" aria-label="Cuentas">
        {pagina.items.map(u => <li key={u.id}>
          <div><strong>{u.correo}</strong><small>{u.id}</small></div>
          <span className="access-badge">{u.estado === 'ACTIVO' ? 'Activa' : 'Inactiva'}</span>
          <span>Alta: {fechaAlta.format(new Date(u.creadoEn))}</span>
        </li>)}
      </ul> : <p>No se encontraron cuentas.</p>) : !error && <p role="status">Consultando cuentas…</p>}
      <div className="catalog-actions">
        {retorno.length > 0 && <button className="button button-outline" type="button" disabled={!pagina}
          onClick={() => { setConsulta({ ...consulta, cursor: retorno.at(-1) }); setRetorno(retorno.slice(0, -1)) }}>Anterior</button>}
        {pagina?.siguienteCursor && <button className="button button-outline" type="button"
          onClick={() => { setRetorno([...retorno, consulta.cursor]); setConsulta({ ...consulta, cursor: pagina.siguienteCursor! }) }}>
          Siguiente
        </button>}
      </div>
      </section>
      <ModeracionPanel token={token} onSesionInvalida={onSesionInvalida} />
    </section>
  )
}
