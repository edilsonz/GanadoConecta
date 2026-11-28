import assert from 'node:assert/strict'
import { randomBytes, randomUUID } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { abrirChrome, campo, elemento, esperarElemento, hasta, literal, pulsar } from './chrome.mjs'

const base = process.env.GC_E2E_DB ?? ''
const origen = process.env.GC_E2E_URL ?? 'https://127.0.0.1:15173'
const cert = process.env.GC_E2E_CERT
const revocacionOffline = process.argv.includes('--revocacion-offline')
const filtro = revocacionOffline || process.argv.includes('--familia') ? /^(A · registro|B ·|C ·)/
  : process.argv.includes('--alertas') ? /^(A · registro|B ·|L ·)/ : null
assert.match(base, /^ganadoconecta_e2e_[a-z0-9]+$/)
assert.ok(/^https:\/\/127\.0\.0\.1:(?!5173$)\d+$/.test(origen) && cert)
const tag = randomBytes(5).toString('hex')
const carpeta = `.cache/e2e-auditoria/${tag}`
await mkdir(carpeta, { recursive: true })

const resultados = []
const navegador = await abrirChrome(process.env.GC_E2E_CHROME ?? 'C:/Program Files/Google/Chrome/Application/chrome.exe', cert)
const cuenta = () => ({ correo: `auditoria-${randomBytes(7).toString('hex')}@example.invalid`, clave: randomBytes(24).toString('base64url') })
const titular = cuenta(), familiar = cuenta()
const nombreFinca = 'Ganadería sintética ' + tag
const madre = 'Madre sintética', cria = 'Cría sintética', lote = 'Alta por lote', local = 'Alta desde IndexedDB'
const boton = (p, texto, ambito) => pulsar(p, 'button', texto, ambito)
const texto = (p, valor, ambito) => esperarElemento(p, 'p,strong,dd,li,td', valor, ambito)
const dato = async (p, selector, valor, ambito) => assert.ok(await campo(p, selector, valor, ambito), `Control no editable: ${selector}`)
const nav = (p, destino) => boton(p, destino, '.producer-sidebar')

async function pagina() {
  const p = await navegador.pagina()
  // Guarda el bearer en memoria del perfil de prueba. La API y PostgreSQL responden normalmente.
  await p.cdp('Page.addScriptToEvaluateOnNewDocument', { source: `(() => {
    const enviar = window.fetch.bind(window);
    let auth, alta, intentosAlta = 0;
    window.fetch = (url, init) => {
      const credencial = new Headers(init?.headers).get('Authorization');
      if (credencial && String(url).startsWith('/api/v1/')) auth = credencial;
      if (init?.method === 'POST' && String(url).endsWith('/animales')) {
        alta = [url, init.body];
        intentosAlta++;
      }
      return enviar(url, init);
    };
    window.gcAuditoria = async (ruta, metodo, datos) => {
      if (!ruta.startsWith('/api/v1/ganaderias/') && ruta !== '/api/v1/invitaciones/aceptar') {
        throw Error('Ruta sintética fuera del ámbito');
      }
      const h = new Headers(auth ? {Authorization:auth} : {});
      if (metodo) {
        const r = await enviar('/api/v1/auth/csrf', {cache:'no-store', credentials:'same-origin'});
        h.set('X-CSRF-TOKEN', (await r.json()).token);
        h.set('Content-Type', 'application/json');
      }
      const r = await enviar(ruta,{method:metodo ?? 'GET',headers:h,cache:'no-store',credentials:'same-origin',
        body:datos ? JSON.stringify(datos) : undefined});
      return {estado:r.status, datos:await r.json().catch(()=>null)};
    };
    window.gcAuditoriaRepetir = () => alta && window.gcAuditoria(alta[0], 'POST', JSON.parse(alta[1]));
    window.gcAuditoriaIntentosAlta = () => intentosAlta;
  })()` })
  return p
}

