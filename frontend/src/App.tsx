import { ApiStatus } from './ApiStatus'
import { AccesoCuenta } from './AccesoCuenta'
import { RegistroCuenta } from './RegistroCuenta'

const futureSections = [
  { number: '01', title: 'Mi ganado', text: 'La información de cada animal, organizada en un solo lugar.' },
  { number: '02', title: 'Cuidado y seguimiento', text: 'Una forma sencilla de consultar actividades e historiales.' },
  { number: '03', title: 'Calendario', text: 'Un espacio para planificar las próximas actividades de tu ganadería.' },
]

export default function App() {
  return (
    <>
      <a className="skip-link" href="#contenido">Ir al contenido</a>
      <header className="site-header">
        <a className="brand" href="#inicio" aria-label="GanadoConecta, inicio">
          <span className="brand-mark" aria-hidden="true">gc<span>·</span></span>
          <span>Ganado<span className="brand-light">Conecta</span></span>
        </a>
        <nav aria-label="Navegación principal">
          <a className="nav-active" href="#inicio" aria-current="page">Inicio</a>
          <a href="#iniciar-sesion">Iniciar sesión</a>
          <a href="#crear-cuenta">Crear cuenta</a>
          <button disabled>Mi ganado <span className="soon-label">Próximamente</span></button>
          <button disabled>Calendario</button>
        </nav>
        <span className="edition"><span aria-hidden="true">◌</span> En preparación</span>
      </header>

      <main id="contenido">
        <section className="hero" id="inicio" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="eyebrow"><span className="small-rule" /> CERCA DE TU GANADERÍA</p>
            <h1 id="hero-title">Tu ganado.<br />Su cuidado.<br /><span>Todo conectado.</span></h1>
            <p className="hero-description">Estamos construyendo un espacio sencillo para organizar la información y acompañar el cuidado de tu ganado vacuno.</p>
            <a className="button button-primary" href="#crear-cuenta">Crear mi cuenta <span aria-hidden="true">↗</span></a>
            <p className="hero-note">Primera versión en desarrollo · Sin registros de animales todavía</p>
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
        </section>

        <AccesoCuenta />
        <RegistroCuenta />

        <section className="project-section" id="proyecto" aria-labelledby="project-title">
          <div className="section-heading">
            <div><p className="eyebrow">LO QUE ESTAMOS PREPARANDO</p><h2 id="project-title">Más claridad, cada día.</h2></div>
            <p>Funciones que se incorporarán por etapas.<br />Aún no están disponibles.</p>
          </div>
          <div className="feature-grid">
            {futureSections.map(section => (
              <article className="feature-card" key={section.number}>
                <span className="feature-number">{section.number}</span>
                <h3>{section.title}</h3><p>{section.text}</p>
                <span className="feature-state">En preparación <span aria-hidden="true">↗</span></span>
              </article>
            ))}
          </div>
        </section>
        <ApiStatus />
      </main>
      <footer><a className="footer-brand" href="#inicio">GanadoConecta</a><p>Información clara. Cuidado cercano.</p><span>Ganado vacuno · Primera etapa</span></footer>
    </>
  )
}
