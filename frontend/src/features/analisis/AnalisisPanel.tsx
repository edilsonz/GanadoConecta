import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { tiposActividad } from '../calendario/agendaSanitariaApi'
import { ErrorAnimal, listarAnimales, type Animal } from '../ganado/animalesApi'
import { fechaHoyLima } from '../ganado/formularioAnimal'
import type { FiltrosReporte } from '../reportes/reportesApi'
import { consultarAnalisis, consultarAlertas, type Analisis } from './analisisApi'
import { AsistentePanel } from './AsistentePanel'
import './analisis.css'

const tiposSanitarios: Record<string, string> = {
  VACUNACION: 'Vacunaciones', DESPARASITACION: 'Desparasitaciones', TRATAMIENTO_SANITARIO: 'Tratamientos',
  BANO_ANTIPARASITARIO: 'Baños antiparasitarios', REVISION_VETERINARIA: 'Revisiones veterinarias',
}
const kilos = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

function periodoInicial(): FiltrosReporte {
  const hasta = fechaHoyLima()
  const desde = new Date(`${hasta}T00:00:00Z`)
  desde.setUTCDate(desde.getUTCDate() - 29)
  return { desde: desde.toISOString().slice(0, 10), hasta, animalId: '' }
}

function useConsulta<T>(cargar: () => Promise<T>, onSesionInvalida: () => void, nombre: string) {
  const [intento, setIntento] = useState(0)
  const [respuesta, setRespuesta] = useState<{ cargar: typeof cargar; intento: number; datos?: T; error?: string }>()
  useEffect(() => {
    let vigente = true
    cargar().then(datos => {
      if (vigente) setRespuesta({ cargar, intento, datos })
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorSolicitudPrivada && fallo.estado === 401) onSesionInvalida()
      const error = fallo instanceof ErrorSolicitudPrivada && fallo.estado === 403
        ? `No tienes acceso a ${nombre}.` : `No se pudo consultar ${nombre}.`
      setRespuesta({ cargar, intento, error })
    })
    return () => { vigente = false }
  }, [cargar, intento, nombre, onSesionInvalida])
  const actual = respuesta?.cargar === cargar && respuesta.intento === intento ? respuesta : undefined
  return { datos: actual?.datos, error: actual?.error, actualizar: () => setIntento(n => n + 1) }
}

function Resultados({ datos, animal }: { datos: Analisis; animal: string }) {
  const { sanidad, reproduccion: r, pesajes, periodo } = datos
  const mayor = Math.max(0, ...sanidad.porTipo.map(evento => evento.total))
  const frecuentes = sanidad.porTipo.filter(e => e.total === mayor).map(e => tiposSanitarios[e.tipo] ?? e.tipo)
  return <>
    <p>Período del <time>{periodo.desde}</time> al <time>{periodo.hasta}</time>, inclusive.</p>
    <div className="analisis-resultados">
      <section>
        <h4>Sanidad</h4>
        <p>Fuente: historial de eventos sanitarios confirmados.</p>
        {sanidad.total === 0 ? <p>No hay eventos sanitarios en el período.</p> : <>
          <p>Eventos confirmados: <strong>{sanidad.total}</strong>.</p>
          <p>Mayor frecuencia registrada: {frecuentes.join(', ')} ({mayor} por tipo).</p>
          <ul>{sanidad.porTipo.map(evento => <li key={evento.tipo}>
            {tiposSanitarios[evento.tipo] ?? evento.tipo}: {evento.total}</li>)}</ul>
        </>}
      </section>
      <section>
        <h4>Reproducción</h4>
        <p>Fuentes: historial reproductivo y registros de partos.</p>
        {r.montas + r.inseminaciones + r.evaluaciones + r.partos === 0
          ? <p>No hay eventos reproductivos en el período.</p>
          : <>
            <p>Montas: {r.montas}. Inseminaciones: {r.inseminaciones}.</p>
            <p>Evaluaciones: {r.evaluaciones}. Confirmadas: {r.confirmadas}. No confirmadas: {r.noConfirmadas}.
              Indeterminadas: {r.indeterminadas}.</p>
            <p>Partos: {r.partos}. Crías vinculadas a partos: {r.crias}.</p>
            <p>Las evaluaciones describen los eventos registrados. El filtro de partos corresponde a la madre.</p>
          </>}
      </section>
      <section>
        <h4>Peso</h4>
        <p>Fuente: historial de pesajes del animal seleccionado.</p>
        {!animal ? <p>Elige un animal para comparar sus pesajes.</p> : pesajes.length === 0
          ? <p>No hay pesajes para este animal en el período.</p> : <>
            <ul>{pesajes.map(pesaje => <li key={pesaje.id}>{pesaje.fecha}: {kilos.format(pesaje.kilos)} kg</li>)}</ul>
            {pesajes.length < 2 ? <p>Hace falta otro pesaje en el período para comparar.</p>
              : pesajes[1].variacion !== null && <p>Variación entre los dos últimos pesajes: <strong>{kilos.format(pesajes[1].variacion)} kg</strong>.</p>}
          </>}
      </section>
    </div>
  </>
}

type Props = { ganaderiaId: string; token: string; onSesionInvalida: () => void }

