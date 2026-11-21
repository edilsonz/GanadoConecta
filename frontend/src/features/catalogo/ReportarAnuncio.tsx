import { useRef, useState } from 'react'
import { rutaCatalogo } from './catalogoApi'
import { solicitudPrivada } from '../../shared/api/solicitudPrivada'

export function ReportarAnuncio({ id, token }: { id: string; token: string }) {
  const [motivo,setMotivo] = useState('')
  const [mensaje,setMensaje] = useState('')
  const [enviando,setEnviando] = useState(false)
  const enCurso = useRef(false)
  return <details>
    <summary>Reportar publicación</summary>
    <form className="animal-form" onSubmit={async e => {
      e.preventDefault()
      if (enCurso.current) return
      enCurso.current = true
      setEnviando(true)
      try {
        await solicitudPrivada(`${rutaCatalogo}/${encodeURIComponent(id)}/reportes`,token,'POST',{motivo:motivo.trim()})
        setMotivo(''); setMensaje('Reporte recibido.')
      } catch { setMensaje('No se pudo confirmar el reporte. Revisa el motivo y la sesión.') }
      finally { enCurso.current = false; setEnviando(false) }
    }}>
      <label className="form-field">Motivo<textarea required minLength={5} maxLength={1000}
        value={motivo} onChange={e => setMotivo(e.target.value)} /></label>
      <button className="button button-outline" disabled={enviando}>Enviar reporte</button>
      {mensaje && <p role="status">{mensaje}</p>}
    </form>
  </details>
}