const api = (p, ruta, metodo, datos) => p.evaluar(`window.gcAuditoria(${literal(ruta)},${literal(metodo) ?? 'undefined'},${literal(datos) ?? 'undefined'})`)
async function registrar(p, actor) {
  await p.abrir(origen)
  await dato(p, '#registro-correo', actor.correo)
  await dato(p, '#registro-contrasena', actor.clave)
  await dato(p, '#registro-confirmacion', actor.clave)
  await boton(p, 'Crear mi cuenta', '#crear-cuenta')
  await texto(p, 'Cuenta creada correctamente', '#crear-cuenta')
}
async function iniciar(p, actor) {
  await dato(p, '#login-correo', actor.correo)
  await dato(p, '#login-contrasena', actor.clave)
  await boton(p, 'Iniciar sesión', '#iniciar-sesion')
  await esperarElemento(p, '#iniciar-sesion .session-email')
}
async function seleccionar(p) {
  await pulsar(p, '.farm-option', nombreFinca)
  await esperarElemento(p, '#producer-title', nombreFinca)
}
async function alta(p, nombre, sexo = 'HEMBRA') {
  await dato(p, '#animal-sexo', sexo)
  await dato(p, '#animal-nombre', nombre)
  await boton(p, 'Registrar animal', 'form.animal-form')
}
async function ficha(p, nombre) {
  if (await p.evaluar(`Boolean(${elemento('.animal-detail-heading button', 'Cerrar')})`)) {
    await boton(p, 'Cerrar', '.animal-detail-heading')
  }
  await pulsar(p, '.animal-option', nombre)
  await esperarElemento(p, '.animal-detail-heading h4', nombre)
}

function operacionesLocales(p, incluirPayload = false) {
  return p.evaluar(`new Promise((resolve,reject)=>{
    const r=indexedDB.open('ganadoconecta-offline'); r.onerror=()=>reject(r.error);
    r.onsuccess=()=>{ const db=r.result;const q=db.transaction('operacionesPendientes').objectStore('operacionesPendientes').getAll();
      q.onsuccess=()=>{resolve(q.result.filter(x=>x.payload?.nombre===${literal(local)}).map(x=>({
        estado:x.estado,id:x.operationId,...(${incluirPayload} ? {payload:x.payload} : {})
      })));db.close()};
      q.onerror=()=>reject(q.error);
    };
  })`)
}

const cambiarRed = (p, offline) => p.cdp('Network.emulateNetworkConditions', {
  offline, latency: 0, downloadThroughput: offline ? 0 : -1, uploadThroughput: offline ? 0 : -1,
})

async function caso(id, ejecutar) {
  if (filtro && !filtro.test(id)) return
  try {
    await ejecutar()
    resultados.push({ id, resultado: 'APROBADO' })
    console.log(`OK ${id}`)
  } catch (e) {
    resultados.push({ id, resultado: 'FALLIDO', detalle: e.message })
    console.error(`FALLO ${id}: ${e.message}`)
  }
}

