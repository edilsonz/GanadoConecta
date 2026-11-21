import { useEffect, useRef, useState } from 'react'
import { ErrorSolicitudPrivada, solicitudPrivada } from '../../shared/api/solicitudPrivada'

type Reporte = { id: string; titulo: string; motivo: string; estado: string; estadoAnuncio: string }
const ruta = '/api/v1/moderacion/reportes'

export function ModeracionPanel({ token, onSesionInvalida }: { token: string; onSesionInvalida: () => void }) {
  const [bandeja, setBandeja] = useState<{ token: string; lista: Reporte[] }>()
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const enCurso = useRef(false)
  const tokenActual = useRef(token)
  tokenActual.current = token

  useEffect(() => {
    let vigente = true
    void solicitudPrivada<Reporte[]>(ruta, token).then(lista => {
      if (vigente && Array.isArray(lista)) setBandeja({ token, lista })
    }).catch(error => {
      if (vigente && error instanceof ErrorSolicitudPrivada && error.estado === 401) onSesionInvalida()
    })
    return () => { vigente = false }
  }, [token, onSesionInvalida])

  async function ejecutar(id?: string, decision?: 'RETIRADO'|'DESESTIMADO') {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true); setMensaje('')
    try {
      if (id) await solicitudPrivada(`${ruta}/${encodeURIComponent(id)}/revision`,token,'POST',{decision})
      const lista = await solicitudPrivada<Reporte[]>(ruta, token)
      if (tokenActual.current === token && Array.isArray(lista)) setBandeja({ token, lista })
    } catch (error) {
      if (tokenActual.current !== token) return
      const codigo = error instanceof ErrorSolicitudPrivada ? error.estado : 0
      if (codigo === 401 || codigo === 403) setBandeja(undefined)
      if (codigo === 401) onSesionInvalida()
      setMensaje(codigo === 409 ? 'El reporte ya fue revisado. Actualiza la bandeja.' : 'No se pudo consultar o actualizar la bandeja.')
    } finally { enCurso.current = false; setOcupado(false) }
  }
  if (bandeja?.token !== token) return null
  const { lista } = bandeja
  return <details className="account-panel">
    <summary>Moderación del catálogo</summary>
    <button className="button button-outline" disabled={ocupado} type="button" onClick={() => void ejecutar()}>
      Actualizar reportes
    </button>
    {mensaje && <p role="status">{mensaje}</p>}
    {lista.length === 0 && <p>No hay reportes.</p>}
    <div className="catalog-grid">{lista.map(reporte => <article className="reader-note" key={reporte.id}>
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
