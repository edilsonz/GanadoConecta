import assert from 'node:assert/strict'
import { randomBytes } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { join, resolve } from 'node:path'
import { abrirChrome, campo, elemento, esperarElemento, hasta, literal, pulsar } from './chrome.mjs'

const origen = process.env.GC_E2E_URL ?? 'https://127.0.0.1:15173'
const base = process.env.GC_E2E_DB ?? ''
const cert = process.env.GC_E2E_CERT
const chrome = process.env.GC_E2E_CHROME ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const solo = process.argv[2]
assert.ok(/^ganadoconecta_e2e_[a-z0-9]+$/.test(base), 'Indica la base aislada en GC_E2E_DB')
assert.ok(/^https:\/\/127\.0\.0\.1:(?!5173$)\d+$/.test(origen) && cert,
  'Usa un origen HTTPS aislado y su certificado de prueba')
assert.ok(!solo || ['07', '08', '09', 'ux'].includes(solo), 'Filtro de prueba inválido')

const id = randomBytes(5).toString('hex')
const carpeta = resolve('.cache/e2e', id)
await mkdir(carpeta, { recursive: true })
const resultados = []
const farm = `Ganadería E2E ${id}`
const correo = `e2e-${id}@example.invalid`
const clave = randomBytes(24).toString('base64url')
const [primero, segundo] = ['PRUEBA-LOTE-01', 'PRUEBA-LOTE-02']
const privado = `Nota privada E2E ${id}`
const vacuna = `Vacuna sintética ${id}`
const zona = `Zona sintética ${id}`
const hoy = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
})

const navegador = await abrirChrome(chrome, cert)
const productor = await navegador.pagina()
const cuentaLista = p => esperarElemento(p, '#iniciar-sesion .session-email')
const boton = (p, texto, ambito) => pulsar(p, 'button', texto, ambito)
const texto = (p, valor, ambito) => esperarElemento(p, 'p,strong,dd,li', valor, ambito)
const captura = (p, nombre) => p.captura(join(carpeta, `${nombre}.png`))
const llenar = async (p, selector, valor, ambito) =>
  assert.ok(await campo(p, selector, valor, ambito), `No se pudo llenar ${selector}`)

function campoConEtiqueta(p, etiqueta, selector, valor, ambito = 'body') {
  return p.evaluar(`(() => {
    const control = (${elemento('label', etiqueta, ambito)})?.querySelector(${literal(selector)});
    if (!control || control.disabled) return false;
    Object.getOwnPropertyDescriptor(Object.getPrototypeOf(control),'value').set.call(control,${literal(valor)});
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input', {bubbles:true}));
    return control.value === ${literal(valor)};
  })()`).then(ok => assert.ok(ok, `No se pudo llenar ${etiqueta}`))
}

// Observa el envío original y permite repetirlo sin volcar credenciales o sustituir respuestas.
await productor.cdp('Page.addScriptToEvaluateOnNewDocument', { source: `
  (() => {
    const enviar = window.fetch.bind(window);
    let autorizacion, cierre;
    window.fetch = (url, inicio) => {
      const credencial = new Headers(inicio?.headers).get('Authorization');
      if (credencial && String(url).startsWith('/api/v1/')) autorizacion = credencial;
      if (inicio?.method === 'POST' && String(url).endsWith('/completar')) cierre = [url,inicio];
      return enviar(url,inicio);
    };
    const tokenCsrf = async () => (await (await enviar('/api/v1/auth/csrf',
      {credentials:'same-origin',cache:'no-store'})).json()).token;
    window.gcE2eRepetirCierre = async () => {
      if (!cierre) return {estado:0};
      const [ruta, inicio] = cierre;
      const headers = new Headers(inicio.headers);
      headers.set('X-CSRF-TOKEN', await tokenCsrf());
      const r = await enviar(ruta,{...inicio,headers});
      return {estado:r.status,codigo:r.ok ? null : (await r.json()).codigo};
    };
    window.gcE2eApi = async (solicitudes) => {
      const respuestas = [];
      for (const {ruta, metodo, datos} of solicitudes) {
        if (!ruta.startsWith('/api/v1/ganaderias/')) throw Error('Ruta de fixture inválida');
        const r = await enviar(ruta, {method:metodo ?? 'GET',credentials:'same-origin',cache:'no-store',
          headers:{Authorization:autorizacion,...(metodo ? {'X-CSRF-TOKEN':await tokenCsrf(),'Content-Type':'application/json'} : {})},
          ...(datos ? {body:JSON.stringify(datos)} : {})});
        respuestas.push({estado:r.status, cuerpo:await r.json()});
        if (!r.ok) break;
      }
      return respuestas;
    };
  })()
` })

