import { useRef, useState } from 'react'
import { ErrorSolicitudPrivada, solicitudPrivada } from '../../shared/api/solicitudPrivada'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'

type Props = { ganaderiaId: string; animalId: string; token: string; onVenta: () => void;
  onSesionInvalida: () => void }
type Recibo = { operationId: string; animalId: string; anunciosCerrados: number }

export function VentaAnimal(p: Props) {
  const operacion = useRef<string | undefined>(undefined)
  const enCurso = useRef(false)
  const [enviando,setEnviando] = useState(false)
  const [mensaje,setMensaje] = useState('')
  useProteccionActualizacionPwa(`venta-${p.animalId}`, 'FORMULARIO', enviando || Boolean(operacion.current))

  async function vender() {
    if (enCurso.current) return
    if (!operacion.current && !window.confirm('¿Marcar el animal como vendido? Se cerrarán sus anuncios activos.')) return
    enCurso.current = true
    setEnviando(true)
    const operationId = operacion.current ??= crypto.randomUUID()
    try {
      const recibo = await solicitudPrivada<Recibo>(
        `/api/v1/ganaderias/${encodeURIComponent(p.ganaderiaId)}/animales/${encodeURIComponent(p.animalId)}/venta`,
        p.token,'POST',{ operationId })
      if (recibo.operationId !== operationId || recibo.animalId !== p.animalId
          || !Number.isInteger(recibo.anunciosCerrados) || recibo.anunciosCerrados < 0) throw new Error()
      operacion.current = undefined
      p.onVenta()
    } catch (error) {
      const codigo = error instanceof ErrorSolicitudPrivada ? error.estado : 0
      if (codigo === 401) p.onSesionInvalida()
      if ([400,403,404,409,422].includes(codigo)) operacion.current = undefined
      setMensaje(codigo === 409 ? 'Ya existe una venta o la operación entra en conflicto. Vuelve a consultar la ficha.'
        : codigo === 403 ? 'Solo el propietario vigente puede registrar la venta.'
          : 'No se pudo confirmar la venta.')
    } finally { enCurso.current=false; setEnviando(false) }
  }
  return <div className="catalog-actions">
    <button type="button" className="button button-outline" disabled={enviando}
      onClick={() => void vender()}>{operacion.current ? 'Reintentar venta' : 'Registrar como vendido'}</button>
    {mensaje && <p role="status">{mensaje}</p>}
  </div>
}
