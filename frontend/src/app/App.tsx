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
import marca from '../assets/marca/ganadoconecta-marca.png'
import pastizal from '../assets/marca/pastizal-ganadero-1280.jpg'
import pastizalMovil from '../assets/marca/pastizal-ganadero-640.jpg'
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
          <img src={marca} alt="" width="392" height="180" />
        </a>
        <nav aria-label="Navegación principal">
          <a className={mostrarCatalogo || mostrarAdministracion ? undefined : 'nav-active'} href={sesion ? '#mis-ganaderias' : '#inicio'}
            aria-current={mostrarCatalogo || mostrarAdministracion ? undefined : 'page'}>{sesion ? 'Mi ganadería' : 'Inicio'}</a>
          <a href="#iniciar-sesion">{sesion ? 'Mi cuenta' : 'Iniciar sesión'}</a>
          <a className={mostrarCatalogo ? 'nav-active' : undefined}
            aria-current={mostrarCatalogo ? 'page' : undefined} href="#catalogo">Catálogo público</a>
          {!sesion && conectado && <a className="nav-cta" href="#crear-cuenta">Crear cuenta</a>}
          {puedeAdministrar && <a className={mostrarAdministracion ? 'nav-active' : undefined}
            aria-current={mostrarAdministracion ? 'page' : undefined} href="#administracion">Administración</a>}
        </nav>
      </header>

      <PwaEstado conectado={conectado} sesionActiva={Boolean(sesion)} />

      <main id="contenido" tabIndex={-1}>
        <div hidden={mostrarCatalogo || mostrarAdministracion}>
        {!sesion && <section className="hero" id="inicio" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="hero-tagline">Tu ganadería, siempre conectada</p>
            <h1 id="hero-title">Tu ganado.<br />{' '}Su cuidado.<br />{' '}<span>Todo conectado.</span></h1>
            <p className="hero-description">Registra tus animales, consulta sus historiales y organiza las tareas de tu ganadería en un solo lugar.</p>
            <div className="hero-actions">
              {conectado && <a className="button button-primary" href="#crear-cuenta">Crear mi cuenta <span aria-hidden="true">↗</span></a>}
              <a className="button button-outline" href="#catalogo">Explorar catálogo</a>
            </div>
          </div>
          <figure className="hero-photo">
            <img src={pastizal} srcSet={`${pastizalMovil} 640w, ${pastizal} 1280w`}
              sizes="(max-width: 680px) 92vw, (max-width: 1280px) 54vw, 680px"
              width="1280" height="853" alt="Ganado vacuno en un pastizal entre árboles y neblina" fetchPriority="high" />
            <figcaption><a href="https://commons.wikimedia.org/wiki/File:Cattle_in_meadow_(Unsplash).jpg">Fotografía de Karsten Würth</a></figcaption>
          </figure>
        </section>}

        {!conectado && !sesion ? (
          <AccesoOfflineSeguro />
        ) : (
          <>
            <AccesoCuenta sesion={sesion} onSesionChange={setSesion} />
            {sesion && <GanaderiasPanel key={sesion.usuarioId} sesion={sesion}
              conectado={conectado} onSesionInvalida={invalidarSesion} />}
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
      <footer className="site-footer"><a className="footer-brand" href={sesion ? '#mis-ganaderias' : '#inicio'}>GanadoConecta</a><p>Tu ganadería, siempre conectada</p></footer>
    </>
  )
}