async function api(...solicitudes) {
  const respuestas = await productor.evaluar(`window.gcE2eApi(${literal(solicitudes)})`)
  const fallo = respuestas.findIndex(r => r.estado >= 400)
  if (fallo >= 0) {
    const { estado, cuerpo } = respuestas[fallo]
    throw Error(`HTTP ${estado} (${cuerpo.codigo ?? 'sin código'}) en fixture ${fallo + 1}`)
  }
  return respuestas.map(r => r.cuerpo)
}

async function iniciar(p) {
  await p.abrir(origen)
  await esperarElemento(p, '#login-correo')
  await llenar(p, '#login-correo', correo)
  await llenar(p, '#login-contrasena', clave)
  await boton(p, 'Iniciar sesión', '#iniciar-sesion')
  await cuentaLista(p)
}

async function seleccionarFarm(p) {
  await pulsar(p, '.farm-option', farm)
  await esperarElemento(p, '#producer-title', farm)
}

async function navegar(p, vista, ancho = 1440) {
  const vistas = { inicio: 'Inicio', ganado: 'Mi ganado', sanidad: 'Sanidad', reproduccion: 'Reproducción',
    produccion: 'Producción', calendario: 'Calendario', anuncios: 'Anuncios', familia: 'Acceso familiar' }
  if (ancho <= 820) await llenar(p, '.producer-mobile-nav select', vista)
  else {
    await boton(p, vistas[vista], '.producer-sidebar')
    await esperarElemento(p, 'button[aria-current="page"]', vistas[vista], '.producer-sidebar')
  }
}

async function ficha(p, nombre) {
  if (await p.evaluar(`Boolean(${elemento('.animal-detail-heading button', 'Cerrar')})`)) {
    await boton(p, 'Cerrar', '.animal-detail-heading')
  }
  await pulsar(p, '.animal-option', nombre)
  await esperarElemento(p, '.animal-detail-heading h4', nombre)
}

let rutaFarm, idPrimero, idSegundo
async function preparar() {
  await productor.abrir(origen)
  await hasta(() => productor.evaluar(`(async () => {
    const r = await fetch('/api/health', {cache:'no-store'}); return r.ok && (await r.json()).status === 'UP';
  })()`), 'La API HTTPS no está saludable')
  await llenar(productor, '#registro-correo', correo)
  await llenar(productor, '#registro-contrasena', clave)
  await llenar(productor, '#registro-confirmacion', clave)
  await boton(productor, 'Crear mi cuenta', '#crear-cuenta')
  await texto(productor, 'Cuenta creada correctamente', '#crear-cuenta')
  await iniciar(productor)
  await llenar(productor, '#ganaderia-nombre', farm)
  await boton(productor, 'Crear ganadería')
  await esperarElemento(productor, '#producer-title', farm)
  const farmId = await productor.evaluar('document.querySelector(".producer-header select").value')
  assert.match(farmId, /^[0-9a-f-]{36}$/)
  rutaFarm = `/api/v1/ganaderias/${farmId}`
  await navegar(productor, 'ganado')
  for (const nombre of [primero, segundo]) {
    await llenar(productor, '#animal-sexo', 'HEMBRA')
    await llenar(productor, '#animal-nombre', nombre)
    await llenar(productor, '#animal-categoria', 'VACA')
    await llenar(productor, '#animal-observaciones', privado)
    await boton(productor, 'Registrar animal', 'form.animal-form')
    await esperarElemento(productor, '.animal-option', nombre)
  }
  const [pagina] = await api({ ruta: rutaFarm + '/animales' })
  idPrimero = pagina.items.find(a => a.nombre === primero)?.animalId
  idSegundo = pagina.items.find(a => a.nombre === segundo)?.animalId
  assert.ok(idPrimero && idSegundo && idPrimero !== idSegundo)
}

