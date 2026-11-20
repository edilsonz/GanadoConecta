import { codigoError, ErrorRegistro, esObjeto, leerJson, obtenerTokenCsrf } from './authApi'
import { ErrorSanidad } from './vacunacionesApi'

export type TipoEventoSanitario='VACUNACION'|'DESPARASITACION'|'TRATAMIENTO_SANITARIO'|'REVISION_VETERINARIA'
export type EventoSanitario={eventId:string;tipo:TipoEventoSanitario;fecha:string;titulo:string;
  descripcion:string|null;responsable:string|null;lote:string|null;producto:string|null;
  indicaciones:string|null;observaciones:string|null;proximaFecha:string|null;creadoEn:string}
export type SolicitudEventoSanitario={operationId:string;eventId:string;schemaVersion:1;
  tipo:Exclude<TipoEventoSanitario,'VACUNACION'>;fecha:string;descripcion:string;
  responsable?:string;producto?:string;indicaciones?:string;observaciones?:string}
export type ResultadoEventoSanitario={operationId:string;eventId:string;
  resultado:'APLICADO'|'REPROCESADO';confirmadoEn:string}
const ruta=(g:string,a:string)=>`/api/v1/ganaderias/${encodeURIComponent(g)}/animales/${encodeURIComponent(a)}/eventos-sanitarios`
function error(r:Response,b:unknown):never{
  if(r.status===401)throw new ErrorSanidad('sesion')
  if(r.status===403)throw new ErrorSanidad(['CSRF_INVALIDO','ORIGEN_NO_PERMITIDO'].includes(codigoError(b)??'')?'seguridad':'permiso')
  if(r.status===409)throw new ErrorSanidad('conflicto')
  if([400,404,422].includes(r.status))throw new ErrorSanidad('validacion')
  throw new ErrorSanidad('temporal')
}
function esEvento(v:unknown):v is EventoSanitario{return esObjeto(v)&&typeof v.eventId==='string'
  &&['VACUNACION','DESPARASITACION','TRATAMIENTO_SANITARIO','REVISION_VETERINARIA'].includes(String(v.tipo))
  &&typeof v.fecha==='string'&&typeof v.titulo==='string'&&typeof v.creadoEn==='string'}
export async function listarHistorialSanitario(g:string,a:string,token:string,tipo?:TipoEventoSanitario){
  const q=tipo?`?tipo=${tipo}`:'';const r=await fetch(ruta(g,a)+q,{credentials:'same-origin',cache:'no-store',headers:{Authorization:`Bearer ${token}`}})
  const b=await leerJson(r);if(r.ok&&Array.isArray(b)&&b.every(esEvento))return b as EventoSanitario[];error(r,b)
}
export async function registrarEventoSanitario(g:string,a:string,token:string,s:SolicitudEventoSanitario,signal?:AbortSignal):Promise<ResultadoEventoSanitario>{
  try {
    for(let i=0;i<2;i++){const csrf=await obtenerTokenCsrf(signal);const r=await fetch(ruta(g,a),{method:'POST',credentials:'same-origin',cache:'no-store',
      headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json','X-CSRF-TOKEN':csrf},body:JSON.stringify(s),signal})
      const b=await leerJson(r);if(r.status===403&&codigoError(b)==='CSRF_INVALIDO'&&i===0)continue
      if([200,201].includes(r.status)&&esObjeto(b)&&b.eventId===s.eventId&&b.operationId===s.operationId
        &&['APLICADO','REPROCESADO'].includes(String(b.resultado))&&typeof b.confirmadoEn==='string'
        &&Number.isFinite(Date.parse(b.confirmadoEn)))return b as ResultadoEventoSanitario
      error(r,b)}
  } catch (fallo) {
    if (fallo instanceof ErrorSanidad) throw fallo
    if (fallo instanceof ErrorRegistro) {
      if (fallo.tipo === 'conexion') throw new ErrorSanidad('conexion')
      if (fallo.tipo === 'seguridad') throw new ErrorSanidad('seguridad')
      throw new ErrorSanidad('temporal')
    }
    throw new ErrorSanidad('conexion')
  }
  throw new ErrorSanidad('temporal')
}
