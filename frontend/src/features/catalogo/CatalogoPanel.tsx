import { useRef, useState } from 'react'
import { buscarCatalogo, categoriasCatalogo, sol, urlFoto, type AnuncioPublico } from './catalogoApi'
import { ReportarAnuncio } from './ReportarAnuncio'

export function CatalogoPanel({ token }: { token?: string }) {
  const [items, setItems] = useState<AnuncioPublico[]>([])
  const [estado, setEstado] = useState<'inicial'|'carga'|'listo'|'error'>('inicial')
  const [filtros, setFiltros] = useState<Record<string,string>>({})
  const [aplicados, setAplicados] = useState<Record<string,string>>({})
  const [pagina, setPagina] = useState(1)
  const [siguiente, setSiguiente] = useState<number>()
  const [reportes, setReportes] = useState(false)
  const revision = useRef(0)
  async function buscar(p: number, consulta = filtros) {
    const turno = ++revision.current
    setEstado('carga')
    try {
      const datos = await buscarCatalogo(consulta,p)
      if (turno !== revision.current) return
      setItems(datos.items); setSiguiente(datos.siguientePagina ?? undefined)
      setPagina(p); setAplicados(consulta); setEstado('listo')
    } catch { if (turno === revision.current) setEstado('error') }
  }
  const cambiar = (campo: string, valor: string) => setFiltros(f => ({ ...f, [campo]: valor }))
  return <section id="catalogo" className="project-section catalog-section" aria-labelledby="catalogo-titulo">
    <h1 id="catalogo-titulo">Catálogo público</h1>
    <details onToggle={e => {
      if (!e.currentTarget.open) return
      void buscar(1)
      if (token) void fetch('/api/v1/catalogo/reportes-disponibles', { cache:'no-store',credentials:'omit' })
        .then(r => r.ok ? r.json() : null).then(d => setReportes(d?.recepcionExperimental === true))
        .catch(() => setReportes(false))
    }}>
      <summary>Ver anuncios de ganado</summary>
      <div className="catalog-workspace">
      <form className="animal-form catalog-filters" onSubmit={e => { e.preventDefault(); void buscar(1) }}>
        <h2>Filtros</h2>
        <div className="catalog-grid">
          <label className="form-field">Categoría<select value={filtros.categoria ?? ''} onChange={e => cambiar('categoria',e.target.value)}>
            <option value="">Todas</option>{categoriasCatalogo.map(c => <option key={c}>{c}</option>)}
          </select></label>
          <label className="form-field">Sexo<select value={filtros.sexo ?? ''} onChange={e => cambiar('sexo',e.target.value)}>
            <option value="">Todos</option><option value="HEMBRA">Hembra</option><option value="MACHO">Macho</option>
          </select></label>
          {([['raza','Raza'],['ubicacion','Ubicación general'],['minimo','Precio mínimo en PEN'],['maximo','Precio máximo en PEN']] as const)
            .map(([clave,etiqueta]) => <label className="form-field" key={clave}>{etiqueta}<input value={filtros[clave] ?? ''}
              type={clave === 'minimo' || clave === 'maximo' ? 'number' : 'text'} min="0" step="0.01" maxLength={100}
              onChange={e => cambiar(clave,e.target.value)} /></label>)}
        </div>
        <button className="button button-primary" disabled={estado === 'carga'}>Buscar anuncios</button>
      </form>
      <div className="catalog-results" aria-busy={estado === 'carga'}>
      {estado === 'carga' && <p className="workspace-state" role="status">Consultando anuncios…</p>}
      {estado === 'error' && <p className="workspace-state" role="alert">No se pudo consultar el catálogo. Revisa los filtros y vuelve a intentar.</p>}
      {estado === 'listo' && (items.length ? <>
        <div className="catalog-grid catalog-results-grid">{items.map(a => <article className="reader-note catalog-listing" key={a.id}>
          {a.fotos.length > 0 && <div className="catalog-photos">
            {a.fotos.map(foto => <img key={foto} loading="lazy" src={urlFoto(a.id,foto)} alt={`Foto de ${a.titulo}`} />)}
          </div>}
          <div className="catalog-card-copy">
          <h3>{a.titulo}</h3>
          <p className="catalog-price">{sol.format(a.precioPen)}</p>
          <p>{a.ubicacionGeneral}</p>
          <p className="catalog-attributes">{[a.categoria, a.raza, a.sexo].filter(Boolean).join(' · ')}</p>
          <p>{a.caracteristicas}</p>
          {a.contacto && <p>Contacto autorizado: {a.contacto}</p>}
          {a.genealogia.length > 0 && <p>Genealogía autorizada: {a.genealogia.map(
            g => [g.relacion === 'MADRE' ? 'Madre' : 'Padre',g.categoria,g.raza].filter(Boolean).join(' ')).join(' · ')}</p>}
          {reportes && token && <ReportarAnuncio id={a.id} token={token} />}
          </div>
        </article>)}</div>
      </> : <p className="workspace-state">No se encontraron anuncios para estos filtros.</p>)}
      {estado === 'listo' && (pagina > 1 || siguiente) && <nav className="catalog-actions" aria-label="Páginas del catálogo">
        {pagina > 1 && <button className="button button-outline" onClick={() => void buscar(pagina-1,aplicados)}>Anterior</button>}
        <span>Página {pagina}</span>
        {siguiente && <button className="button button-outline" onClick={() => void buscar(siguiente,aplicados)}>Siguiente</button>}
      </nav>}
      </div>
      </div>
    </details>
  </section>
}