async function campana(verificar = true) {
  const descripcion = `Campaña E2E ${id}`
  const agenda = '#agenda-sanitaria'
  await navegar(productor, 'calendario')
  await pulsar(productor, 'label.checkbox-field', 'Actividad colectiva', agenda)
  for (const nombre of [primero, segundo]) await pulsar(productor, 'label.checkbox-field', nombre, agenda)
  await boton(productor, 'Usar campaña de vacunación', agenda)
  await campoConEtiqueta(productor, 'Fecha prevista *', 'input', hoy.format(Date.now() + 86_400_000), agenda)
  await campoConEtiqueta(productor, 'Descripción *', 'textarea', descripcion, agenda)
  await boton(productor, 'Programar actividad', agenda)
  await texto(productor, 'Actividad programada.', agenda)
  await esperarElemento(productor, '.agenda-item .access-badge', 'Programada')
  await boton(productor, 'Seleccionar atendidos', agenda)
  assert.equal(await productor.evaluar('document.querySelectorAll(".agenda-item form input:checked").length'), 0)
  await pulsar(productor, 'label.checkbox-field', primero, '.agenda-item form')
  await campoConEtiqueta(productor, 'Nombre de la vacuna', 'input', vacuna, '.agenda-item')
  await boton(productor, 'Confirmar atenciones', agenda)
  await texto(productor, `Sin atención confirmada: ${segundo}`, agenda)
  if (!verificar) return
  assert.deepEqual(await productor.evaluar('window.gcE2eRepetirCierre()'), { estado: 200, codigo: null },
    'El reintento debe ser idempotente')
  await captura(productor, '07-campana-realizada')
  await productor.abrir(origen)
  await cuentaLista(productor)
  await seleccionarFarm(productor)
  await navegar(productor, 'calendario')
  await texto(productor, `Sin atención confirmada: ${segundo}`, agenda)
  await navegar(productor, 'ganado')
  for (const nombre of [primero, segundo]) {
    await ficha(productor, nombre)
    await pulsar(productor, '[role=tab]', 'Sanidad')
    if (nombre === primero) await texto(productor, vacuna, '.vaccinations-panel')
    else await texto(productor, 'Aún no hay vacunaciones registradas.', '.vaccinations-panel')
  }
  const [a, b] = await api(...[idPrimero, idSegundo].map(animal => ({
    ruta: `${rutaFarm}/animales/${animal}/eventos-sanitarios`,
  })))
  assert.equal(a.length, 1, 'La campaña debe dejar exactamente un evento tras repetir el cierre')
  assert.equal(a[0].tipo, 'VACUNACION')
  assert.equal(b.length, 0, 'El participante sin atención no debe tener eventos')
}

const anuncioRuta = animal => `${rutaFarm}/animales/${animal}/anuncios`
async function abrirAnuncios(nombre) {
  await navegar(productor, 'ganado')
  await ficha(productor, nombre)
  await pulsar(productor, '[role=tab]', 'Anuncios')
  await pulsar(productor, 'summary', 'Anuncios de este animal')
  await esperarElemento(productor, 'h4', 'Preparar un anuncio', '[role=tabpanel]')
}

