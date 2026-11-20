import { useEffect,useRef,useState,type FormEvent } from 'react'
import { ErrorSanidad } from './vacunacionesApi'
import { listarHistorialSanitario,registrarEventoSanitario,type EventoSanitario,type SolicitudEventoSanitario,type TipoEventoSanitario } from './historialSanitarioApi'
import { useProteccionActualizacionPwa } from './pwaActividad'
type Props={ganaderiaId:string;animalId:string;accessToken:string;puedeEscribir:boolean;onSesionInvalida:()=>void}
const etiquetas:Record<TipoEventoSanitario,string>={VACUNACION:'Vacunación',DESPARASITACION:'Desparasitación',TRATAMIENTO_SANITARIO:'Tratamiento sanitario',REVISION_VETERINARIA:'Revisión veterinaria'}
const inicial={tipo:'DESPARASITACION' as SolicitudEventoSanitario['tipo'],fecha:'',descripcion:'',responsable:'',producto:'',indicaciones:'',observaciones:''}
export function HistorialSanitarioPanel(p:Props){
 const[items,setItems]=useState<EventoSanitario[]>([]),[estado,setEstado]=useState<'carga'|'listo'|'error'>('carga')
 const[filtro,setFiltro]=useState<''|TipoEventoSanitario>(''),[form,setForm]=useState(inicial),[pendiente,setPendiente]=useState<SolicitudEventoSanitario|null>(null)
 const[enviando,setEnviando]=useState(false),[mensaje,setMensaje]=useState('');const ocupado=useRef(false)
 useProteccionActualizacionPwa(`evento-sanitario-${p.animalId}`,'FORMULARIO',enviando||Boolean(pendiente)||Object.values(form).some(v=>v!==''&&v!=='DESPARASITACION'))
 async function cargar(tipo=filtro){setEstado('carga');try{setItems(await listarHistorialSanitario(p.ganaderiaId,p.animalId,p.accessToken,tipo||undefined));setEstado('listo')}
 catch(e){setEstado('error');if(e instanceof ErrorSanidad&&e.tipo==='sesion')p.onSesionInvalida()}}
 useEffect(()=>{void cargar(filtro)},[p.ganaderiaId,p.animalId,p.accessToken,filtro])
 async function guardar(e:FormEvent){e.preventDefault();if(ocupado.current||!p.puedeEscribir)return
  if(!form.fecha||!form.descripcion.trim()){setMensaje('Indica la fecha y una descripción del hecho realizado.');return}
  const s=pendiente??{...form,operationId:crypto.randomUUID(),eventId:crypto.randomUUID(),schemaVersion:1 as const}
  ocupado.current=true;setEnviando(true);setPendiente(s);setMensaje('')
  try{await registrarEventoSanitario(p.ganaderiaId,p.animalId,p.accessToken,s);setPendiente(null);setForm(inicial);setMensaje('Evento sanitario registrado correctamente.');await cargar()}
  catch(x){const t=x instanceof ErrorSanidad?x.tipo:'temporal';setMensaje(t==='sesion'?'Tu sesión terminó.':t==='permiso'?'No tienes permiso vigente para registrar este evento.':t==='conflicto'?'Existe un conflicto; el registro no se sobrescribió.':t==='validacion'?'Revisa los datos del evento sanitario.':'No recibimos confirmación. Puedes reintentar el mismo registro.');if(t==='validacion')setPendiente(null);if(t==='sesion')p.onSesionInvalida()}
  finally{ocupado.current=false;setEnviando(false)}}
 return <section className="health-history" aria-label="Historial sanitario">
  <div className="animals-list-heading"><h4>Historial sanitario</h4><label>Filtrar por tipo <select value={filtro} onChange={e=>setFiltro(e.target.value as typeof filtro)}><option value="">Todos</option>{Object.entries(etiquetas).map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label></div>
  {estado==='carga'&&<p role="status">Consultando historial sanitario…</p>}{estado==='error'&&<p role="alert">No se pudo cargar el historial sanitario.</p>}
  {estado==='listo'&&items.length===0&&<p>Aún no hay eventos sanitarios con este filtro.</p>}
  <ol>{items.map(x=><li key={x.eventId}><strong>{etiquetas[x.tipo]} · {x.fecha}</strong><p>{x.titulo}</p>
   {x.responsable&&<small>Responsable: {x.responsable}</small>}{x.producto&&<p>Producto registrado: {x.producto}</p>}{x.indicaciones&&<p>Indicaciones registradas: {x.indicaciones}</p>}{x.observaciones&&<p>{x.observaciones}</p>}</li>)}</ol>
  {mensaje&&<p role="status">{mensaje}</p>}
  {p.puedeEscribir&&<form className="animal-form" onSubmit={guardar}><p>Registra un hecho ya realizado. El sistema no genera diagnósticos ni indicaciones.</p>
   <fieldset disabled={enviando||Boolean(pendiente)}><div className="animal-fields-grid">
    <div className="form-field"><label htmlFor="evento-tipo">Tipo *</label><select id="evento-tipo" value={form.tipo} onChange={e=>setForm(f=>({...f,tipo:e.target.value as typeof f.tipo}))}>{Object.entries(etiquetas).filter(([v])=>v!=='VACUNACION').map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></div>
    <div className="form-field"><label htmlFor="evento-fecha">Fecha *</label><input id="evento-fecha" type="date" value={form.fecha} onChange={e=>setForm(f=>({...f,fecha:e.target.value}))}/></div>
    {([['responsable','Responsable',150],['producto','Producto utilizado',150]] as const).map(([k,l,m])=><div className="form-field" key={k}><label htmlFor={`evento-${k}`}>{l}</label><input id={`evento-${k}`} maxLength={m} value={form[k]} onChange={e=>setForm(f=>({...f,[k]:e.target.value}))}/></div>)}
   </div><div className="form-field"><label htmlFor="evento-descripcion">Descripción del hecho *</label><textarea id="evento-descripcion" maxLength={1000} value={form.descripcion} onChange={e=>setForm(f=>({...f,descripcion:e.target.value}))}/></div>
   <div className="form-field"><label htmlFor="evento-indicaciones">Indicaciones registradas por el responsable</label><textarea id="evento-indicaciones" maxLength={1000} value={form.indicaciones} onChange={e=>setForm(f=>({...f,indicaciones:e.target.value}))}/></div>
   <div className="form-field"><label htmlFor="evento-observaciones">Observaciones</label><textarea id="evento-observaciones" maxLength={2000} value={form.observaciones} onChange={e=>setForm(f=>({...f,observaciones:e.target.value}))}/></div></fieldset>
   <button className="button button-primary" disabled={enviando}>{enviando?'Guardando…':pendiente?'Reintentar mismo evento':'Guardar evento sanitario'}</button></form>}
 </section>
}
