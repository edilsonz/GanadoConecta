import { base, catalogo, filtros } from './consultas-sinteticas.mjs'

for (const [filtro] of filtros) {
  for (let pagina = 1; pagina <= 3; pagina++) {
    const respuesta = await fetch(base + catalogo(pagina, filtro),
      { signal: AbortSignal.timeout(4000), redirect: 'error' })
    if (!respuesta.ok || (await respuesta.json()).items?.length !== 20) {
      throw new Error('No se pudo poblar una página sintética del catálogo.')
    }
  }
}
console.log('12 páginas públicas consultadas.')
