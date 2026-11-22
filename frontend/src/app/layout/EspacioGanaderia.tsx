import { useState } from 'react'
import { AccesoFamiliarPanel } from '../../features/ganaderias/AccesoFamiliarPanel'
import { AnimalesPanel } from '../../features/ganado/AnimalesPanel'
import { AnalisisPanel } from '../../features/analisis/AnalisisPanel'
import { ReportesPanel } from '../../features/reportes/ReportesPanel'
import type { Ganaderia, RolGanaderia } from '../../features/ganaderias/ganaderiasApi'
import type { SesionActiva } from '../../features/cuenta/sesionApi'
import { InicioGanaderia } from './InicioGanaderia'
import './espacioGanaderia.css'

const vistas = {
  inicio: 'Inicio',
  ganado: 'Mi ganado',
  sanidad: 'Sanidad',
  reproduccion: 'Reproducción',
  produccion: 'Producción',
  calendario: 'Calendario',
  reportes: 'Reportes',
  analisis: 'Análisis y alertas',
  anuncios: 'Anuncios',
  familia: 'Acceso familiar',
} as const

export type VistaGanaderia = keyof typeof vistas

const grupos = [
  { nombre: 'Trabajo diario', secciones: ['inicio', 'ganado', 'sanidad', 'reproduccion', 'produccion', 'calendario'] },
  { nombre: 'Seguimiento', secciones: ['reportes', 'analisis'] },
  { nombre: 'Gestión', secciones: ['anuncios', 'familia'] },
] as const satisfies readonly { nombre: string; secciones: readonly VistaGanaderia[] }[]

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
  const vistaPropia = vistaActual === 'reportes' || vistaActual === 'analisis'
  const gruposVisibles = grupos.map(grupo => ({
    nombre: grupo.nombre,
    secciones: grupo.secciones.filter(id => disponibles.includes(id)),
  })).filter(grupo => grupo.secciones.length)

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
          {gruposVisibles.map(grupo => <div className="sidebar-group" key={grupo.nombre}>
            <p>{grupo.nombre}</p>
            {grupo.secciones.map(id => <button key={id} type="button"
              aria-current={id === vistaActual ? 'page' : undefined}
              onClick={() => navegar(id)}>{vistas[id]}</button>)}
          </div>)}
        </nav>
        <label className="producer-mobile-nav form-field">Ir a sección
          <select value={vistaActual} onChange={event => navegar(event.target.value as VistaGanaderia)}>
            {gruposVisibles.map(grupo => <optgroup key={grupo.nombre} label={grupo.nombre}>
              {grupo.secciones.map(id => <option key={id} value={id}>{vistas[id]}</option>)}
            </optgroup>)}
          </select>
        </label>
        <div className="producer-content">
          {vistaActual === 'inicio' && <InicioGanaderia ganaderiaId={ganaderia.id} token={sesion.accessToken}
            conectado={conectado} accesoVigente={rolEnLinea !== null} navegar={navegar}
            onSesionInvalida={onSesionInvalida} />}
          <div hidden={vistaActual === 'inicio' || vistaActual === 'familia' || vistaPropia}>
            <AnimalesPanel ganaderia={ganaderia} sesion={sesion} vista={vistaPropia ? 'inicio' : vistaActual}
              onSesionInvalida={onSesionInvalida} onRolEnLineaChange={setRolEnLinea} />
          </div>
          {vistaActual === 'reportes' && (conectado
            ? <ReportesPanel key={ganaderia.id} ganaderiaId={ganaderia.id}
                token={sesion.accessToken} onSesionInvalida={onSesionInvalida} />
            : <section className="producer-home"><h2>Reportes</h2>
                <p>Recupera la conexión para consultar los registros confirmados.</p></section>)}
          {vistaActual === 'analisis' && (conectado
            ? <AnalisisPanel key={ganaderia.id} ganaderiaId={ganaderia.id}
                token={sesion.accessToken} onSesionInvalida={onSesionInvalida} />
            : <section className="producer-home"><h2>Análisis y alertas</h2>
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