const panelAnuncios = '[role=tabpanel]:not([hidden]) details'
function tarjeta(titulo, elem) {
  return `(() => {
    const a = (${elemento('article', titulo, panelAnuncios)});
    if (a) a.dataset.gcE2e = 'actual';
    return a?.querySelector(${literal(elem)}) != null;
  })()`
}
async function guardarAnuncio(titulo, ubicacion, foto) {
  await campoConEtiqueta(productor, 'Título', 'input', titulo, panelAnuncios)
  await campoConEtiqueta(productor, 'Ubicación general', 'input', ubicacion, panelAnuncios)
  await campoConEtiqueta(productor, 'Precio solicitado en PEN', 'input', '100', panelAnuncios)
  await campoConEtiqueta(productor, 'Características públicas', 'textarea', 'Descripción sintética autorizada', panelAnuncios)
  await boton(productor, 'Guardar anuncio', panelAnuncios)
  await hasta(() => productor.evaluar(tarjeta(titulo, 'input[type=file]')), 'No se creó el anuncio')
  if (foto) {
    const { root } = await productor.cdp('DOM.getDocument')
    const { nodeId } = await productor.cdp('DOM.querySelector', {
      nodeId: root.nodeId, selector: 'article[data-gc-e2e=actual] input[type=file]',
    })
    await productor.cdp('DOM.setFileInputFiles', { nodeId, files: [foto] })
    await texto(productor, 'Foto guardada.', panelAnuncios)
  }
  await boton(productor, 'Publicar', 'article[data-gc-e2e=actual]')
  await esperarElemento(productor, 'article[data-gc-e2e=actual] p', 'Publicado')
  await productor.evaluar('document.querySelector("article[data-gc-e2e=actual]")?.removeAttribute("data-gc-e2e")')
}

async function catalogoPublico(p, ubicacion) {
  if (await p.evaluar(`location.href === ${literal(origen + '/#catalogo')}`)) await p.abrir('about:blank')
  await p.abrir(origen + '/#catalogo')
  await pulsar(p, 'summary', 'Ver anuncios de ganado', '#catalogo')
  await campoConEtiqueta(p, 'Ubicación general', 'input', ubicacion, '#catalogo')
  await boton(p, 'Buscar anuncios', '#catalogo')
}

async function catalogo() {
  await abrirAnuncios(segundo)
  const titulo = `Anuncio con foto ${id}`
  const png = join(carpeta, 'foto-sintetica.png')
  const base64 = await productor.evaluar(`(() => {
    const c=document.createElement('canvas'); c.width=240; c.height=120;
    const g=c.getContext('2d'); g.fillStyle='#315d47'; g.fillRect(0,0,240,120);
    g.fillStyle='#fffdf7'; g.font='18px sans-serif'; g.fillText('FOTO DE PRUEBA',32,64);
    return c.toDataURL('image/png').split(',')[1];
  })()`)
  await writeFile(png, Buffer.from(base64, 'base64'))
  await guardarAnuncio(titulo, zona, png)
  const ruta = anuncioRuta(idSegundo)
  // Volumen para paginar mediante los mismos endpoints privados sobre PostgreSQL.
  const ids = await productor.evaluar('Array.from({length:20}, () => crypto.randomUUID())')
  const extras = ids.flatMap((anuncio, i) => [
    { ruta, metodo: 'POST', datos: { id: anuncio, version: 0, titulo: `Página E2E ${i + 1}`,
      caracteristicas: 'Ficha sintética', ubicacionGeneral: zona, precioPen: 101,
      autorizaContacto: false, genealogiaAutorizada: false } },
    { ruta: `${ruta}/${anuncio}/estado`, metodo: 'POST', datos: { accion: 'PUBLICAR', version: 1 } },
  ])
  await api(...extras)
  const publico = await navegador.pagina()
  try {
    await catalogoPublico(publico, zona)
    await boton(publico, 'Siguiente', '#catalogo')
    await esperarElemento(publico, 'span', 'Página 2', '#catalogo')
    await esperarElemento(publico, 'h3', titulo, '#catalogo')
    await hasta(() => publico.evaluar(`(() => {
      const img=document.querySelector('#catalogo img');
      img?.scrollIntoView({block:'center',behavior:'instant'});
      return img?.complete && img.naturalWidth===240;
    })()`), 'No se pudo cargar la foto pública real')
    const verificacion = await publico.evaluar(`(async () => {
      const datos = await (await fetch('/api/v1/catalogo?pagina=2&ubicacion='+encodeURIComponent(${literal(zona)}),
        {credentials:'omit',cache:'no-store'})).json();
      const anuncio = datos.items[0];
      const historial = await fetch(${literal(`${rutaFarm}/animales/${idSegundo}/eventos-sanitarios`)}, {credentials:'omit'});
      return { titulo:anuncio.titulo, invisible:!JSON.stringify(anuncio).includes(${literal(privado)}),
        sinCamposPrivados:!['animalId','ganaderiaId','observaciones','historial','contacto'].some(c=>anuncio[c]!=null),
        historial:historial.status };
    })()`)
    assert.deepEqual(verificacion, { titulo, invisible: true, sinCamposPrivados: true, historial: 401 })
    await captura(publico, '08-catalogo-pagina-2')
    await boton(publico, 'Anterior', '#catalogo')
    await esperarElemento(publico, 'span', 'Página 1', '#catalogo')
    await campoConEtiqueta(publico, 'Sexo', 'select', 'MACHO', '#catalogo')
    await boton(publico, 'Buscar anuncios', '#catalogo')
    await texto(publico, 'No se encontraron anuncios', '#catalogo')
    await hasta(() => productor.evaluar(tarjeta(titulo, 'button')), 'No se encontró la tarjeta para pausar')
    await boton(productor, 'Pausar', 'article[data-gc-e2e=actual]')
    await esperarElemento(productor, 'article[data-gc-e2e=actual] p', 'Pausado')
    const sinFoto = await publico.evaluar(`(async () => {
      const r=await fetch('/api/v1/catalogo?pagina=2&ubicacion='+encodeURIComponent(${literal(zona)}),
        {credentials:'omit',cache:'no-store'});
      return r.ok && (await r.json()).items.length===0;
    })()`)
    assert.equal(sinFoto, true)
    await catalogoPublico(publico, zona)
    await esperarElemento(publico, '#catalogo article h3', 'Página E2E')
    assert.equal(await publico.evaluar(`Boolean(${elemento('button', 'Siguiente', '#catalogo')})`), false)
  } finally { await publico.cerrar() }
}

