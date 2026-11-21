import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'

export type DatosAnuncio = { titulo: string; caracteristicas: string; ubicacionGeneral: string;
  precioPen: string | number; contacto: string | null; genealogiaAutorizada: boolean }
export type AnuncioPrivado = { id: string; datos: DatosAnuncio; estado: 'BORRADOR'|'PUBLICADO'|'PAUSADO'|'RETIRADO'|'CERRADO_VENTA';
  version: number; fotos: string[] }
export type AnuncioPublico = { id: string; titulo: string; caracteristicas: string; ubicacionGeneral: string;
  precioPen: number; contacto?: string; categoria?: string; raza?: string; sexo: 'HEMBRA'|'MACHO'; fotos: string[];
  genealogia: { relacion: 'MADRE'|'PADRE'; categoria?: string; raza?: string }[] }
type Pagina = { items: AnuncioPublico[]; siguientePagina?: number }
export const rutaCatalogo = '/api/v1/catalogo'
export const urlFoto = (anuncio: string, foto: string) =>
  `${rutaCatalogo}/${encodeURIComponent(anuncio)}/fotos/${encodeURIComponent(foto)}`
export const rutaAnuncios = (g: string, a: string) =>
  `/api/v1/ganaderias/${encodeURIComponent(g)}/animales/${encodeURIComponent(a)}/anuncios`
export const categoriasCatalogo = ['TERNERA','VAQUILLA','VACA','TERNERO','TORETE','TORO']
export const sol = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export async function buscarCatalogo(filtros: Record<string,string>, pagina: number): Promise<Pagina> {
  const query = new URLSearchParams({ pagina: String(pagina) })
  for (const [clave, valor] of Object.entries(filtros)) if (valor.trim()) query.set(clave, valor.trim())
  const respuesta = await fetch(`${rutaCatalogo}?${query}`, { cache: 'no-store', credentials: 'omit' })
  if (!respuesta.ok) throw new ErrorSolicitudPrivada(respuesta.status)
  const datos: Pagina = await respuesta.json()
  if (!Array.isArray(datos.items)) throw new Error('Catálogo inválido')
  return datos
}
