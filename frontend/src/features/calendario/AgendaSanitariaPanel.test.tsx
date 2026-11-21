import { cleanup,fireEvent,render,screen,waitFor } from '@testing-library/react';import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';import { AgendaSanitariaPanel } from './AgendaSanitariaPanel';import { completarActividad,listarAgenda,programarActividad } from './agendaSanitariaApi';
vi.mock('./agendaSanitariaApi',async o=>({...await o<typeof import('./agendaSanitariaApi')>(),listarAgenda:vi.fn(),programarActividad:vi.fn(),completarActividad:vi.fn()}));
const animal={animalId:'a',ganaderiaId:'g',sexo:'HEMBRA' as const,nombre:'Fiona',areteFrente:null,areteReverso:null,coloresIdentificacionTextil:null,ubicacionIdentificacionTextil:null,raza:null,fechaNacimiento:null,nacimientoEstimado:null,categoria:null,observaciones:null,estado:'ACTIVO' as const,version:1,creadoEn:new Date().toISOString()};const p={ganaderiaId:'g',animales:[animal],accessToken:'t',puedeEscribir:true,onSesionInvalida:vi.fn()};
beforeEach(()=>{vi.resetAllMocks();vi.mocked(listarAgenda).mockResolvedValue([]);vi.mocked(programarActividad).mockResolvedValue({} as never);vi.mocked(completarActividad).mockResolvedValue({} as never)});afterEach(cleanup)
describe('Agenda sanitaria',()=>{
 it('avisa de una campaña pendiente y confirma solo el animal seleccionado', async () => {
   vi.mocked(listarAgenda).mockResolvedValue([{ actividadId:'c', animalId:null, eventoOrigenId:null,
     tipo:'CAMPANA_BANO', fechaPrevista:'2026-11-21', descripcion:'Campaña sintética', observaciones:null,
     estado:'PENDIENTE', estadoVisible:'PENDIENTE', version:1, creadoEn:new Date().toISOString(),
     completadoEn:null, participantes:['a','b'] }])
   render(<AgendaSanitariaPanel {...p} animales={[animal,{...animal,animalId:'b',nombre:'Otra'}]} />)
   expect(await screen.findByLabelText('Recordatorios internos')).toHaveTextContent('Campaña sintética')
   fireEvent.click(screen.getByRole('button',{name:'Seleccionar atendidos'}))
   expect(screen.getByRole('button',{name:'Confirmar atenciones'})).toBeDisabled()
   fireEvent.click(screen.getByRole('checkbox',{name:'Fiona'}))
   fireEvent.click(screen.getByRole('button',{name:'Confirmar atenciones'}))
   await waitFor(() => expect(completarActividad).toHaveBeenCalledWith('g','c','t',
     expect.objectContaining({atendidos:['a']})))
 })
 it('muestra vacío real y error',async()=>{render(<AgendaSanitariaPanel {...p}/>);expect(await screen.findByText('No hay actividades con este filtro.')).toBeInTheDocument();cleanup();vi.mocked(listarAgenda).mockRejectedValueOnce(new Error());render(<AgendaSanitariaPanel {...p}/>);expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar')})
 it('programa conservando identidad en reintento',async()=>{vi.mocked(programarActividad).mockRejectedValueOnce(new Error());render(<AgendaSanitariaPanel {...p}/>);await screen.findByText('No hay actividades con este filtro.');fireEvent.change(screen.getByLabelText('Animal *'),{target:{value:'a'}});fireEvent.change(screen.getByLabelText('Fecha prevista *'),{target:{value:'2099-12-01'}});fireEvent.change(screen.getByLabelText('Descripción *'),{target:{value:'Control futuro'}});fireEvent.click(screen.getByRole('button',{name:'Programar actividad'}));fireEvent.click(await screen.findByRole('button',{name:'Reintentar misma actividad'}));await waitFor(()=>expect(programarActividad).toHaveBeenCalledTimes(2));expect(vi.mocked(programarActividad).mock.calls[0]).toEqual(vi.mocked(programarActividad).mock.calls[1])})
 it('marca realizada sin afirmar evento clínico',async()=>{vi.mocked(listarAgenda).mockResolvedValue([{actividadId:'x',animalId:'a',eventoOrigenId:null,tipo:'VACUNACION',fechaPrevista:'2099-12-01',descripcion:'Control',observaciones:null,estado:'PENDIENTE',estadoVisible:'PROXIMA',version:1,creadoEn:new Date().toISOString(),completadoEn:null}]);render(<AgendaSanitariaPanel {...p}/>);fireEvent.click(await screen.findByText('Marcar realizada'));expect(await screen.findByText('Actividad marcada como completada. No se creó un evento clínico.')).toBeInTheDocument()})
 it('lector solo consulta',async()=>{render(<AgendaSanitariaPanel {...p} puedeEscribir={false}/>);await screen.findByText('No hay actividades con este filtro.');expect(screen.queryByText('Programar actividad')).not.toBeInTheDocument()})
 it('consulta una sola vez con el filtro inicial y permite volver a todos', async () => {
   render(<AgendaSanitariaPanel {...p} animalInicialId="a" />)
   await screen.findByText('No hay actividades con este filtro.')
   expect(listarAgenda).toHaveBeenCalledExactlyOnceWith('g', 't', { animalId: 'a' })
   expect(screen.getByLabelText('Animal *')).toHaveValue('a')
   fireEvent.change(screen.getByLabelText('Filtrar por animal'), { target: { value: '' } })
   await waitFor(() => expect(listarAgenda).toHaveBeenLastCalledWith('g', 't', {}))
 })
})
