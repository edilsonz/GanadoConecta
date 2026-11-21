import { useCallback, useEffect, useState, useSyncExternalStore } from 'react'
import { ApiStatus } from './ApiStatus'
import { AccesoCuenta } from '../features/cuenta/AccesoCuenta'
import { GanaderiasPanel } from '../features/ganaderias/GanaderiasPanel'
import { RegistroCuenta } from '../features/cuenta/RegistroCuenta'
import { CatalogoPanel } from '../features/catalogo/CatalogoPanel'
import { ModeracionPanel } from '../features/catalogo/ModeracionPanel'
import { AdministracionPanel } from '../features/administracion/AdministracionPanel'
import { consultarPermisoAdministrativo } from '../features/administracion/administracionApi'
import type { SesionActiva } from '../features/cuenta/sesionApi'
import { bloquearContextosOfflineUsuario } from '../offline/registroAnimalesOffline'
import {
  AccesoOfflineSeguro,
  PwaEstado,
  useEstadoConexion,
} from '../pwa/PwaEstado'

function observarNavegacion(cambio: () => void) {
  window.addEventListener('hashchange', cambio)
  return () => window.removeEventListener('hashchange', cambio)
}

const vistaActual = () => window.location.hash

export default function App() {
  const [sesion, setSesion] = useState<SesionActiva | null>(null)
  const conectado = useEstadoConexion()
  const vista = useSyncExternalStore(observarNavegacion, vistaActual)
  const mostrarCatalogo = vista === '#catalogo'
  const token = sesion?.accessToken
  const [permisoAdministracion, setPermisoAdministracion] = useState<string>()
  const puedeAdministrar = conectado && Boolean(token && permisoAdministracion === token)
  const mostrarAdministracion = vista === '#administracion' && puedeAdministrar
  const retirarPermiso = useCallback(() => setPermisoAdministracion(undefined), [])
  const invalidarSesion = useCallback(() => setSesion(actual => {
    if (actual) void bloquearContextosOfflineUsuario(actual.usuarioId).catch(() => undefined)
    return null
  }), [])

  useEffect(() => {
    let vigente = true
    if (conectado && token) void consultarPermisoAdministrativo(token).then(permitido => {
      if (vigente) setPermisoAdministracion(permitido ? token : undefined)
    }).catch(() => { if (vigente) retirarPermiso() })
    return () => { vigente = false }
  }, [token, conectado, retirarPermiso])

  return (
    <>
      <a className="skip-link" href="#contenido">Ir al contenido</a>
      <header className="site-header">
        <a className="brand" href={sesion ? '#mis-ganaderias' : '#inicio'} aria-label="GanadoConecta, inicio">
          <span className="brand-mark" aria-hidden="true">gc<span>·</span></span>
          <span>Ganado<span className="brand-light">Conecta</span></span>
        </a>
        <nav aria-label="Navegación principal">
          <a className={mostrarCatalogo || mostrarAdministracion ? undefined : 'nav-active'} href={sesion ? '#mis-ganaderias' : '#inicio'}
            aria-current={mostrarCatalogo || mostrarAdministracion ? undefined : 'page'}>{sesion ? 'Mi ganadería' : 'Inicio'}</a>
          <a href="#iniciar-sesion">{sesion ? 'Mi cuenta' : 'Iniciar sesión'}</a>
          {!sesion && conectado && <a href="#crear-cuenta">Crear cuenta</a>}
          <a className={mostrarCatalogo ? 'nav-active' : undefined}
            aria-current={mostrarCatalogo ? 'page' : undefined} href="#catalogo">Catálogo público</a>
          {puedeAdministrar && <a className={mostrarAdministracion ? 'nav-active' : undefined}
            aria-current={mostrarAdministracion ? 'page' : undefined} href="#administracion">Administración</a>}
        </nav>
      </header>

      <PwaEstado conectado={conectado} sesionActiva={Boolean(sesion)} />

      <main id="contenido">
        <div hidden={mostrarCatalogo || mostrarAdministracion}>
        {!sesion && <section className="hero" id="inicio" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="eyebrow"><span className="small-rule" /> CERCA DE TU GANADERÍA</p>
            <h1 id="hero-title">Tu ganado.<br />Su cuidado.<br /><span>Todo conectado.</span></h1>
            <p className="hero-description">Estamos construyendo un espacio sencillo para organizar la información y acompañar el cuidado de tu ganado vacuno.</p>
            <a className="button button-primary" href="#crear-cuenta">Crear mi cuenta <span aria-hidden="true">↗</span></a>
            <p className="hero-note">PWA instalable · La carga básica offline no sustituye sesión ni permisos</p>
          </div>
          <div className="landscape" aria-hidden="true">
            <div className="landscape-caption"><span>GANADOCONECTA</span><span>CRECER CON CUIDADO ↗</span></div>
            <svg viewBox="0 0 600 620" preserveAspectRatio="xMidYMid slice" role="presentation">
              <circle cx="440" cy="142" r="48" fill="#e8ce86" />
              <path d="M0 300 Q130 155 290 275 T600 210 V620 H0Z" fill="#c4d6bb" />
              <path d="M0 390 Q180 210 370 345 T600 315 V620 H0Z" fill="#73947a" />
              <path d="M0 500 Q180 330 370 415 T600 420 V620 H0Z" fill="#3d6b52" />
              <path d="M380 620 Q260 470 390 360" fill="none" stroke="#e6e2c8" strokeWidth="30" />
              <path d="M0 540 Q160 440 275 470 M0 575 Q170 470 290 505 M0 610 Q170 510 310 550" fill="none" stroke="#83a387" strokeWidth="2" />
              <g transform="translate(155 290)" fill="#f6f4df" stroke="#254e3c" strokeWidth="4" strokeLinejoin="round">
                <path d="M0 45 Q-10 0 30 0 H95 Q115 2 115 28 L115 65 H96 L90 42 H30 L23 65 H7 Z" />
                <path d="M94 0 L118 -13 L138 -3 L139 27 L116 34 L105 22 Z" />
                <path d="M118 -13 L111 -26 M132 -9 L142 -20" fill="none" />
                <path d="M0 12 L-14 30 L-12 46" fill="none" />
                <path d="M24 0 L45 3 L55 23 L32 32 L16 19 Z" fill="#254e3c" stroke="none" />
                <circle cx="129" cy="6" r="3" fill="#254e3c" stroke="none" />
              </g>
            </svg>
            <div className="landscape-bottom"><span>Una mirada al futuro del cuidado ganadero.</span><span>↗</span></div>
          </div>
        </section>}

        {!conectado && !sesion ? (
          <AccesoOfflineSeguro />
        ) : (
          <>
            <AccesoCuenta sesion={sesion} onSesionChange={setSesion} />
            {sesion && <GanaderiasPanel key={sesion.usuarioId} sesion={sesion} onSesionInvalida={invalidarSesion} />}
            {!sesion && conectado && <RegistroCuenta />}
          </>
        )}

        {conectado && sesion && !mostrarAdministracion &&
          <ModeracionPanel key={sesion.usuarioId} token={sesion.accessToken} onSesionInvalida={invalidarSesion} />}
        {!sesion && <ApiStatus />}
        </div>
        {mostrarAdministracion && sesion && <AdministracionPanel key={sesion.usuarioId}
          token={sesion.accessToken} onSesionInvalida={invalidarSesion} onPermisoPerdido={retirarPermiso} />}
        {mostrarCatalogo && (conectado
          ? <CatalogoPanel token={sesion?.accessToken} />
          : <section className="project-section" id="catalogo"><h1>Catálogo público</h1>
              <p>Conéctate a internet para consultar las publicaciones.</p></section>)}
      </main>
      <footer><a className="footer-brand" href={sesion ? '#mis-ganaderias' : '#inicio'}>GanadoConecta</a><p>Información clara. Cuidado cercano.</p></footer>
    </>
  )
}
