import { useEffect, useRef, useState } from 'react'
import { ErrorAnimal, listarAnimales, type Animal } from '../ganado/animalesApi'
import { GraficoPeso } from '../produccion/GraficoPeso'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { consultarReporte, type FiltrosReporte, type ReporteGanadero } from './reportesApi'
import './reportes.css'

const fechaLima = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
})
const numero = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })
const tipos: Record<string, string> = {
  VACUNACION: 'Vacunaciones', DESPARASITACION: 'Desparasitaciones', TRATAMIENTO_SANITARIO: 'Tratamientos sanitarios',
  BANO_ANTIPARASITARIO: 'Baños antiparasitarios', REVISION_VETERINARIA: 'Revisiones veterinarias',
}
const indicadoresReproduccion = {
  montas: 'Montas', inseminaciones: 'Inseminaciones', evaluaciones: 'Evaluaciones de gestación',
  confirmadas: 'Evaluaciones confirmadas', noConfirmadas: 'Evaluaciones no confirmadas',
  indeterminadas: 'Evaluaciones indeterminadas', partos: 'Partos registrados', crias: 'Crías vinculadas a partos',
} as const

type Props = { ganaderiaId: string; token: string; onSesionInvalida: () => void }

function Indicadores({ filas }: { filas: [string, number][] }) {
  return <dl className="reporte-indicadores">{filas.map(([nombre, total]) =>
    <div key={nombre}><dt>{nombre}</dt><dd>{numero.format(total)}</dd></div>)}</dl>
}

function Contenido({ datos, filtros }: { datos: ReporteGanadero; filtros: FiltrosReporte }) {
  const { sanidad, reproduccion, peso } = datos
  return (
    <>
      <p>Período consultado: {filtros.desde} a {filtros.hasta}, inclusive.</p>
      <div className="reporte-resumen-grid">
      <section className="reporte-tarjeta" aria-labelledby="reporte-sanidad">
        <h3 id="reporte-sanidad">Reporte sanitario</h3>
        {sanidad.total === 0 ? <p>No hay eventos sanitarios confirmados para este filtro.</p> :
          <Indicadores filas={[[ 'Eventos confirmados', sanidad.total ],
            ...sanidad.porTipo.map(({ tipo, total }): [string, number] => [tipos[tipo] ?? tipo, total])]} />}
      </section>
      <section className="reporte-tarjeta" aria-labelledby="reporte-reproduccion">
        <h3 id="reporte-reproduccion">Reporte reproductivo</h3>
        {reproduccion.montas || reproduccion.inseminaciones || reproduccion.evaluaciones || reproduccion.partos
          ? <Indicadores filas={Object.keys(indicadoresReproduccion).map(clave => {
            const k = clave as keyof typeof indicadoresReproduccion
            return [indicadoresReproduccion[k], reproduccion[k]]
          })} /> : <p>No hay registros reproductivos para este filtro.</p>}
        {filtros.animalId && <p>En partos y crías, el filtro corresponde a la madre.</p>}
      </section>
      </div>
      <section className="reporte-tarjeta" aria-labelledby="reporte-productivo">
        <h3 id="reporte-productivo">Reporte productivo</h3>
        {!filtros.animalId ? <p>Elige un animal para consultar su evolución de peso.</p> :
          !peso.ultimo ? <p>No hay mediciones de peso para este filtro.</p> :
            <>
              <Indicadores filas={[[ 'Mediciones en el período', peso.total ], [ 'Último peso (kg)', peso.ultimo.kilos ]]} />
              <p>Último pesaje del período: {peso.ultimo.fecha}.</p>
              {peso.total > peso.mediciones.length && <p>Se muestran las {peso.mediciones.length} mediciones más recientes. Acota el período para consultar las anteriores.</p>}
              <div className="reporte-peso-grid">
              <div className="reporte-grafico"><GraficoPeso mediciones={peso.mediciones} /></div>
              <table className="reporte-pesos workspace-table">
                <caption>Mediciones y variaciones dentro del período</caption>
                <thead><tr><th scope="col">Fecha</th><th scope="col">Peso (kg)</th><th scope="col">Variación (kg)</th></tr></thead>
                <tbody>{peso.mediciones.map(m => <tr key={m.id}>
                  <td>{m.fecha}</td><td>{numero.format(m.kilos)}</td>
                  <td>{m.variacion === null ? 'Sin anterior' : (m.variacion > 0 ? '+' : '') + numero.format(m.variacion)}</td>
                </tr>)}</tbody>
              </table>
              </div>
            </>}
      </section>
    </>
  )
}