const titularTab = await pagina()
let segundaTab
let ruta, idMadre, idCria
try {
  await caso('A · registro, acceso y restauración HTTPS', async () => {
    await registrar(titularTab, titular)
    await iniciar(titularTab, titular)
    await titularTab.abrir(origen)
    await esperarElemento(titularTab, '#iniciar-sesion .session-email')
  })
  await caso('B · creación, listado y selección de ganadería', async () => {
    await dato(titularTab, '#ganaderia-nombre', nombreFinca)
    await boton(titularTab, 'Crear ganadería')
    await esperarElemento(titularTab, '#producer-title', nombreFinca)
    const id = await titularTab.evaluar('document.querySelector(".producer-header select").value')
    assert.match(id, /^[a-f\d-]{36}$/)
    ruta = `/api/v1/ganaderias/${id}`
    await titularTab.abrir(origen)
    await seleccionar(titularTab)
    assert.equal((await api(titularTab, ruta + '/acceso')).datos.rol, 'PROPIETARIO')
  })
  if (!ruta) throw Error('No se estableció una ganadería aislada')

  await caso('D · alta individual y lote mixto persistente', async () => {
    await nav(titularTab, 'Mi ganado')
    await boton(titularTab, 'Registrar animal', 'form.animal-form')
    await esperarElemento(titularTab, '#animal-error-sexo', 'Selecciona MACHO o HEMBRA.')
    await alta(titularTab, madre)
    await esperarElemento(titularTab, '.animal-option', madre)
    await alta(titularTab, cria)
    await esperarElemento(titularTab, '.animal-option', cria)
    await pulsar(titularTab, 'summary', 'Registrar varios animales')
    const tabla = '.bulk-table-scroll'
    await boton(titularTab, 'Añadir fila')
    await boton(titularTab, 'Añadir fila')
    await dato(titularTab, 'tbody tr:first-child select', 'MACHO', tabla)
    await dato(titularTab, 'tbody tr:first-child input', lote, tabla)
    await boton(titularTab, 'Revisar lote')
    await texto(titularTab, 'Requiere revisión.', tabla)
    await boton(titularTab, 'Confirmar 1 fila')
    await texto(titularTab, '1 de 2 filas confirmadas')
    await titularTab.captura(join(carpeta, 'lote.png'))
    await titularTab.abrir(origen)
    await seleccionar(titularTab)
    await nav(titularTab, 'Mi ganado')
    const { estado, datos } = await api(titularTab, ruta + '/animales')
    assert.equal(estado, 200)
    assert.equal(datos.items.filter(x => x.nombre === lote).length, 1)
    idMadre = datos.items.find(x => x.nombre === madre)?.animalId
    idCria = datos.items.find(x => x.nombre === cria)?.animalId
    assert.ok(idMadre && idCria && idMadre !== idCria)
  })

  await caso(revocacionOffline ? 'C · revocación remota con una alta en IndexedDB'
    : 'C · invitación, roles, acceso ajeno y revocación', async () => {
    segundaTab = await pagina()
    await registrar(segundaTab, familiar)
    await iniciar(segundaTab, familiar)
    assert.equal((await api(segundaTab, ruta + '/acceso')).estado, 403)
    const panel = '.farm-family-panel'
    const peticion = { animalId: randomUUID(), operationId: randomUUID(), tipo: 'CREAR_ANIMAL',
      schemaVersion: 2, baseVersion: 0, sexo: 'HEMBRA', nombre: 'Alta del editor sintético' }
    let editorId
    if (revocacionOffline) {
      const emision = await api(titularTab, ruta + '/familia/invitaciones', 'POST', { correo: familiar.correo, rol: 'EDITOR' })
      assert.equal(emision.estado, 201, 'No se creó la invitación sintética por la API')
      const aceptacion = await api(segundaTab, '/api/v1/invitaciones/aceptar', 'POST', { codigo: emision.datos.codigo })
      assert.equal(aceptacion.estado, 200)
      const directorio = await api(titularTab, ruta + '/familia')
      editorId = directorio.datos?.integrantes.find(x => x.correo === familiar.correo)?.usuarioId
      assert.match(editorId ?? '', /^[a-f\d-]{36}$/)
    } else {
      await nav(titularTab, 'Acceso familiar')
      await pulsar(titularTab, 'summary', 'Acceso familiar', panel)
      await dato(titularTab, 'input[type=email]', familiar.correo, panel)
      await boton(titularTab, 'Generar invitación', panel)
      const codigo = await hasta(() => titularTab.evaluar(`document.querySelector('${panel} input[readonly]')?.value`), 'No se emitió el código de prueba')
      await pulsar(segundaTab, 'summary', 'Aceptar invitación familiar')
      await dato(segundaTab, 'details[open] input', codigo)
      await boton(segundaTab, 'Aceptar invitación')
      await texto(segundaTab, 'Invitación aceptada.')
      await seleccionar(segundaTab)
      assert.equal((await api(segundaTab, ruta + '/acceso')).datos.rol, 'LECTOR')
      assert.equal((await api(segundaTab, ruta + '/animales', 'POST', peticion)).estado, 403)
      await boton(titularTab, 'Actualizar integrantes', panel)
      await texto(titularTab, `${familiar.correo} · LECTOR · ACTIVA`, panel)
      const selectorRol = 'div.form-field select:not(:disabled)'
      await esperarElemento(titularTab, selectorRol, '', panel)
      await titularTab.evaluar(`(() => {
        const select = ${elemento(selectorRol, '', panel)};
        select.value = 'EDITOR';
        select.dispatchEvent(new Event('change', { bubbles: true }));
      })()`)
      await texto(titularTab, `${familiar.correo} · EDITOR · ACTIVA`, panel)
    }
    assert.equal((await api(segundaTab, ruta + '/animales', 'POST', peticion)).estado, 201)
    let pendiente
    if (revocacionOffline) {
      await segundaTab.abrir(origen)
      await seleccionar(segundaTab)
      await nav(segundaTab, 'Mi ganado')
      await boton(segundaTab, 'Preparar trabajo sin conexión')
      await texto(segundaTab, 'Trabajo sin conexión preparado')
      await segundaTab.cdp('Network.enable')
      await cambiarRed(segundaTab, true)
    }
    try {
      if (revocacionOffline) {
        await hasta(() => segundaTab.evaluar('!navigator.onLine'), 'Chrome no entró en modo desconectado')
        await alta(segundaTab, local)
        await texto(segundaTab, local, '.local-pending-list')
        const operaciones = await operacionesLocales(segundaTab, true)
        assert.equal(operaciones.length, 1)
        pendiente = operaciones[0]
        assert.equal(pendiente.estado, 'PENDIENTE')
        assert.equal(pendiente.id, pendiente.payload.operationId)
      }
      if (revocacionOffline) {
        assert.equal((await api(titularTab, ruta + '/membresias/' + editorId, 'DELETE')).estado, 204)
      } else {
        await boton(titularTab, 'Revocar acceso', panel)
        await texto(titularTab, 'REVOCADA', panel)
      }
    } finally {
      if (revocacionOffline) await cambiarRed(segundaTab, false)
    }
    if (pendiente) {
      await hasta(() => segundaTab.evaluar('navigator.onLine'), 'Chrome no recuperó conexión')
      const intentosPrevios = await segundaTab.evaluar('window.gcAuditoriaIntentosAlta()')
      await boton(segundaTab, 'Sincronizar pendientes')
      await esperarElemento(segundaTab, '.animals-notice[role=status]', 'Tu acceso a esta ganadería cambió.')
      assert.equal(await segundaTab.evaluar('window.gcAuditoriaIntentosAlta()'), intentosPrevios,
        'La aplicación intentó enviar el alta tras revocar el acceso')
      const reenvio = await api(segundaTab, ruta + '/animales', 'POST', pendiente.payload)
      assert.equal(reenvio.estado, 403)
      const [conservada] = await operacionesLocales(segundaTab, true)
      assert.equal(conservada.id, pendiente.id)
      assert.notEqual(conservada.estado, 'SINCRONIZADO')
      assert.deepEqual(conservada.payload, pendiente.payload)
      assert.equal((await api(titularTab, ruta + '/animales/' + pendiente.payload.animalId)).estado, 404)
      assert.equal((await api(titularTab, ruta + '/animales/' + peticion.animalId)).estado, 200)
    }
    assert.equal((await api(segundaTab, ruta + '/animales')).estado, 403)
  })

  await caso('E · campaña de desparasitación y seguimiento por animal', async () => {
    assert.ok(idMadre && idCria)
    const fecha = new Intl.DateTimeFormat('en-CA', {timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date())
    const actividadId = randomUUID()
    const agenda = `${ruta}/agenda-sanitaria`
    const alta = await api(titularTab, agenda, 'POST', { actividadId, participantes: [idMadre, idCria],
      tipo: 'CAMPANA_DESPARASITACION', fechaPrevista: fecha, descripcion: 'Campaña sintética verificada' })
    assert.equal(alta.estado, 201)
    const cierre = { atendidos: [idMadre], fechaAtencion: fecha }
    const destino = `${agenda}/${actividadId}/completar`
    assert.equal((await api(titularTab, destino, 'POST', cierre)).estado, 200)
    assert.equal((await api(titularTab, destino, 'POST', cierre)).estado, 200)
    const eventos = await api(titularTab, `${ruta}/animales/${idMadre}/eventos-sanitarios`)
    const noAtendido = await api(titularTab, `${ruta}/animales/${idCria}/eventos-sanitarios`)
    assert.equal(eventos.estado, 200)
    assert.equal(noAtendido.estado, 200)
    assert.equal(eventos.datos.filter(x => x.tipo === 'DESPARASITACION').length, 1)
    assert.equal(noAtendido.datos.length, 0)
    await nav(titularTab, 'Mi ganado')
    await ficha(titularTab, madre)
    await boton(titularTab, 'Sanidad', '[role=tablist]')
    await texto(titularTab, 'Campaña sintética verificada')
  })

  await caso('F · monta, evaluación, parto y parentesco', async () => {
    assert.ok(idMadre && idCria)
    await nav(titularTab, 'Mi ganado')
    await ficha(titularTab, madre)
    await boton(titularTab, 'Reproducción', '[role=tablist]')
    const repro = '.reproduction-panel'
    await dato(titularTab, 'input[type=date]', '2026-09-01', repro)
    await boton(titularTab, 'Guardar evento reproductivo', repro)
    await texto(titularTab, 'Monta', repro)
    await dato(titularTab, 'form fieldset select', 'EVALUACION_GESTACION', repro)
    await dato(titularTab, 'input[type=date]', '2026-09-15', repro)
    await boton(titularTab, 'Guardar evento reproductivo', repro)
    await texto(titularTab, 'Evaluación de gestación', repro)
    const partos = '.births-panel'
    await dato(titularTab, 'input[type=date]', '2026-10-01', partos)
    await pulsar(titularTab, 'label.checkbox-field', cria, partos)
    await boton(titularTab, 'Guardar parto', partos)
    await texto(titularTab, 'Parto registrado.', partos)
    const genealogia = await api(titularTab, `${ruta}/animales/${idCria}/genealogia`)
    assert.equal(genealogia.estado, 200)
    assert.equal(genealogia.datos.ascendencia.find(x => x.relacion === 'MADRE')?.animalId, idMadre)
    await boton(titularTab, 'Genealogía', '[role=tablist]')
    await esperarElemento(titularTab, '.genealogy-panel span', cria)
    await titularTab.captura(join(carpeta, 'genealogia.png'))
  })

  await caso('G · pesajes, diferencia y gráfica', async () => {
    await nav(titularTab, 'Mi ganado')
    await ficha(titularTab, madre)
    await boton(titularTab, 'Peso', '[role=tablist]')
    const panel = 'section[aria-label="Peso"]'
    for (const [kilos, fecha] of [['120.25', '2026-09-01'], ['135.50', '2026-09-30']]) {
      await dato(titularTab, 'input[type=number]', kilos, panel)
      await dato(titularTab, 'input[type=date]', fecha, panel)
      await boton(titularTab, 'Guardar peso', panel)
      await esperarElemento(titularTab, 'ol strong', fecha, panel)
    }
    await esperarElemento(titularTab, '[role=img]', '', panel)
    await texto(titularTab, `${new Intl.NumberFormat('es-PE').format(15.25)} kg`, panel)
    await titularTab.captura(join(carpeta, 'peso.png'))
  })

  await caso('L · aviso de hoy y retiro al cerrar una actividad', async () => {
    await nav(titularTab, 'Mi ganado')
    const nombreAlerta = 'Animal para alerta sintética'
    await alta(titularTab, nombreAlerta)
    await esperarElemento(titularTab, '.animal-option', nombreAlerta)
    const lista = await api(titularTab, ruta + '/animales')
    const animalId = lista.datos.items.find(item => item.nombre === nombreAlerta)?.animalId
    assert.ok(animalId)
    const fechaPrevista = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
    }).format(new Date())
    const actividadId = randomUUID()
    const descripcion = 'Revisión sintética para hoy'
    const agenda = ruta + '/agenda-sanitaria'
    const programada = await api(titularTab, agenda, 'POST', {
      actividadId, animalId, fechaPrevista, descripcion, tipo: 'REVISION_VETERINARIA',
    })
    assert.equal(programada.estado, 201)
    await nav(titularTab, 'Análisis y alertas')
    await esperarElemento(titularTab, '.analisis-avisos strong', 'Para hoy')
    await texto(titularTab, descripcion, '.analisis-avisos')
    const cerrada = await api(titularTab, agenda + '/' + actividadId + '/completar', 'POST')
    assert.equal(cerrada.estado, 200)
    await boton(titularTab, 'Actualizar alertas')
    await texto(titularTab, 'No hay actividades de hoy o vencidas por atender.', '.analisis-tarjeta')
    const historial = await api(titularTab, ruta + '/animales/' + animalId + '/eventos-sanitarios')
    assert.equal(historial.estado, 200)
    assert.equal(historial.datos.length, 0)
  })

  await caso('M · IndexedDB, reconexión y reenvío sin duplicar', async () => {
    await nav(titularTab, 'Mi ganado')
    await boton(titularTab, 'Preparar trabajo sin conexión')
    await texto(titularTab, 'Trabajo sin conexión preparado')
    await titularTab.cdp('Network.enable')
    const red = offline => cambiarRed(titularTab, offline)
    let idOperacion
    await red(true)
    try {
      await hasta(() => titularTab.evaluar('!navigator.onLine'), 'Chrome no entró en modo desconectado')
      await alta(titularTab, local)
      await texto(titularTab, local, '.local-pending-list')
      const operaciones = await operacionesLocales(titularTab)
      assert.equal(operaciones.length, 1)
      assert.equal(operaciones[0].estado, 'PENDIENTE')
      assert.match(operaciones[0].id, /^[a-f\d-]{36}$/)
      idOperacion = operaciones[0].id
      assert.equal(await titularTab.evaluar(`Boolean(${elemento('.animal-option', local)})`), false)
      await titularTab.captura(join(carpeta, 'pendiente-offline.png'))
    } finally { await red(false) }
    await hasta(() => titularTab.evaluar('navigator.onLine'), 'Chrome no recuperó conexión')
    await boton(titularTab, 'Sincronizar pendientes')
    await esperarElemento(titularTab, '.animal-option', local)
    const reenvio = await titularTab.evaluar('window.gcAuditoriaRepetir()')
    assert.equal(reenvio.estado, 200)
    await titularTab.abrir(origen)
    await seleccionar(titularTab)
    const lista = await api(titularTab, ruta + '/animales')
    assert.equal(lista.datos.items.filter(x => x.nombre === local).length, 1)
    assert.deepEqual(await operacionesLocales(titularTab), [{ estado: 'SINCRONIZADO', id: idOperacion }])
  })

  await caso('A · cierre e imposibilidad de restaurar tras recarga', async () => {
    await boton(titularTab, 'Cerrar sesión', '#iniciar-sesion')
    await esperarElemento(titularTab, '#login-correo')
    await titularTab.abrir(origen)
    await esperarElemento(titularTab, '#login-correo')
    assert.equal((await api(titularTab, ruta + '/animales')).estado, 401)
  })
} finally {
  await titularTab.cerrar()
  if (segundaTab) await segundaTab.cerrar()
  navegador.cerrar()
  await writeFile(join(carpeta, 'resultado.json'), JSON.stringify({ resultados }, null, 2))
  console.log('Evidencia local: ' + carpeta)
}
if (resultados.some(r => r.resultado === 'FALLIDO')) process.exitCode = 1
