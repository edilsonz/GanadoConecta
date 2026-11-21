import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { ModeracionPanel } from './ModeracionPanel'
import { ErrorSolicitudPrivada, solicitudPrivada } from '../../shared/api/solicitudPrivada'

vi.mock('../../shared/api/solicitudPrivada', async importOriginal => ({
  ...(await importOriginal<typeof import('../../shared/api/solicitudPrivada')>()),
  solicitudPrivada: vi.fn(),
}))

it('oculta la bandeja si el servidor deniega el permiso', async () => {
  vi.mocked(solicitudPrivada).mockReset().mockRejectedValue(new ErrorSolicitudPrivada(403))
  render(<ModeracionPanel token="acceso-prueba" onSesionInvalida={vi.fn()} />)
  await waitFor(() => expect(solicitudPrivada).toHaveBeenCalledOnce())
  expect(screen.queryByText('Moderación del catálogo')).not.toBeInTheDocument()
})

it('muestra la bandeja autorizada y la oculta ante revocación', async () => {
  vi.mocked(solicitudPrivada).mockReset().mockResolvedValueOnce([])
    .mockRejectedValueOnce(new ErrorSolicitudPrivada(403))
  render(<ModeracionPanel token="acceso-prueba" onSesionInvalida={vi.fn()} />)
  const titulo = await screen.findByText('Moderación del catálogo')
  titulo.closest('details')!.open = true
  expect(screen.getByText('No hay reportes.')).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Actualizar reportes' }))
  await waitFor(() => expect(titulo).not.toBeInTheDocument())
})