function periodoInicial(): FiltrosReporte {
  const hoy = Date.now()
  return { desde: fechaLima.format(hoy - 29 * 86_400_000), hasta: fechaLima.format(hoy), animalId: '' }
}

export function ReportesPanel({ ganaderiaId, token, onSesionInvalida }: Props) {
  const [filtro, setFiltro] = useState(periodoInicial)
  const [consulta, setConsulta] = useState(filtro)
  const [resultado, setResultado] = useState<ReporteGanadero>()
  const [errorReporte, setErrorReporte] = useState('')
  const [errorFiltro, setErrorFiltro] = useState('')
  const [animales, setAnimales] = useState<Animal[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [errorAnimales, setErrorAnimales] = useState(false)
  const [cargandoAnimales, setCargandoAnimales] = useState(true)
  const [solicitudAnimales, setSolicitudAnimales] = useState<{ cursor?: string }>({})
  const [revision, setRevision] = useState(0)
  const consultaAnterior = useRef<string | undefined>(undefined)

  useEffect(() => {
    let vigente = true
    setResultado(undefined)
    setErrorReporte('')
    consultarReporte(ganaderiaId, token, consulta).then(datos => {
      if (vigente) setResultado(datos)
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 401) onSesionInvalida()
      else if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 403) setErrorReporte('No tienes acceso a este reporte.')
      else setErrorReporte('No se pudo cargar el reporte.')
    })
    return () => { vigente = false }
  }, [ganaderiaId, token, consulta, revision, onSesionInvalida])

  useEffect(() => {
    let vigente = true
    setCargandoAnimales(true)
    setErrorAnimales(false)
    const ambito = ganaderiaId + ':' + token
    const continuar = consultaAnterior.current === ambito ? solicitudAnimales.cursor : undefined
    listarAnimales(ganaderiaId, token, continuar).then(pagina => {
      if (!vigente) return
      consultaAnterior.current = ambito
      setAnimales(actual => continuar
        ? [...actual, ...pagina.items.filter(a => !actual.some(previo => previo.animalId === a.animalId))]
        : pagina.items)
      setCursor(pagina.siguienteCursor)
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorAnimal && fallo.tipo === 'sesion') onSesionInvalida()
      else setErrorAnimales(true)
    }).finally(() => { if (vigente) setCargandoAnimales(false) })
    return () => { vigente = false }
  }, [ganaderiaId, token, solicitudAnimales, onSesionInvalida])

  return (
    <section className="reportes-panel" aria-labelledby="titulo-reportes">
      <h2 id="titulo-reportes">Reportes</h2>
      <p>Consulta registros confirmados de esta ganadería. Las fechas corresponden a Lima.</p>
      <form className="reporte-filtros" onSubmit={e => {
        e.preventDefault()
        if (!filtro.desde || !filtro.hasta || filtro.desde > filtro.hasta) {
          setErrorFiltro('Elige un rango válido. «Desde» debe ser anterior o igual a «Hasta».')
          return
        }
        setErrorFiltro('')
        setConsulta({ ...filtro })
      }}>
        <label className="form-field">Desde<input type="date" value={filtro.desde} required
          onChange={e => setFiltro({ ...filtro, desde: e.target.value })} /></label>
        <label className="form-field">Hasta<input type="date" value={filtro.hasta} required
          onChange={e => setFiltro({ ...filtro, hasta: e.target.value })} /></label>
        <label className="form-field">Animal<select value={filtro.animalId}
          onChange={e => setFiltro({ ...filtro, animalId: e.target.value })}>
          <option value="">Todos los animales</option>
          {animales.map(a => <option key={a.animalId} value={a.animalId}>{a.nombre || a.areteFrente || a.animalId}</option>)}
        </select></label>
        <button className="button button-primary">Consultar reporte</button>
      </form>
      {errorFiltro && <p role="alert">{errorFiltro}</p>}
      {cargandoAnimales && <p role="status">Cargando animales para el filtro…</p>}
      {errorAnimales && <p role="alert">No se pudo consultar la lista de animales.</p>}
      {errorAnimales && <button type="button" className="button button-outline"
        onClick={() => setSolicitudAnimales({ ...solicitudAnimales })}>Reintentar lista de animales</button>}
      {cursor && !errorAnimales && <button type="button" className="button button-outline"
        disabled={cargandoAnimales} onClick={() => setSolicitudAnimales({ cursor })}>Cargar más animales</button>}
      {errorReporte ? <>
        <p role="alert">{errorReporte}</p>
        <button type="button" className="button button-outline" onClick={() => setRevision(n => n + 1)}>Reintentar reporte</button>
      </> : resultado ? <Contenido datos={resultado} filtros={consulta} />
        : <p role="status">Cargando reporte…</p>}
    </section>
  )
}
