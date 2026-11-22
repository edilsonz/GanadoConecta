import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarAlertas } from '../../features/analisis/analisisApi'
import { listarAgenda, type ActividadSanitaria } from '../../features/calendario/agendaSanitariaApi'
import { consultarReporte } from '../../features/reportes/reportesApi'
import { ErrorSolicitudPrivada } from '../../shared/api/solicitudPrivada'
import { InicioGanaderia } from './InicioGanaderia'

vi.mock('../../features/analisis/analisisApi')
vi.mock('../../features/calendario/agendaSanitariaApi', { spy: true })
vi.mock('../../features/reportes/reportesApi')

const reporte = {
  sanidad: { total: 7, porTipo: [] },
  reproduccion: { montas: 3, inseminaciones: 0, evaluaciones: 5, confirmadas: 2,
    noConfirmadas: 3, indeterminadas: 0, partos: 1, crias: 1 },
  peso: { total: 0, ultimo: null, mediciones: [] },
}
const proxima: ActividadSanitaria = {
  actividadId: 'actividad-1', animalId: 'animal-1', eventoOrigenId: null, tipo: 'VACUNACION',
  fechaPrevista: '2026-11-25', descripcion: 'Vacunación programada', observaciones: null,
  estado: 'PENDIENTE', estadoVisible: 'PROXIMA', version: 1, creadoEn: '2026-11-21T10:00:00Z', completadoEn: null,
}

const props = {
  ganaderiaId: 'ganaderia-1', token: 'token-de-prueba', conectado: true, accesoVigente: true,
  navegar: vi.fn(), onSesionInvalida: vi.fn(),
}

describe('Resumen de la ganadería', () => {
  beforeEach(() => {
    vi.mocked(consultarReporte).mockResolvedValue(reporte)
    vi.mocked(listarAgenda).mockResolvedValue([
      { ...proxima, actividadId: 'actividad-2', estadoVisible: 'VENCIDA', descripcion: 'Actividad vencida' },
      proxima,
    ])
    vi.mocked(consultarAlertas).mockResolvedValue({ fechaCorte: '2026-11-21', avisos: [
      { actividadId: 'actividad-2', estado: 'VENCIDA', fechaPrevista: '2026-11-20', tipo: 'CAMPANA_BANO',
        descripcion: 'Actividad vencida' },
    ] })
  })

  afterEach(() => vi.resetAllMocks())

  it('usa las cifras de la API, separa agenda futura de avisos y ofrece accesos rápidos', async () => {
    render(<InicioGanaderia {...props} />)
    expect(await screen.findByText('7')).toBeVisible()
    expect(screen.getByText('2')).toBeVisible()
    expect(within(screen.getByRole('region', { name: 'Próximas actividades' }))
      .getByText('Vacunación programada')).toBeVisible()
    expect(within(screen.getByRole('region', { name: 'Alertas de seguimiento' }))
      .getByText('Actividad vencida')).toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: 'Abrir agenda' }))
    expect(props.navegar).toHaveBeenCalledWith('calendario')
    expect(listarAgenda).toHaveBeenCalledWith(props.ganaderiaId, props.token, { estado: 'PENDIENTE' })
  })

  it('espera los permisos y elimina las cifras visibles al quedar sin conexión', async () => {
    const { rerender } = render(<InicioGanaderia {...props} accesoVigente={false} />)
    expect(consultarReporte).not.toHaveBeenCalled()
    rerender(<InicioGanaderia {...props} />)
    expect(await screen.findByText('7')).toBeVisible()
    rerender(<InicioGanaderia {...props} conectado={false} />)
    expect(screen.queryByText('7')).not.toBeInTheDocument()
    expect(screen.getByText(/Conéctate a internet/)).toBeVisible()
    expect(consultarReporte).toHaveBeenCalledTimes(1)
  })

  it('distingue fallos temporales, permite reconsultar y oculta los datos ante un 403', async () => {
    vi.mocked(consultarReporte).mockRejectedValueOnce(new Error('fallo temporal'))
    render(<InicioGanaderia {...props} />)
    expect(await screen.findByText('No se pudo consultar los indicadores.')).toBeVisible()
    expect(screen.queryByText('0')).not.toBeInTheDocument()
    expect(screen.getByText('Vacunación programada')).toBeVisible()
    vi.mocked(consultarAlertas).mockRejectedValueOnce(new ErrorSolicitudPrivada(403))
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar resumen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya no tienes acceso')
    expect(screen.queryByText('7')).not.toBeInTheDocument()
    expect(screen.queryByText('Vacunación programada')).not.toBeInTheDocument()
  })
})
