export const base = 'http://127.0.0.1:18083'
export const filtros = [
  ['', null, null],
  ['categoria=VACA&sexo=HEMBRA', 'VACA', null],
  ['categoria=TORO&ubicacion=Cusco', 'TORO', 'Cusco'],
  ['ubicacion=Lima&minimo=1000&maximo=1600', null, 'Lima'],
]
export const catalogo = (pagina, filtro) => `/api/v1/catalogo?pagina=${pagina}${filtro && '&' + filtro}`
