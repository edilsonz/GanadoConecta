import { useState } from 'react'
import { AccesoFamiliarPanel } from '../../features/ganaderias/AccesoFamiliarPanel'
import { AnimalesPanel } from '../../features/ganado/AnimalesPanel'
import { ReportesPanel } from '../../features/reportes/ReportesPanel'
import type { Ganaderia, RolGanaderia } from '../../features/ganaderias/ganaderiasApi'
import type { SesionActiva } from '../../features/cuenta/sesionApi'
import './espacioGanaderia.css'

const vistas = {
  inicio: 'Inicio',
  ganado: 'Mi ganado',
  sanidad: 'Sanidad',
  reproduccion: 'Reproducción',
  produccion: 'Producción',
  calendario: 'Calendario',
  reportes: 'Reportes',
  anuncios: 'Anuncios',
  familia: 'Acceso familiar',
} as const

export type VistaGanaderia = keyof typeof vistas

type Props = {
  ganaderia: Ganaderia
  ganaderias: Ganaderia[]
  sesion: SesionActiva
  conectado: boolean
  onSeleccionar: (id: string) => void
  onSesionInvalida: () => void
}

export function EspacioGanaderia({ ganaderia, ganaderias, sesion, conectado, onSeleccionar, onSesionInvalida }: Props) {
  const [vista, setVista] = useState<VistaGanaderia>('inicio')
  const [familiaVisitada, setFamiliaVisitada] = useState(false)
  const [rolEnLinea, setRolEnLinea] = useState<RolGanaderia | null>(null)
  const propietario = ganaderia.rol === 'PROPIETARIO' && rolEnLinea === 'PROPIETARIO'
  const disponibles = (Object.keys(vistas) as VistaGanaderia[]).filter(id =>
    propietario || (id !== 'familia' && id !== 'anuncios'))
  const vistaActual = disponibles.includes(vista) ? vista : 'inicio'

  function navegar(destino: VistaGanaderia) {
    if (destino === 'familia') setFamiliaVisitada(true)
    setVista(destino)
  }

  return (
    <div className="farm-selected-workspace">
      <header className="producer-header">
        <div>
          <p className="eyebrow">ESPACIO DEL PRODUCTOR</p>
          <h1 id="producer-title">{ganaderia.nombre}</h1>
        </div>
        <label className="form-field">Ganadería activa
          <select value={ganaderia.id} onChange={event => onSeleccionar(event.target.value)}>
            {ganaderias.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}
          </select>
        </label>
      </header>
      <div className="producer-layout">
        <nav className="producer-sidebar" aria-label="Secciones de la ganadería">
          {disponibles.map(id => <button key={id} type="button"
            aria-current={id === vistaActual ? 'page' : undefined}
            onClick={() => navegar(id)}>{vistas[id]}</button>)}
        </nav>
        <label className="producer-mobile-nav form-field">Ir a sección
          <select value={vistaActual} onChange={event => navegar(event.target.value as VistaGanaderia)}>
            {disponibles.map(id => <option key={id} value={id}>{vistas[id]}</option>)}
          </select>
        </label>
        <div className="producer-content">
          <section className="producer-home" hidden={vistaActual !== 'inicio'} aria-labelledby="producer-home-title">
            <h2 id="producer-home-title">Inicio</h2>
            <p>Consulta las fichas de tu ganadería o revisa las actividades programadas.</p>
            <div className="producer-actions">
              <button type="button" className="button button-primary" onClick={() => navegar('ganado')}>Ver animales</button>
              <button type="button" className="button button-outline" onClick={() => navegar('calendario')}>Ver calendario</button>
            </div>
          </section>
          <div hidden={vistaActual === 'inicio' || vistaActual === 'familia' || vistaActual === 'reportes'}>
            <AnimalesPanel ganaderia={ganaderia} sesion={sesion} vista={vistaActual === 'reportes' ? 'inicio' : vistaActual}
              onSesionInvalida={onSesionInvalida} onRolEnLineaChange={setRolEnLinea} />
          </div>
          {vistaActual === 'reportes' && (conectado
            ? <ReportesPanel key={ganaderia.id} ganaderiaId={ganaderia.id}
                token={sesion.accessToken} onSesionInvalida={onSesionInvalida} />
            : <section className="producer-home"><h2>Reportes</h2>
                <p>Recupera la conexión para consultar los registros confirmados.</p></section>)}
          {propietario && familiaVisitada && <div hidden={vistaActual !== 'familia'}>
            <AccesoFamiliarPanel ganaderiaId={ganaderia.id}
              token={sesion.accessToken} onSesionInvalida={onSesionInvalida} />
          </div>}
        </div>
      </div>
    </div>
  )
}
