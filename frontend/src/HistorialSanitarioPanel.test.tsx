import { cleanup,fireEvent,render,screen,waitFor } from '@testing-library/react'
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest'
import { HistorialSanitarioPanel } from './HistorialSanitarioPanel'
import { listarHistorialSanitario,registrarEventoSanitario } from './historialSanitarioApi'
vi.mock('./historialSanitarioApi',async o=>({...await o<typeof import('./historialSanitarioApi')>(),listarHistorialSanitario:vi.fn(),registrarEventoSanitario:vi.fn()}))
const p={ganaderiaId:'g',animalId:'a',accessToken:'t',puedeEscribir:true,onSesionInvalida:vi.fn()}
beforeEach(()=>{vi.resetAllMocks();vi.mocked(listarHistorialSanitario).mockResolvedValue([]);vi.mocked(registrarEventoSanitario).mockResolvedValue()})
afterEach(cleanup)
describe('Historial sanitario',()=>{
 it('distingue vacío confirmado y error',async()=>{vi.mocked(listarHistorialSanitario).mockRejectedValueOnce(new Error())
  render(<HistorialSanitarioPanel {...p}/>);expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar')})
 it('registra un hecho y conserva identificadores al reintentar',async()=>{vi.mocked(registrarEventoSanitario).mockRejectedValueOnce(new Error())
  render(<HistorialSanitarioPanel {...p}/>);await screen.findByText('Aún no hay eventos sanitarios con este filtro.')
  fireEvent.change(screen.getByLabelText('Fecha *'),{target:{value:'2026-01-02'}});fireEvent.change(screen.getByLabelText('Descripción del hecho *'),{target:{value:'Hecho documentado'}})
  fireEvent.click(screen.getByText('Guardar evento sanitario'));fireEvent.click(await screen.findByText('Reintentar mismo evento'))
  await waitFor(()=>expect(registrarEventoSanitario).toHaveBeenCalledTimes(2));expect(vi.mocked(registrarEventoSanitario).mock.calls[0]).toEqual(vi.mocked(registrarEventoSanitario).mock.calls[1])})
 it('filtra por tipo',async()=>{render(<HistorialSanitarioPanel {...p}/>);await screen.findByText('Aún no hay eventos sanitarios con este filtro.')
  fireEvent.change(screen.getByLabelText('Filtrar por tipo'),{target:{value:'REVISION_VETERINARIA'}})
  await waitFor(()=>expect(listarHistorialSanitario).toHaveBeenLastCalledWith('g','a','t','REVISION_VETERINARIA'))})
 it('lector consulta y no registra',async()=>{render(<HistorialSanitarioPanel {...p} puedeEscribir={false}/>);await screen.findByText('Aún no hay eventos sanitarios con este filtro.');expect(screen.queryByText('Guardar evento sanitario')).not.toBeInTheDocument()})
})
