import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AceptarInvitacion, AccesoFamiliarPanel } from './AccesoFamiliarPanel'
import { solicitudPrivada } from './solicitudPrivada'

vi.mock('./solicitudPrivada', () => ({ solicitudPrivada: vi.fn() }))

describe('Acceso familiar', () => {
  it('acepta el código por POST y actualiza las ganaderías', async () => {
    vi.mocked(solicitudPrivada).mockResolvedValueOnce({ ganaderiaId: 'g' })
    const actualizar = vi.fn()
    render(<AceptarInvitacion token="jwt-sintético" onSesionInvalida={vi.fn()} actualizar={actualizar} />)
    fireEvent.change(screen.getByLabelText('Código de invitación'), { target: { value: 'a'.repeat(43) } })
    fireEvent.click(screen.getByRole('button', { name: 'Aceptar invitación' }))
    expect(await screen.findByText('Invitación aceptada.')).toBeTruthy()
    expect(solicitudPrivada).toHaveBeenLastCalledWith('/api/v1/invitaciones/aceptar',
      'jwt-sintético', 'POST', { codigo: 'a'.repeat(43) })
    expect(actualizar).toHaveBeenCalledOnce()
  })

  it('muestra el código emitido y deja el rol de titular fuera de la invitación', async () => {
    vi.mocked(solicitudPrivada).mockImplementation(async (_ruta, _token, metodo) =>
      metodo === 'POST' ? { codigo: 'b'.repeat(43) } : { integrantes: [], invitaciones: [] })
    render(<AccesoFamiliarPanel ganaderiaId="g" token="jwt-sintético" onSesionInvalida={vi.fn()} />)
    expect(screen.queryByRole('option', { name: 'Propietario' })).toBeNull()
    fireEvent.change(screen.getByLabelText('Correo del familiar'), { target: { value: 'familiar@ejemplo.invalid' } })
    fireEvent.click(screen.getByRole('button', { name: 'Generar invitación' }))
    expect(await screen.findByDisplayValue('b'.repeat(43))).toBeTruthy()
    expect(solicitudPrivada).toHaveBeenCalledWith('/api/v1/ganaderias/g/familia/invitaciones',
      'jwt-sintético', 'POST', { correo: 'familiar@ejemplo.invalid', rol: 'LECTOR' })
  })
})