async function venta() {
  await abrirAnuncios(primero)
  const ubicacion = `Venta sintética ${id}`
  for (const n of [1, 2]) await guardarAnuncio(`Venta E2E ${n} ${id}`, ubicacion)
  const publico = await navegador.pagina()
  try {
    await catalogoPublico(publico, ubicacion)
    await esperarElemento(publico, 'h3', `Venta E2E 1 ${id}`, '#catalogo')
    await esperarElemento(publico, 'h3', `Venta E2E 2 ${id}`, '#catalogo')
    await boton(productor, 'Registrar como vendido', '[role=tabpanel]:not([hidden])')
    await pulsar(productor, '[role=tab]', 'General')
    await texto(productor, 'Vendido', '.animal-detail')
    await pulsar(productor, '[role=tab]', 'Anuncios')
    await pulsar(productor, 'summary', 'Anuncios de este animal')
    await esperarElemento(productor, 'article p', 'Cerrado por venta', panelAnuncios)
    assert.equal(await productor.evaluar(`[...document.querySelectorAll(${literal(panelAnuncios + ' article p')})]
      .filter(e=>e.innerText.includes('Cerrado por venta')).length`), 2)
    await captura(productor, '09-anuncios-cerrados')
    await catalogoPublico(publico, ubicacion)
    await texto(publico, 'No se encontraron anuncios', '#catalogo')
    assert.equal(await publico.evaluar(`(async () => {
      const r=await fetch('/api/v1/catalogo/reportes-disponibles',{credentials:'omit'});
      return r.ok && (await r.json()).recepcionExperimental===false;
    })()`), true, 'Los reportes deben seguir desactivados')
    assert.equal(await productor.evaluar(`Boolean(${elemento('summary', 'Moderación del catálogo')})`), false)
    await pulsar(productor, '[role=tab]', 'Sanidad')
    await texto(productor, vacuna, '.vaccinations-panel')
    await pulsar(productor, '[role=tab]', 'General')
    await texto(productor, privado, '.animal-detail')
  } finally { await publico.cerrar() }
}

