import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { FichaAnimalTabs } from './FichaAnimalTabs'

it('abre el panel sugerido, navega por teclado y conserva el borrador al volver', () => {
  render(<FichaAnimalTabs sugerida="sanidad" contenido={{
    general: <p>Identificación</p>,
    sanidad: <label>Observaciones<input /></label>,
    peso: <p>Pesos registrados</p>,
  }} />)

  const sanidad = screen.getByRole('tab', { name: 'Sanidad' })
  const peso = screen.getByRole('tab', { name: 'Peso' })
  expect(sanidad).toHaveAttribute('aria-selected', 'true')
  expect(screen.queryByText('Pesos registrados')).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Observaciones'), { target: { value: 'Borrador' } })
  fireEvent.keyDown(sanidad, { key: 'ArrowRight' })
  expect(peso).toHaveFocus()
  expect(screen.getByRole('tabpanel', { name: 'Peso' })).toHaveTextContent('Pesos registrados')
  expect(screen.queryByRole('textbox', { name: 'Observaciones' })).not.toBeInTheDocument()
  fireEvent.keyDown(peso, { key: 'ArrowLeft' })
  expect(screen.getByRole('textbox', { name: 'Observaciones' })).toHaveValue('Borrador')
  expect(screen.queryByRole('tab', { name: 'Anuncios' })).not.toBeInTheDocument()
})