export function AnalisisPanel({ ganaderiaId, token, onSesionInvalida }: Props) {
  const [filtros, setFiltros] = useState(periodoInicial)
  const [aplicados, setAplicados] = useState(filtros)
  const [errorFechas, setErrorFechas] = useState(false)
  const [animales, setAnimales] = useState<Animal[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [cargandoAnimales, setCargandoAnimales] = useState(true)
  const [pagina, setPagina] = useState<{ cursor?: string }>({})
  const [errorAnimales, setErrorAnimales] = useState(false)
  const consultar = useCallback(() => consultarAnalisis(ganaderiaId, token, aplicados), [ganaderiaId, token, aplicados])
  const avisos = useCallback(() => consultarAlertas(ganaderiaId, token), [ganaderiaId, token])
  const analisis = useConsulta(consultar, onSesionInvalida, 'el análisis')
  const alertas = useConsulta(avisos, onSesionInvalida, 'las alertas')

  useEffect(() => {
    let vigente = true
    setCargandoAnimales(true)
    setErrorAnimales(false)
    listarAnimales(ganaderiaId, token, pagina.cursor).then(resultado => {
      if (!vigente) return
      setAnimales(previos => pagina.cursor
        ? [...previos, ...resultado.items.filter(item => !previos.some(animal => animal.animalId === item.animalId))]
        : resultado.items)
      setCursor(resultado.siguienteCursor)
    }).catch(fallo => {
      if (!vigente) return
      if (fallo instanceof ErrorAnimal && fallo.tipo === 'sesion') onSesionInvalida()
      else setErrorAnimales(true)
    }).finally(() => { if (vigente) setCargandoAnimales(false) })
    return () => { vigente = false }
  }, [ganaderiaId, token, pagina, onSesionInvalida])

  function aplicar(evento: FormEvent) {
    evento.preventDefault()
    const invalido = !filtros.desde || !filtros.hasta || filtros.desde > filtros.hasta
    setErrorFechas(invalido)
    if (!invalido) setAplicados({ ...filtros })
  }

  return <section className="analisis-panel" aria-labelledby="analisis-titulo">
    <h2 id="analisis-titulo">Análisis y alertas</h2>
    <div className="analisis-workspace">
    <div className="analisis-resumen">
    <section className="analisis-tarjeta" aria-labelledby="analisis-datos">
      <h3 id="analisis-datos">Análisis descriptivo</h3>
      <p>Estadísticas de registros confirmados. Los hallazgos describen lo registrado y no predicen resultados futuros.</p>
      <form className="analisis-filtros" onSubmit={aplicar}>
        <label className="form-field">Desde<input type="date" required value={filtros.desde}
          onChange={e => setFiltros({ ...filtros, desde: e.target.value })} /></label>
        <label className="form-field">Hasta<input type="date" required value={filtros.hasta}
          onChange={e => setFiltros({ ...filtros, hasta: e.target.value })} /></label>
        <label className="form-field">Animal<select value={filtros.animalId}
          onChange={e => setFiltros({ ...filtros, animalId: e.target.value })}>
          <option value="">Todos los animales</option>
          {animales.map(a => <option key={a.animalId} value={a.animalId}>{a.nombre || a.areteFrente || a.animalId}</option>)}
        </select></label>
        <button className="button button-primary">Consultar análisis</button>
      </form>
      {errorFechas && <p role="alert">«Desde» debe ser anterior o igual a «Hasta».</p>}
      {cargandoAnimales && <p role="status">Cargando lista de animales…</p>}
      {errorAnimales && <p role="alert">No se pudo cargar la lista de animales.</p>}
      {(cursor || errorAnimales) && <button type="button" className="button button-outline" disabled={cargandoAnimales}
        onClick={() => setPagina(errorAnimales ? { ...pagina } : { cursor: cursor ?? undefined })}>
        {errorAnimales ? 'Reintentar lista' : 'Cargar más animales'}</button>}
      {analisis.error ? <p role="alert">{analisis.error}</p> : analisis.datos
        ? <Resultados datos={analisis.datos} animal={aplicados.animalId} />
        : <p role="status">Consultando registros…</p>}
    </section>
    <section className="analisis-tarjeta" aria-labelledby="analisis-alertas">
      <h3 id="analisis-alertas">Alertas de seguimiento</h3>
      <p>Fuente: calendario sanitario. Se muestran actividades de hoy y vencidas.</p>
      <button type="button" className="button button-outline" onClick={alertas.actualizar}>Actualizar alertas</button>
      {alertas.error ? <p role="alert">{alertas.error}</p> : !alertas.datos ? <p role="status">Consultando alertas…</p> : <>
        <p>Fecha de corte: <time>{alertas.datos.fechaCorte}</time> (Lima).</p>
        {alertas.datos.avisos.length === 0 ? <p>No hay actividades de hoy o vencidas por atender.</p> :
          <ul className="analisis-avisos">{alertas.datos.avisos.map(aviso => <li key={aviso.actividadId}>
            <strong>{aviso.estado === 'VENCIDA' ? 'Vencida' : 'Para hoy'} · {tiposActividad[aviso.tipo]}</strong>
            <p><time>{aviso.fechaPrevista}</time> · {aviso.descripcion}</p>
          </li>)}</ul>}
      </>}
    </section>
    </div>
    <AsistentePanel key={`${ganaderiaId}:${aplicados.animalId}:${aplicados.desde}:${aplicados.hasta}`}
      ganaderiaId={ganaderiaId} token={token} filtros={aplicados} onSesionInvalida={onSesionInvalida}
      animal={aplicados.animalId ? animales.find(a => a.animalId === aplicados.animalId)?.nombre || 'Animal seleccionado' : ''} />
    </div>
  </section>
}
