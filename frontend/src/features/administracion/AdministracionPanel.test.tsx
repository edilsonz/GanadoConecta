import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { AdministracionPanel } from './AdministracionPanel'
import { consultarEstadisticasPlataforma, listarUsuarios } from './administracionApi'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'

vi.mock('./administracionApi', () => ({ consultarEstadisticasPlataforma: vi.fn(), listarUsuarios: vi.fn() }))
vi.mock('../catalogo/ModeracionPanel', () => ({ ModeracionPanel: () => <p>Acceso independiente a moderación</p> }))
const props = { token: 'sintetico', onSesionInvalida: vi.fn(), onPermisoPerdido: vi.fn() }

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(consultarEstadisticasPlataforma).mockResolvedValue({ cuentas: 8, cuentasActivas: 7, ganaderias: 4, ganaderiasActivas: 3 })
  vi.mocked(listarUsuarios).mockResolvedValue({ items: [], siguienteCursor: null })
})

it('muestra conteos reales, filtro y estados vacíos', async () => {
  render(<AdministracionPanel {...props} />)
  expect(await screen.findByText('No se encontraron cuentas.')).toBeVisible()
  expect(screen.getByText('8')).toBeVisible()
  expect(screen.getByText('Acceso independiente a moderación')).toBeVisible()
  fireEvent.change(screen.getByLabelText('UUID o correo completo'), { target: { value: 'persona@e2e.invalid' } })
  fireEvent.change(screen.getByLabelText('Estado de cuenta'), { target: { value: 'INACTIVO' } })
  fireEvent.click(screen.getByRole('button', { name: 'Buscar cuentas' }))
  await waitFor(() => expect(listarUsuarios).toHaveBeenLastCalledWith('sintetico', { buscar: 'persona@e2e.invalid', estado: 'INACTIVO' }))
})

it('usa el cursor para cambiar de página', async () => {
  vi.mocked(listarUsuarios).mockResolvedValueOnce({ items: [{
    id: 'usuario', correo: 'persona@e2e.invalid', estado: 'ACTIVO', creadoEn: '2026-11-21T12:00Z',
  }], siguienteCursor: 'usuario' })
  render(<AdministracionPanel {...props} />)
  await screen.findByText('persona@e2e.invalid')
  fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
  await waitFor(() => expect(listarUsuarios).toHaveBeenLastCalledWith('sintetico', { buscar: '', estado: '', cursor: 'usuario' }))
})

it('notifica pérdida de permiso sin seguir mostrando datos', async () => {
  vi.mocked(listarUsuarios).mockRejectedValueOnce(new ErrorSolicitudPrivada(403))
  render(<AdministracionPanel {...props} />)
  await waitFor(() => expect(props.onPermisoPerdido).toHaveBeenCalledOnce())
  expect(screen.queryByRole('list', { name: 'Cuentas' })).not.toBeInTheDocument()
})