async function interfaz() {
  for (const ancho of [1440, 768, 360]) {
    const p = await navegador.pagina({ ancho, alto: ancho === 360 ? 800 : 980, movil: ancho === 360 })
    const comprobarAncho = async () => {
      const vista = await p.evaluar(`({ancho:innerWidth, util:document.documentElement.clientWidth,
        desplazamiento:document.documentElement.scrollWidth})`)
      assert.equal(vista.ancho, ancho, 'Viewport distinto del esperado')
      assert.ok(vista.desplazamiento <= vista.util, `Desborde a ${ancho} px: ${vista.desplazamiento} > ${vista.util}`)
    }
    try {
      await iniciar(p)
      await seleccionarFarm(p)
      await navegar(p, 'ganado', ancho)
      await esperarElemento(p, 'h3', 'Mis animales')
      for (const selector of ['#animal-sexo', '#animal-nombre', '#animal-observaciones']) await esperarElemento(p, selector)
      await comprobarAncho()
      await ficha(p, primero)
      for (const tab of ['General', 'Sanidad', 'Reproducción', 'Peso', 'Genealogía', 'Anuncios']) {
        await pulsar(p, '[role=tab]', tab)
        await esperarElemento(p, '[role=tab][aria-selected=true]', tab)
        await comprobarAncho()
      }
      await pulsar(p, '[role=tab]', 'General')
      await p.cdp('Input.dispatchKeyEvent', { type: 'rawKeyDown', key: 'ArrowRight', code: 'ArrowRight' })
      await p.cdp('Input.dispatchKeyEvent', { type: 'keyUp', key: 'ArrowRight', code: 'ArrowRight' })
      await esperarElemento(p, '[role=tab][aria-selected=true]:focus', 'Sanidad')
      assert.ok(await p.evaluar(`[...document.querySelectorAll('[role=tab]')].every(tab =>
        tab.id && document.getElementById(tab.getAttribute('aria-controls'))?.getAttribute('aria-labelledby')===tab.id)`))
      await captura(p, `ux-${ancho}-ficha`)
      for (const destino of ['sanidad', 'reproduccion', 'produccion', 'calendario', 'anuncios', 'familia', 'inicio']) {
        await navegar(p, destino, ancho)
        await comprobarAncho()
        assert.equal(await p.evaluar(`Boolean(${elemento('[role=alert]', '')})`), false, `Alerta visible en ${destino}`)
      }
      assert.equal(p.excepciones.length, 0, `Excepciones JavaScript a ${ancho} px`)
    } finally { await p.cerrar() }
  }
}

async function ejecutar(prueba, accion) {
  try {
    await accion()
    resultados.push({ prueba, resultado: 'aprobada' })
    console.log(`OK    ${prueba}`)
  } catch (error) {
    resultados.push({ prueba, resultado: 'fallida', detalle: error.message })
    console.log(`FALLO ${prueba}: ${error.message}`)
  }
}

try {
  await ejecutar('Preparación por HTTPS', preparar)
  if (resultados[0].resultado === 'aprobada') {
    if (!solo || solo === '07') await ejecutar('SPEC-07', campana)
    if (!solo || solo === '08') await ejecutar('SPEC-08', catalogo)
    if (!solo || solo === '09') {
      if (solo === '09') await ejecutar('Preparación sanitaria de SPEC-09', () => campana(false))
      await ejecutar('SPEC-09', venta)
    }
    if (!solo || solo === 'ux') await ejecutar('UX/UI', interfaz)
  }
} finally {
  navegador.cerrar()
  await writeFile(join(carpeta, 'resultado.json'), JSON.stringify({ origen, base, resultados }, null, 2) + '\n')
  console.log(`Evidencias: .cache/e2e/${id}`)
  process.exitCode = resultados.some(r => r.resultado === 'fallida') ? 1 : 0
}
