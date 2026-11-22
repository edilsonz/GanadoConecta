import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './app/App'
import './estilos/styles.css'
import './estilos/identidad.css'
import './estilos/historiales.css'
import './features/calendario/agendaSanitaria.css'
import './features/catalogo/catalogo.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode><App /></StrictMode>,
)
