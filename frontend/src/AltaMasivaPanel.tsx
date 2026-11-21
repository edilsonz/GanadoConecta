import { useEffect, useRef, useState } from 'react'
import type { FormularioAnimal } from './AnimalesPanel'
import { ErrorAnimal, registrarAnimal, type CategoriaAnimal, type SolicitudRegistroAnimal } from './animalesApi'
import { useProteccionActualizacionPwa } from './pwaActividad'

type Fila = {
  id: string
  datos: FormularioAnimal
  errores: Record<string, string>
  solicitud?: SolicitudRegistroAnimal
  resultado?: { confirmado: boolean; texto: string }
}
type Props = {
  ganaderiaId: string
  token: string
  conectado: boolean
  inicial: FormularioAnimal
  validar: (datos: FormularioAnimal) => Record<string, string>
  preparar: (datos: FormularioAnimal, id: string) => SolicitudRegistroAnimal
  categorias: Record<CategoriaAnimal, string>
  alConfirmar: () => void
  onSesionInvalida: () => void
}
const LIMITE = 20

export function AltaMasivaPanel(p: Props) {
  const nueva = (): Fila => ({ id: crypto.randomUUID(), datos: { ...p.inicial }, errores: {} })
  const [filas, setFilas] = useState<Fila[]>([])
  const [vistaPrevia, setVistaPrevia] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const enviando = useRef(false)
  const envio = useRef<AbortController | null>(null)
  useEffect(() => () => envio.current?.abort(), [p.ganaderiaId])
  useProteccionActualizacionPwa(`alta-masiva-${p.ganaderiaId}`, 'FORMULARIO',
    ocupado || filas.some(f => !f.resultado?.confirmado))

  function editar<K extends keyof FormularioAnimal>(id: string, clave: K, valor: FormularioAnimal[K]) {
    setFilas(actuales => actuales.map(f => f.id === id
      ? { ...f, datos: { ...f.datos, [clave]: valor }, errores: {}, resultado: undefined } : f))
  }

  function revisar() {
    setFilas(actuales => actuales.map(f => {
      if (f.resultado?.confirmado || f.solicitud) return f
      return { ...f, errores: p.validar(f.datos) }
    }))
    setVistaPrevia(true)
    setMensaje('')
  }

  const disponibles = filas.filter(f => !f.resultado?.confirmado
    && Object.keys(f.errores).length === 0 && (f.solicitud || !f.resultado))

  async function confirmar() {
    if (!vistaPrevia || !p.conectado || enviando.current) return
    enviando.current = true
    const controlador = new AbortController()
    envio.current = controlador
    setOcupado(true)
    setMensaje('')
    let confirmadas = 0
    try {
      for (const fila of disponibles) {
        if (controlador.signal.aborted) break
        const solicitud = fila.solicitud ?? p.preparar(fila.datos, fila.id)
        const actualizar = (cambio: Partial<Fila>) => setFilas(actuales =>
          actuales.map(f => f.id === fila.id ? { ...f, ...cambio } : f))
        actualizar({ solicitud, resultado: undefined })
        try {
          const recibo = await registrarAnimal(p.ganaderiaId, solicitud, p.token, controlador.signal)
          if (controlador.signal.aborted) break
          confirmadas++
          actualizar({ resultado: { confirmado: true, texto: recibo.advertencias.includes('ARETE_COINCIDENTE')
            ? 'Confirmado. Hay otro animal con ese arete.' : 'Confirmado.' } })
        } catch (error) {
          if (controlador.signal.aborted) break
          const fallo = error instanceof ErrorAnimal ? error : new ErrorAnimal('servicio')
          if (fallo.tipo === 'validacion' || fallo.tipo === 'conflicto') {
            actualizar({ solicitud: undefined, errores: fallo.erroresPorCampo, resultado: {
              confirmado: false, texto: fallo.tipo === 'conflicto'
                ? 'Conflicto de identidad. Revisa el listado antes de continuar con esta fila.'
                : 'El servidor rechazó esta fila. Corrige sus datos.',
            } })
            continue
          }
          actualizar({ resultado: { confirmado: false, texto: 'Sin confirmación. Se conservaron los identificadores de la fila.' } })
          setMensaje(fallo.tipo === 'permiso' ? 'El servidor denegó el acceso. Se detuvo el lote.'
            : 'Se detuvo el lote. Revisa la sesión o conexión y reintenta las filas pendientes.')
          if (fallo.tipo === 'sesion') p.onSesionInvalida()
          break
        }
      }
    } finally {
      if (!controlador.signal.aborted && confirmadas) p.alConfirmar()
      envio.current = null
      enviando.current = false
      setOcupado(false)
    }
  }

  const campos = [
    ['nombre', 'Nombre'], ['areteFrente', 'Arete frontal'], ['areteReverso', 'Arete reverso'],
    ['raza', 'Raza'], ['coloresIdentificacionTextil', 'Colores de lana o cinta'], ['observaciones', 'Observaciones'],
  ] as const

  return <details className="health-history">
    <summary>Registrar varios animales</summary>
    <p>Hasta {LIMITE} animales por lote. Revisa las filas antes de confirmar.</p>
    {!p.conectado && <p role="status">Recupera la conexión para enviar el lote.</p>}
    <fieldset disabled={ocupado} style={{ minWidth: 0 }}>
      <div className="bulk-table-scroll" role="region" aria-label="Tabla editable del lote" tabIndex={0}>
        <table>
          <caption>Animales del lote</caption>
          <thead><tr><th scope="col">Fila</th><th scope="col">Datos del animal</th><th scope="col">Resultado</th></tr></thead>
          <tbody>{filas.map((f, i) => <tr key={f.id}>
            <th scope="row">{i + 1}</th>
            <td><fieldset disabled={vistaPrevia || Boolean(f.solicitud) || f.resultado?.confirmado}
              style={{ minWidth: 0 }}>
              <legend>Datos de la fila {i + 1}</legend>
              <div className="animal-fields-grid">
                <label className="form-field">Sexo *<select value={f.datos.sexo}
                  onChange={e => editar(f.id, 'sexo', e.target.value as FormularioAnimal['sexo'])}>
                  <option value="">Selecciona</option><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option>
                </select></label>
                <label className="form-field">Categoría<select value={f.datos.categoria}
                  onChange={e => editar(f.id, 'categoria', e.target.value as CategoriaAnimal)}>
                  <option value="">Sin categoría</option>
                  {Object.entries(p.categorias).map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}
                </select></label>
                {campos.map(([clave, etiqueta]) => <label className="form-field" key={clave}>{etiqueta}
                  <input maxLength={clave === 'observaciones' ? 2000 : 100}
                    value={f.datos[clave]} onChange={e => editar(f.id, clave, e.target.value)} />
                </label>)}
                <label className="form-field">Ubicación textil<select value={f.datos.ubicacionIdentificacionTextil}
                  onChange={e => editar(f.id, 'ubicacionIdentificacionTextil', e.target.value as FormularioAnimal['ubicacionIdentificacionTextil'])}>
                  <option value="">Sin ubicación</option><option value="IZQUIERDA">Izquierda</option>
                  <option value="DERECHA">Derecha</option><option value="AMBAS">Ambas</option>
                </select></label>
                <label className="form-field">Fecha de nacimiento<input type="date" value={f.datos.fechaNacimiento}
                  onChange={e => editar(f.id, 'fechaNacimiento', e.target.value)} /></label>
                {f.datos.fechaNacimiento && <label className="checkbox-field">
                  <input type="checkbox" checked={f.datos.nacimientoEstimado}
                    onChange={e => editar(f.id, 'nacimientoEstimado', e.target.checked)} />Fecha aproximada
                </label>}
              </div>
            </fieldset></td>
            <td style={{ overflowWrap: 'anywhere' }}>
              {f.resultado?.texto ?? (vistaPrevia && (Object.keys(f.errores).length ? 'Requiere revisión.' : 'Lista para enviar.'))}
              {Object.values(f.errores).map((texto, indice) => <p key={indice}>{texto}</p>)}
              {!vistaPrevia && !f.solicitud && !f.resultado?.confirmado && <button type="button"
                onClick={() => setFilas(a => a.filter(item => item.id !== f.id))}>Quitar fila {i + 1}</button>}
            </td>
          </tr>)}</tbody>
        </table>
      </div>
      {vistaPrevia ? <>
        <button type="button" className="button button-outline" onClick={() => setVistaPrevia(false)}>Volver a editar</button>
        <button type="button" className="button button-primary" disabled={!p.conectado || !disponibles.length}
          onClick={() => void confirmar()}>Confirmar {disponibles.length} {disponibles.length === 1 ? 'fila' : 'filas'}</button>
      </> : <>
        <button type="button" className="button button-outline" disabled={filas.length >= LIMITE}
          onClick={() => setFilas(a => a.length < LIMITE ? [...a, nueva()] : a)}>Añadir fila</button>
        <button type="button" className="button button-primary" disabled={filas.length === 0}
          onClick={revisar}>Revisar lote</button>
        {filas.length > 0 && <button type="button" className="button button-outline" onClick={() => {
          if (window.confirm('¿Empezar otro lote y cerrar el borrador actual?')) { setFilas([]); setMensaje('') }
        }}>Empezar otro lote</button>}
      </>}
    </fieldset>
    <p role="status">{filas.filter(f => f.resultado?.confirmado).length} de {filas.length} filas confirmadas. {mensaje}</p>
  </details>
}
