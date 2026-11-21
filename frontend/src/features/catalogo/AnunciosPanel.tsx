import { useRef, useState, type FormEvent } from 'react'
import { rutaAnuncios, sol, type AnuncioPrivado, type DatosAnuncio } from './catalogoApi'
import { ErrorSolicitudPrivada, solicitudPrivada } from '../../shared/api/solicitudPrivada'
import { useProteccionActualizacionPwa } from '../../pwa/pwaActividad'

type Props = { ganaderiaId: string; animalId: string; token: string; onSesionInvalida: () => void; vendido?: boolean }
const inicial: DatosAnuncio = { titulo:'', caracteristicas:'', ubicacionGeneral:'', precioPen:'',
  contacto:null, genealogiaAutorizada:false }
const etiquetas = { BORRADOR:'Borrador', PUBLICADO:'Publicado', PAUSADO:'Pausado', RETIRADO:'Retirado',
  CERRADO_VENTA:'Cerrado por venta' }

export function AnunciosPanel(p: Props) {
  const ruta = rutaAnuncios(p.ganaderiaId,p.animalId)
  const [items, setItems] = useState<AnuncioPrivado[]>()
  const [editar, setEditar] = useState<AnuncioPrivado>()
  const [form, setForm] = useState(inicial)
  const [autorizaContacto, setAutorizaContacto] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const enCurso = useRef(false)
  const borradorId = useRef<string | undefined>(undefined)
  useProteccionActualizacionPwa(`anuncio-${p.animalId}`, 'FORMULARIO', ocupado || form !== inicial)

  async function ejecutar(trabajo: () => Promise<void>) {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true); setMensaje('')
    try { await trabajo() }
    catch (error) {
      const codigo = error instanceof ErrorSolicitudPrivada ? error.estado : 0
      if (codigo === 401) p.onSesionInvalida()
      setMensaje(codigo === 409 ? 'El anuncio cambió. Actualiza antes de continuar.'
        : codigo === 403 ? 'Solo el propietario vigente puede gestionar anuncios.'
          : [400,413,415,422].includes(codigo) ? 'Revisa los campos, el consentimiento y el archivo.'
            : codigo === 503 ? 'El almacenamiento de fotos no está disponible.'
              : 'No se pudo confirmar la operación.')
    } finally { enCurso.current = false; setOcupado(false) }
  }
  async function cargar() { setItems(await solicitudPrivada<AnuncioPrivado[]>(ruta,p.token)) }
  function cambiar<K extends keyof DatosAnuncio>(campo: K, valor: DatosAnuncio[K]) {
    setForm(datos => ({ ...datos, [campo]: valor }))
  }
  function limpiar() {
    setEditar(undefined); setForm(inicial); setAutorizaContacto(false); borradorId.current = undefined
  }
  function guardar(evento: FormEvent) {
    evento.preventDefault()
    void ejecutar(async () => {
      const id = editar?.id ?? (borradorId.current ??= crypto.randomUUID())
      const datos = { ...form, id, version: editar?.version ?? 0, autorizaContacto,
        contacto: autorizaContacto ? form.contacto : null }
      await solicitudPrivada(editar ? `${ruta}/${id}` : ruta,p.token,editar ? 'PUT' : 'POST',datos)
      limpiar()
      setMensaje('Anuncio guardado.')
      await cargar()
    })
  }
  return <details className="health-history" onToggle={e => {
    if (e.currentTarget.open) void ejecutar(cargar)
  }}>
    <summary>Anuncios de este animal</summary>
    <button type="button" className="button button-outline" disabled={ocupado}
      onClick={() => void ejecutar(cargar)}>Actualizar anuncios</button>
    {mensaje && <p role="status">{mensaje}</p>}
    {items?.length === 0 && <p>Aún no hay anuncios para este animal.</p>}
    <div className="catalog-grid">{items?.map(a => <article className="reader-note" key={a.id}>
      <strong>{a.datos.titulo}</strong><p>{etiquetas[a.estado]} · {sol.format(Number(a.datos.precioPen))} · {a.fotos.length} fotos</p>
      {!p.vendido && a.estado !== 'RETIRADO' && a.estado !== 'CERRADO_VENTA' && <>
        <div className="catalog-actions">
          <button className="button button-outline" type="button" disabled={ocupado} onClick={() => {
            setEditar(a); setForm(a.datos); setAutorizaContacto(Boolean(a.datos.contacto))
          }}>Editar anuncio</button>
          {(a.estado === 'PUBLICADO' ? ['PAUSAR','RETIRAR'] : ['PUBLICAR','RETIRAR']).map(accion =>
            <button className="button button-outline" type="button" key={accion} disabled={ocupado}
              onClick={() => {
                if (accion === 'RETIRAR' && !window.confirm('¿Retirar definitivamente el anuncio?')) return
                void ejecutar(async () => {
                  await solicitudPrivada(`${ruta}/${a.id}/estado`,p.token,'POST',{ accion, version:a.version })
                  await cargar()
                })
              }}>{accion === 'PUBLICAR' ? 'Publicar' : accion === 'PAUSAR' ? 'Pausar' : 'Retirar'}</button>)}
        </div>
        {a.fotos.length < 5 && <label className="form-field">Añadir foto JPEG o PNG
          <input type="file" accept="image/jpeg,image/png" disabled={ocupado} onChange={e => {
            const archivo = e.currentTarget.files?.[0]
            e.currentTarget.value = ''
            if (!archivo) return
            if (archivo.size > 5 * 1024 * 1024 || !['image/jpeg','image/png'].includes(archivo.type)) {
              setMensaje('Selecciona una imagen JPEG o PNG de hasta 5 MiB.'); return
            }
            void ejecutar(async () => {
              await solicitudPrivada(`${ruta}/${a.id}/fotos?version=${a.version}`,p.token,'POST',archivo)
              setMensaje('Foto guardada.'); await cargar()
            })
          }} /></label>}
      </>}
    </article>)}</div>
    {!p.vendido && <form className="animal-form" onSubmit={guardar}>
      <h4>{editar ? 'Editar anuncio' : 'Preparar un anuncio'}</h4>
      <p>Los datos del anuncio y sus fotos serán públicos al publicarlo. Indica solo una ubicación general.</p>
      <fieldset disabled={ocupado}>
        <div className="animal-fields-grid">
          {([['titulo','Título',120],['ubicacionGeneral','Ubicación general',100]] as const).map(([campo,etiqueta,max]) =>
            <label className="form-field" key={campo}>{etiqueta}<input required maxLength={max}
              value={form[campo]} onChange={e => cambiar(campo,e.target.value)} /></label>)}
          <label className="form-field">Precio solicitado en PEN<input required type="number" min="0.01"
            step="0.01" max="9999999999.99" value={form.precioPen} onChange={e => cambiar('precioPen',e.target.value)} /></label>
        </div>
        <label className="form-field">Características públicas<textarea required maxLength={1000}
          value={form.caracteristicas} onChange={e => cambiar('caracteristicas',e.target.value)} /></label>
        <label className="checkbox-field"><input type="checkbox" checked={autorizaContacto}
          onChange={e => setAutorizaContacto(e.target.checked)} />Autorizo publicar el contacto indicado</label>
        {autorizaContacto && <label className="form-field">Contacto público<input required maxLength={180}
          value={form.contacto ?? ''} onChange={e => cambiar('contacto',e.target.value)} /></label>}
        <label className="checkbox-field"><input type="checkbox" checked={form.genealogiaAutorizada}
          onChange={e => cambiar('genealogiaAutorizada',e.target.checked)} />Autorizo publicar categoría y raza de madre y padre</label>
      </fieldset>
      <button className="button button-primary" disabled={ocupado}>Guardar anuncio</button>
      {editar && <button className="button button-outline" type="button" onClick={limpiar}>Cancelar edición</button>}
    </form>}
  </details>
}
