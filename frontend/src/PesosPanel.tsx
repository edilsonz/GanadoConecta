import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ErrorSolicitudPrivada, solicitudPrivada } from './solicitudPrivada'

type Medicion = { id: string; kilos: number; fecha: string; observaciones: string | null }
type Historial = { mediciones: Medicion[]; ultimo: Medicion | null }
type Props = { ganaderiaId: string; animalId: string; token: string; puedeEscribir: boolean; onSesionInvalida: () => void }
const formato = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

function Grafico({ mediciones }: { mediciones: Medicion[] }) {
  const dias = mediciones.map(m => Date.parse(m.fecha))
  const minimo = mediciones.reduce((a, m) => Math.min(a, m.kilos), Infinity)
  const maximo = mediciones.reduce((a, m) => Math.max(a, m.kilos), 0)
  const amplitud = dias[dias.length - 1] - dias[0]
  const puntos = mediciones.map((m, i) => [
    amplitud ? 16 + 288 * (dias[i] - dias[0]) / amplitud : 160,
    maximo === minimo ? 55 : 96 - 82 * (m.kilos - minimo) / (maximo - minimo),
  ])
  return <figure style={{ margin: 0 }}>
    <svg viewBox="0 0 320 110" width="100%" role="img" aria-label="Evolución del peso según fecha de medición">
      <polyline points={puntos.map(p => p.join(',')).join(' ')} fill="none" stroke="#386650" strokeWidth="2" />
      {puntos.map(([x, y], i) => <circle key={mediciones[i].id} cx={x} cy={y} r="3" fill="#386650" />)}
    </svg>
    <figcaption>{mediciones[0].fecha} — {mediciones[mediciones.length - 1].fecha} ·
      Rango {formato.format(minimo)} a {formato.format(maximo)} kg</figcaption>
  </figure>
}

export function PesosPanel({ ganaderiaId, animalId, token, puedeEscribir, onSesionInvalida }: Props) {
  const ruta = `/api/v1/ganaderias/${ganaderiaId}/animales/${animalId}/pesos`
  const [historial, setHistorial] = useState<Historial>()
  const [estado, setEstado] = useState<'carga' | 'listo' | 'error'>('carga')
  const [revision, setRevision] = useState(0)
  const [kilos, setKilos] = useState('')
  const [fecha, setFecha] = useState('')
  const [notas, setNotas] = useState('')
  const [pendiente, setPendiente] = useState<Medicion>()
  const [mensaje, setMensaje] = useState('')
  const [enviando, setEnviando] = useState(false)
  const ocupado = useRef(false)

  useEffect(() => {
    let vigente = true
    void solicitudPrivada<Historial>(ruta, token).then(resultado => {
      if (vigente) { setHistorial(resultado); setEstado('listo') }
    }).catch(error => {
      if (!vigente) return
      setEstado('error')
      if (error instanceof ErrorSolicitudPrivada && [401,403].includes(error.estado)) {
        setHistorial(undefined)
        if (error.estado === 401) onSesionInvalida()
      }
    })
    return () => { vigente = false }
  }, [ruta, token, revision, onSesionInvalida])

  function actualizar() { setEstado('carga'); setRevision(n => n + 1) }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (ocupado.current || !puedeEscribir) return
    const valor = Number(kilos)
    if (!pendiente && (!(valor > 0 && valor < 1e9) || !fecha)) {
      setMensaje('Ingresa un peso mayor que cero y la fecha de medición.')
      return
    }
    const dato = pendiente ?? { id: crypto.randomUUID(), kilos: valor, fecha, observaciones: notas || null }
    ocupado.current = true
    setEnviando(true)
    setPendiente(dato)
    setMensaje('')
    try {
      const recibo = await solicitudPrivada<{ id: string; resultado: string }>(ruta, token, 'POST', dato)
      if (recibo.id !== dato.id || !['APLICADO', 'REPROCESADO'].includes(recibo.resultado)) throw new Error()
      setPendiente(undefined); setKilos(''); setFecha(''); setNotas('')
      setMensaje('Medición registrada.')
      actualizar()
    } catch (error) {
      const codigo = error instanceof ErrorSolicitudPrivada ? error.estado : 0
      if ([400, 422, 409].includes(codigo)) setPendiente(undefined)
      if (codigo === 401) onSesionInvalida()
      setMensaje(codigo === 403 ? 'El servidor no autorizó el registro.'
        : codigo === 409 ? 'La medición entra en conflicto. Actualiza y revisa el historial.'
        : [400,422].includes(codigo) ? 'Revisa el peso, la fecha y las observaciones.'
          : 'No se recibió confirmación. Puedes reenviar la misma medición.')
    } finally { ocupado.current = false; setEnviando(false) }
  }

  return <section className="health-history" aria-label="Peso">
    <div className="animals-list-heading"><h4>Peso</h4>
      <button type="button" className="button button-outline" onClick={actualizar}>Actualizar pesos</button>
    </div>
    {estado === 'carga' && <p role="status">Consultando pesos…</p>}
    {estado === 'error' && <p role="alert">No se pudo cargar el historial de peso.</p>}
    {estado === 'listo' && historial?.mediciones.length === 0 && <p>Aún no hay mediciones de peso.</p>}
    {historial?.ultimo && <>
      <p><strong>Último peso</strong> {formato.format(historial.ultimo.kilos)} kg · {historial.ultimo.fecha}</p>
      <Grafico mediciones={historial.mediciones} />
      <ol>{historial.mediciones.map((m, i) => <li key={m.id}>
        <strong>{m.fecha} · {formato.format(m.kilos)} kg</strong>
        {i > 0 && <p>Variación frente al {historial.mediciones[i-1].fecha}: {
          formato.format(Math.round((m.kilos - historial.mediciones[i-1].kilos) * 1000) / 1000)} kg</p>}
        {m.observaciones && <p>{m.observaciones}</p>}
      </li>)}</ol>
    </>}
    {puedeEscribir && <form className="animal-form" onSubmit={guardar}>
      <fieldset disabled={enviando || Boolean(pendiente)} style={{ minWidth: 0 }}>
        <div className="animal-fields-grid">
          <label className="form-field">Peso en kilogramos
            <input type="number" required min="0.001" max="999999999.999" step="0.001" value={kilos} onChange={e => setKilos(e.target.value)} />
          </label>
          <label className="form-field">Fecha de medición
            <input type="date" required value={fecha} onChange={e => setFecha(e.target.value)} />
          </label>
        </div>
        <label className="form-field">Observaciones del peso
          <textarea value={notas} maxLength={2000} onChange={e => setNotas(e.target.value)} />
        </label>
      </fieldset>
      <button className="button button-primary" disabled={enviando}>{pendiente ? 'Reintentar medición' : 'Guardar peso'}</button>
    </form>}
    {mensaje && <p role="status">{mensaje}</p>}
  </section>
}
