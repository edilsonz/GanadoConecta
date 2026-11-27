const { solicitudPrivada } = require('../../src/shared/api/solicitudPrivada')

const ruta = '/api/v1/ganaderias'
const token = 'sesion-sintetica'
const respuesta = (body, status = 200) => Response.json(body, { status })

afterEach(() => jest.restoreAllMocks())

test('las consultas privadas envían el bearer en memoria, sin caché y sin pedir CSRF', async () => {
  const fetch = jest.spyOn(globalThis, 'fetch').mockResolvedValueOnce(respuesta([]))
  await expect(solicitudPrivada(ruta, token)).resolves.toEqual([])
  expect(fetch).toHaveBeenCalledTimes(1)
  const [url, opciones] = fetch.mock.calls[0]
  expect(url).toBe(ruta)
  expect(opciones).toMatchObject({ credentials: 'same-origin', cache: 'no-store' })
  expect(opciones.headers.get('Authorization')).toBe(`Bearer ${token}`)
  expect(opciones.headers.has('X-CSRF-TOKEN')).toBe(false)
})

test('repite una escritura una sola vez si vence CSRF, con el mismo cuerpo', async () => {
  const fetch = jest.spyOn(globalThis, 'fetch')
    .mockResolvedValueOnce(respuesta({ token: 'primero' }))
    .mockResolvedValueOnce(respuesta({ codigo: 'CSRF_INVALIDO' }, 403))
    .mockResolvedValueOnce(respuesta({ token: 'segundo' }))
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
  const cuerpo = { operationId: 'operacion-sintetica' }
  await expect(solicitudPrivada(ruta, token, 'POST', cuerpo)).resolves.toBeUndefined()
  expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/api/v1/auth/csrf', ruta, '/api/v1/auth/csrf', ruta])
  const escrituras = [fetch.mock.calls[1][1], fetch.mock.calls[3][1]]
  expect(escrituras.map(x => x.body)).toEqual([JSON.stringify(cuerpo), JSON.stringify(cuerpo)])
  expect(escrituras.map(x => x.headers.get('X-CSRF-TOKEN'))).toEqual(['primero', 'segundo'])
})

test('un rechazo de autorización no se trata como un fallo CSRF', async () => {
  const fetch = jest.spyOn(globalThis, 'fetch')
    .mockResolvedValueOnce(respuesta({ token: 'csrf-sintetico' }))
    .mockResolvedValueOnce(respuesta({ codigo: 'ACCESO_DENEGADO' }, 403))
  await expect(solicitudPrivada(ruta, token, 'POST', {})).rejects.toMatchObject({ estado: 403 })
  expect(fetch).toHaveBeenCalledTimes(2)
})

test('conserva contenido binario al enviar una foto autorizada', async () => {
  const fetch = jest.spyOn(globalThis, 'fetch')
    .mockResolvedValueOnce(respuesta({ token: 'csrf-sintetico' }))
    .mockResolvedValueOnce(respuesta({ guardada: true }, 201))
  const foto = new Blob(['contenido de prueba'], { type: 'image/jpeg' })
  await expect(solicitudPrivada(ruta + '/foto', token, 'POST', foto)).resolves.toEqual({ guardada: true })
  const opciones = fetch.mock.calls[1][1]
  expect(opciones.body).toBe(foto)
  expect(opciones.headers.get('Content-Type')).toBe('image/jpeg')
})
