import { spawn } from 'node:child_process'
import { createHash, X509Certificate } from 'node:crypto'
import { mkdtemp, readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { setTimeout as esperar } from 'node:timers/promises'

export async function hasta(probar, descripcion, plazo = 10_000) {
  const fin = Date.now() + plazo
  do {
    const valor = await probar()
    if (valor) return valor
    await esperar(100)
  } while (Date.now() < fin)
  throw Error(descripcion)
}

export async function abrirChrome(exe, certificado) {
  const perfil = await mkdtemp(join(tmpdir(), 'ganadoconecta-chrome-e2e-'))
  const certificadoLocal = new X509Certificate(await readFile(certificado))
  const pin = createHash('sha256').update(certificadoLocal.publicKey.export({ type: 'spki', format: 'der' })).digest('base64')
  const proceso = spawn(exe, ['--headless', '--disable-gpu', '--no-first-run', '--disable-background-networking',
    '--remote-debugging-port=0', `--user-data-dir=${perfil}`, `--ignore-certificate-errors-spki-list=${pin}`,
    'about:blank'], { stdio: 'ignore', windowsHide: true })
  let arranqueFallido = false
  proceso.once('error', () => { arranqueFallido = true })
  let ws
  try {
    const puerto = await hasta(async () => {
      if (arranqueFallido || proceso.exitCode !== null) throw Error('Chrome terminó antes de iniciar la prueba')
      return readFile(join(perfil, 'DevToolsActivePort'), 'utf8').catch(() => '')
    }, 'Chrome no abrió DevTools', 25_000)
    const [numero, ruta] = puerto.trim().split(/\r?\n/)
    ws = new WebSocket(`ws://127.0.0.1:${numero}${ruta}`)
    await new Promise((resolve, reject) => {
      ws.addEventListener('open', resolve, { once: true })
      ws.addEventListener('error', () => reject(Error('No se pudo conectar con Chrome')), { once: true })
    })
    let indice = 0
    const pendientes = new Map()
    const eventos = new Map()
    ws.addEventListener('message', ({ data }) => {
      const mensaje = JSON.parse(data)
      if (mensaje.id) {
        pendientes.get(mensaje.id)?.(mensaje)
        pendientes.delete(mensaje.id)
      } else eventos.get(mensaje.sessionId)?.(mensaje)
    })
    function enviar(method, params = {}, sessionId) {
      return new Promise((resolve, reject) => {
        const id = ++indice
        const tiempo = setTimeout(() => {
          pendientes.delete(id)
          reject(Error(`Chrome no respondió a ${method}`))
        }, 18_000)
        pendientes.set(id, mensaje => {
          clearTimeout(tiempo)
          if (mensaje.error) reject(Error(`Chrome rechazó ${method}`))
          else resolve(mensaje.result)
        })
        ws.send(JSON.stringify({ id, method, params, sessionId }))
      })
    }
    async function pagina({ ancho = 1440, alto = 980, movil = false } = {}) {
      const { browserContextId } = await enviar('Target.createBrowserContext')
      const { targetId } = await enviar('Target.createTarget', { url: 'about:blank', browserContextId })
      const { sessionId } = await enviar('Target.attachToTarget', { targetId, flatten: true })
      const cdp = (metodo, params) => enviar(metodo, params, sessionId)
      const excepciones = []
      eventos.set(sessionId, ({ method, params }) => {
        if (method === 'Page.javascriptDialogOpening') {
          void cdp('Page.handleJavaScriptDialog', { accept: true })
        }
        if (method === 'Runtime.exceptionThrown') excepciones.push(params.exceptionDetails.text)
      })
      await cdp('Page.enable')
      await cdp('Runtime.enable')
      await cdp('Emulation.setDeviceMetricsOverride', {
        width: ancho, height: alto, deviceScaleFactor: 1, mobile: movil, screenWidth: ancho, screenHeight: alto,
      })
      return {
        excepciones,
        cdp,
        async evaluar(expression) {
          const { result, exceptionDetails } = await cdp('Runtime.evaluate',
            { expression, awaitPromise: true, returnByValue: true, userGesture: true })
          if (exceptionDetails) throw Error('Falló una comprobación dentro de la página')
          return result.value
        },
        async abrir(url) {
          const respuesta = await cdp('Page.navigate', { url })
          if (respuesta.errorText) throw Error(`Chrome no pudo abrir el origen HTTPS de pruebas (${respuesta.errorText})`)
        },
        async captura(rutaArchivo) {
          const { data } = await cdp('Page.captureScreenshot', { format: 'png', fromSurface: true })
          await writeFile(rutaArchivo, Buffer.from(data, 'base64'))
        },
        cerrar: () => enviar('Target.disposeBrowserContext', { browserContextId }),
      }
    }
    return { pagina, cerrar: () => { ws.close(); proceso.kill() } }
  } catch (error) {
    ws?.close()
    proceso.kill()
    throw error
  }
}

export const literal = JSON.stringify

export function elemento(selector, texto = '', ambito = 'body') {
  return `[...document.querySelectorAll(${literal(ambito)})].flatMap(
    raiz => [...raiz.querySelectorAll(${literal(selector)})]).find(
    e => e.checkVisibility({checkVisibilityCSS:true}) && e.getClientRects().length &&
      e.textContent.replace(/\\s+/g,' ').trim().includes(${literal(texto)}))`
}

export const esperarElemento = (p, selector, texto, ambito) =>
  hasta(() => p.evaluar(`Boolean(${elemento(selector, texto, ambito)})`), `No apareció ${texto || selector}`)

export async function pulsar(p, selector, texto, ambito) {
  const punto = await hasta(() => p.evaluar(`(() => {
    const e = ${elemento(selector, texto, ambito)};
    if (!e || e.matches(':disabled')) return null;
    e.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});
    const r = e.getBoundingClientRect();
    const x = r.left + r.width / 2, y = r.top + r.height / 2;
    return e.contains(document.elementFromPoint(x,y)) ? {x,y} : null;
  })()`), `Control inaccesible: ${texto || selector}`)
  await p.cdp('Input.dispatchMouseEvent', { type: 'mousePressed', ...punto, button: 'left', clickCount: 1 })
  await p.cdp('Input.dispatchMouseEvent', { type: 'mouseReleased', ...punto, button: 'left', clickCount: 1 })
}

export async function campo(p, selector, valor, ambito) {
  await esperarElemento(p, selector, '', ambito)
  return p.evaluar(`(() => {
    const e = ${elemento(selector, '', ambito)};
    if (e.disabled) return false;
    const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(e), 'value').set;
    setter.call(e, ${literal(valor)});
    e.dispatchEvent(new Event(e instanceof HTMLSelectElement ? 'change' : 'input', {bubbles:true}));
    return e.value === ${literal(valor)};
  })()`)
}
