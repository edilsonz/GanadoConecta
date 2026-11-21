import { useEffect, useId, useState, type KeyboardEvent, type ReactNode } from 'react'

const etiquetas = {
  general: 'General',
  sanidad: 'Sanidad',
  reproduccion: 'Reproducción',
  peso: 'Peso',
  genealogia: 'Genealogía',
  anuncios: 'Anuncios',
} as const

export type SeccionFicha = keyof typeof etiquetas

export function FichaAnimalTabs({ sugerida, contenido }: {
  sugerida: SeccionFicha
  contenido: Partial<Record<SeccionFicha, ReactNode>>
}) {
  const id = useId()
  const [seleccion, setSeleccion] = useState({ sugerida, activa: sugerida })
  const [visitadas, setVisitadas] = useState<SeccionFicha[]>([])
  const opciones = (Object.keys(etiquetas) as SeccionFicha[]).filter(clave => contenido[clave] !== undefined)
  const candidata = seleccion.sugerida === sugerida ? seleccion.activa : sugerida
  const activa = opciones.includes(candidata) ? candidata : 'general'

  useEffect(() => {
    setVisitadas(previas => previas.includes(activa) ? previas : [...previas, activa])
  }, [activa])

  function navegar(evento: KeyboardEvent<HTMLButtonElement>, posicion: number) {
    const siguiente = {
      ArrowLeft: (posicion + opciones.length - 1) % opciones.length,
      ArrowRight: (posicion + 1) % opciones.length,
      Home: 0,
      End: opciones.length - 1,
    }[evento.key]
    if (siguiente === undefined) return
    evento.preventDefault()
    const destino = opciones[siguiente]
    setSeleccion({ sugerida, activa: destino })
    document.getElementById(`${id}-tab-${destino}`)?.focus()
  }

  return (
    <>
      <div className="animal-tabs" role="tablist" aria-label="Secciones de la ficha">
        {opciones.map((clave, i) => (
          <button type="button" role="tab" key={clave} id={`${id}-tab-${clave}`}
            aria-selected={clave === activa} aria-controls={`${id}-panel-${clave}`}
            tabIndex={clave === activa ? 0 : -1}
            onClick={() => setSeleccion({ sugerida, activa: clave })}
            onKeyDown={e => navegar(e, i)}>{etiquetas[clave]}</button>
        ))}
      </div>
      {opciones.map(clave => (
        <div key={clave} role="tabpanel" id={`${id}-panel-${clave}`}
          aria-labelledby={`${id}-tab-${clave}`} hidden={clave !== activa} tabIndex={0}>
          {(clave === activa || visitadas.includes(clave)) && contenido[clave]}
        </div>
      ))}
    </>
  )
}
