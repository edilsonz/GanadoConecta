import { useEffect, useState } from 'react'

type ConnectionState = 'checking' | 'available' | 'unavailable'

export function ApiStatus() {
  const [state, setState] = useState<ConnectionState>('checking')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    setState('checking')
    const timeout = window.setTimeout(() => controller.abort(), 5000)

    async function check() {
      try {
        const response = await fetch('/api/health', { signal: controller.signal, cache: 'no-store' })
        const body: unknown = await response.json()
        const healthy = response.ok && typeof body === 'object' && body !== null
          && 'status' in body && body.status === 'UP'
        if (active) setState(healthy ? 'available' : 'unavailable')
      } catch {
        if (active) setState('unavailable')
      } finally {
        window.clearTimeout(timeout)
      }
    }
    void check()
    return () => {
      active = false
      window.clearTimeout(timeout)
      controller.abort()
    }
  }, [attempt])

  const messages = {
    checking: 'Comprobando conexión…',
    available: 'Conexión con el servicio disponible',
    unavailable: 'No se pudo comprobar la conexión',
  }

  return (
    <section className="connection-panel" id="conexion" aria-labelledby="connection-title">
      <div>
        <p className="eyebrow">ESTADO DE CONEXIÓN</p>
        <h2 id="connection-title">Un primer paso para conectar.</h2>
        <p className={`connection-message ${state}`} role="status" aria-live="polite">
          <span className="status-dot" aria-hidden="true" />{messages[state]}
        </p>
        <p className="subtle">Esta comprobación no inicia una sesión ni carga información de animales.</p>
      </div>
      <button className="button button-outline" disabled={state === 'checking'} onClick={() => setAttempt(value => value + 1)}>
        Comprobar de nuevo <span aria-hidden="true">↗</span>
      </button>
    </section>
  )
}
