import { codigoError,esObjeto,leerJson,obtenerTokenCsrf } from '../cuenta/authApi'
import { ErrorSanidad } from '../sanidad/vacunacionesApi'
export const tiposActividad = {
  VACUNACION: 'Vacunación', TRATAMIENTO_SANITARIO: 'Tratamiento sanitario',
  REVISION_VETERINARIA: 'Revisión veterinaria', REPRODUCTIVA: 'Actividad reproductiva',
  PRODUCTIVA: 'Actividad productiva', CAMPANA_VACUNACION: 'Campaña de vacunación',
  CAMPANA_DESPARASITACION: 'Campaña de desparasitación', CAMPANA_BANO: 'Campaña de baño antiparasitario',
} as const
export type TipoActividad = keyof typeof tiposActividad
export type EstadoVisible='PROXIMA'|'PENDIENTE'|'VENCIDA'|'COMPLETADA'
export type ActividadSanitaria={actividadId:string;animalId:string|null;eventoOrigenId:string|null;tipo:TipoActividad;fechaPrevista:string;descripcion:string;observaciones:string|null;estado:'PENDIENTE'|'COMPLETADA';estadoVisible:EstadoVisible;version:1|2;creadoEn:string;completadoEn:string|null;participantes?:string[];atendidos?:string[];fechaAtencion?:string|null}
export type SolicitudActividad={actividadId:string;animalId?:string;participantes?:string[];eventoOrigenId?:string;tipo:TipoActividad;fechaPrevista:string;descripcion:string;observaciones?:string}
export type ConfirmacionCampana={atendidos:string[];fechaAtencion:string;nombreVacuna?:string}
const ruta=(g:string)=>`/api/v1/ganaderias/${encodeURIComponent(g)}/agenda-sanitaria`
function fallo(r:Response,b:unknown):never{if(r.status===401)throw new ErrorSanidad('sesion');if(r.status===403)throw new ErrorSanidad(['CSRF_INVALIDO','ORIGEN_NO_PERMITIDO'].includes(codigoError(b)??'')?'seguridad':'permiso');if(r.status===409)throw new ErrorSanidad('conflicto');if([400,404,422].includes(r.status))throw new ErrorSanidad('validacion');throw new ErrorSanidad('temporal')}
function esActividad(x:unknown):x is ActividadSanitaria{return esObjeto(x)&&typeof x.actividadId==='string'&&(typeof x.animalId==='string'||x.animalId===null&&Array.isArray(x.participantes)&&x.participantes.every(id=>typeof id==='string'))&&Object.hasOwn(tiposActividad,String(x.tipo))&&typeof x.fechaPrevista==='string'&&typeof x.descripcion==='string'&&['PROXIMA','PENDIENTE','VENCIDA','COMPLETADA'].includes(String(x.estadoVisible))}
export async function listarAgenda(g:string,t:string,f:{animalId?:string;tipo?:TipoActividad;estado?:'PENDIENTE'|'COMPLETADA'}={}){const q=new URLSearchParams();Object.entries(f).forEach(([k,v])=>v&&q.set(k,v));const r=await fetch(`${ruta(g)}${q.size?'?'+q:''}`,{credentials:'same-origin',cache:'no-store',headers:{Authorization:`Bearer ${t}`}});const b=await leerJson(r);if(r.ok&&Array.isArray(b)&&b.every(esActividad))return b as ActividadSanitaria[];fallo(r,b)}
async function escribir(url:string,t:string,body?:unknown){for(let i=0;i<2;i++){const csrf=await obtenerTokenCsrf();const r=await fetch(url,{method:'POST',credentials:'same-origin',cache:'no-store',headers:{Authorization:`Bearer ${t}`,'X-CSRF-TOKEN':csrf,...(body?{'Content-Type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{})});const b=await leerJson(r);if(r.status===403&&codigoError(b)==='CSRF_INVALIDO'&&i===0)continue;if(r.ok&&esActividad(b))return b;fallo(r,b)}}
export const programarActividad=(g:string,t:string,s:SolicitudActividad)=>escribir(ruta(g),t,s)
export const completarActividad=(g:string,id:string,t:string,confirmacion?:ConfirmacionCampana)=>escribir(`${ruta(g)}/${encodeURIComponent(id)}/completar`,t,confirmacion)
