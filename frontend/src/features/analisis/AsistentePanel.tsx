import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import type { FiltrosReporte } from '../reportes/reportesApi'
import { preguntarAsistente, type RespuestaAsistente, type TemaAsistente } from './analisisApi'

const temas = {
  PESO: ['Peso', '¿Cuánto cambió el peso entre los últimos pesajes?'],
  SANIDAD: ['Sanidad', '¿Qué eventos sanitarios tiene registrados?'],
  REPRODUCCION: ['Reproducción', '¿Qué eventos reproductivos constan?'],
  AGENDA: ['Calendario', '¿Qué actividades pendientes tengo?'],
} as const

type Props = { ganaderiaId: string; token: string; filtros: FiltrosReporte; animal: string; onSesionInvalida: () => void }
type Turno = { id: number; pregunta: string; respuesta?: RespuestaAsistente; error?: string }

export function AsistentePanel({ ganaderiaId, token, filtros, animal, onSesionInvalida }: Props) {
  const [tema, setTema] = useState<TemaAsistente>('PESO')
  const [pregunta, setPregunta] = useState('')
  const [turnos, setTurnos] = useState<Turno[]>([])
  const [validacion, setValidacion] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const siguiente = useRef(0)
  const vigente = useRef(false)
  const pendiente = useRef(false)

  useEffect(() => {
    vigente.current = true
    return () => { vigente.current = false }
  }, [])

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const texto = pregunta.trim()
    if (!texto || pendiente.current) return
    if (tema !== 'AGENDA' && !filtros.animalId) {
      setValidacion('Elige un animal en los filtros de análisis y pulsa «Consultar análisis».')
      return
    }
    setValidacion('')
    pendiente.current = true
    setOcupado(true)
    setPregunta('')
    const id = ++siguiente.current
    const registrar = (resultado: Partial<Turno>) => {
      if (vigente.current) setTurnos(previos => previos.map(t => t.id === id ? { ...t, ...resultado } : t))
    }
    setTurnos(previos => [...previos.slice(-5), { id, pregunta: texto }])
    try {
      registrar({ respuesta: await preguntarAsistente(ganaderiaId, token, texto, tema, filtros) })
    } catch (fallo) {
      if (!vigente.current) return
      const estado = fallo instanceof ErrorSolicitudPrivada ? fallo.estado : undefined
      if (estado === 401) onSesionInvalida()
      if (estado === 403) {
        setTurnos([{ id, pregunta: texto, error: 'No tienes acceso al ámbito de esta consulta.' }])
      } else {
        registrar({ error: estado === 429 ? 'El asistente está ocupado. Inténtalo de nuevo en unos segundos.'
          : 'El asistente no está disponible en este momento. Puedes seguir consultando análisis y alertas.' })
      }
    } finally {
      pendiente.current = false
      if (vigente.current) setOcupado(false)
    }
  }

  return <section className="analisis-tarjeta asistente-panel" aria-labelledby="analisis-asistente">
    <h3 id="analisis-asistente">Asistente conversacional</h3>
    <p>Consulta registros confirmados. La IA no reemplaza la valoración veterinaria.</p>
    <p className="asistente-ambito">{animal || 'Todos los animales'} · {filtros.desde} a {filtros.hasta}</p>
    <div role="log" aria-label="Conversación de esta vista" className="asistente-conversacion">
      {turnos.map(turno => <article key={turno.id}>
        <p className="asistente-pregunta"><strong>Tú</strong> {turno.pregunta}</p>
        {turno.error ? <p role="alert">{turno.error}</p> : !turno.respuesta
          ? <p role="status">Consultando el modelo local…</p> : <>
            <p className="asistente-respuesta"><strong>Asistente</strong> {turno.respuesta.texto}</p>
            {turno.respuesta.fuentes.length > 0 && <ul aria-label="Registros de origen">
              {turno.respuesta.fuentes.map(f => <li key={f.registroId}>{f.tipo} · <time dateTime={f.fecha}>{f.fecha}</time></li>)}
            </ul>}
          </>}
      </article>)}
    </div>
    <form className="asistente-formulario" onSubmit={enviar}>
      <label className="form-field">Tema<select value={tema} disabled={ocupado}
        onChange={e => { setTema(e.target.value as TemaAsistente); setValidacion('') }}>
        {Object.entries(temas).map(([id, [nombre]]) => <option key={id} value={id}>{nombre}</option>)}
      </select></label>
      <label className="form-field">Pregunta<input type="text" required maxLength={400} value={pregunta}
        placeholder={temas[tema][1]} onChange={e => setPregunta(e.target.value)} disabled={ocupado} /></label>
      <button type="submit" className="button button-primary" disabled={ocupado}>Preguntar</button>
    </form>
    {validacion && <p role="alert">{validacion}</p>}
  </section>
}
