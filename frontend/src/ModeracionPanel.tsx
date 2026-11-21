import { useRef, useState } from 'react'
import { ErrorSolicitudPrivada, solicitudPrivada } from './solicitudPrivada'

type Reporte = { id: string; titulo: string; motivo: string; estado: string; estadoAnuncio: string }
export function ModeracionPanel({ token, onSesionInvalida }: { token: string; onSesionInvalida: () => void }) {
  const ruta = '/api/v1/moderacion/reportes'
  const [lista, setLista] = useState<Reporte[]>()
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const enCurso = useRef(false)

  async function ejecutar(id?: string, decision?: 'RETIRADO'|'DESESTIMADO') {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true); setMensaje('')
    try {
      if (id) await solicitudPrivada(`${ruta}/${encodeURIComponent(id)}/revision`,token,'POST',{decision})
      setLista(await solicitudPrivada<Reporte[]>(ruta,token))
    } catch (error) {
      setLista(undefined)
      const codigo = error instanceof ErrorSolicitudPrivada ? error.estado : 0
      if (codigo === 401) onSesionInvalida()
      setMensaje(codigo === 403 ? 'Esta sección requiere el permiso de moderación.'
        : codigo === 409 ? 'El reporte ya fue revisado. Actualiza la bandeja.' : 'No se pudo consultar o actualizar la bandeja.')
    } finally { enCurso.current = false; setOcupado(false) }
  }
  return <details className="account-panel" onToggle={e => { if (e.currentTarget.open) void ejecutar() }}>
    <summary>Moderación del catálogo</summary>
    <button className="button button-outline" disabled={ocupado} type="button" onClick={() => void ejecutar()}>
      Actualizar reportes
    </button>
    {mensaje && <p role="status">{mensaje}</p>}
    {lista?.length === 0 && <p>No hay reportes.</p>}
    <div className="catalog-grid">{lista?.map(reporte => <article className="reader-note" key={reporte.id}>
      <h3>{reporte.titulo}</h3><p>{reporte.motivo}</p><small>{reporte.estado} · {reporte.estadoAnuncio}</small>
      {reporte.estado === 'PENDIENTE' && <div className="catalog-actions">
        <button className="button button-outline" disabled={ocupado} type="button"
          onClick={() => void ejecutar(reporte.id,'DESESTIMADO')}>Desestimar</button>
        <button className="button button-primary" disabled={ocupado} type="button" onClick={() => {
          if (window.confirm('¿Retirar este anuncio del catálogo?')) void ejecutar(reporte.id,'RETIRADO')
        }}>Retirar publicación</button>
      </div>}
    </article>)}</div>
  </details>
}
