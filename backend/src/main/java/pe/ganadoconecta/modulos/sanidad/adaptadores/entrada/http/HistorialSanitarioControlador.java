package pe.ganadoconecta.modulos.sanidad.adaptadores.entrada.http;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http.IdentidadAutenticada;
import pe.ganadoconecta.modulos.sanidad.aplicacion.*;
import pe.ganadoconecta.modulos.sanidad.dominio.*;

@RestController
@RequestMapping("/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}/eventos-sanitarios")
@Profile("postgres")
public class HistorialSanitarioControlador {
    private final RegistrarEventoSanitario registrar;
    private final ListarHistorialSanitario listar;
    public HistorialSanitarioControlador(RegistrarEventoSanitario registrar,
            ListarHistorialSanitario listar){this.registrar=registrar;this.listar=listar;}
    @PostMapping ResponseEntity<ResultadoRespuesta> registrar(@PathVariable UUID ganaderiaId,
            @PathVariable UUID animalId,@RequestBody RegistroSolicitud body,
            @RequestParam MultiValueMap<String,String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad){
        validarParametros(parametros,Set.of());
        var r=registrar.ejecutar(body.aDominio(ganaderiaId,animalId),
                identidad.usuarioId(),identidad.sesionId());
        return ResponseEntity.status(r.estado()==ResultadoEventoSanitario.Estado.APLICADO?201:200)
                .cacheControl(CacheControl.noStore()).body(new ResultadoRespuesta(
                    r.operationId(),r.eventId(),r.estado().name(),r.confirmadoEn()));
    }
    @GetMapping ResponseEntity<List<EventoRespuesta>> listar(@PathVariable UUID ganaderiaId,
            @PathVariable UUID animalId,@RequestParam(required=false) String tipo,
            @RequestParam MultiValueMap<String,String> parametros,
            @AuthenticationPrincipal IdentidadAutenticada identidad){
        validarParametros(parametros,Set.of("tipo"));
        TipoEventoSanitario filtro=tipo(tipo,true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(listar.ejecutar(
                identidad.usuarioId(),ganaderiaId,animalId,filtro).stream()
                .map(EventoRespuesta::desde).toList());
    }
    private static void validarParametros(MultiValueMap<String,String> p,Set<String> permitidos){
        if(!permitidos.containsAll(p.keySet())||p.values().stream().anyMatch(v->v.size()!=1))
            throw new SolicitudSanitariaInvalidaException();
    }
    private static TipoEventoSanitario tipo(String valor,boolean permiteNulo){
        if(valor==null&&permiteNulo)return null;
        try{return TipoEventoSanitario.valueOf(valor);}catch(Exception ex){
            throw new DatoSanitarioInvalidoException("tipo");}
    }
    record RegistroSolicitud(UUID operationId,UUID eventId,Integer schemaVersion,
            String tipo,LocalDate fecha,String descripcion,String responsable,
            String producto,String indicaciones,String observaciones){
        SolicitudEventoSanitario aDominio(UUID g,UUID a){return new SolicitudEventoSanitario(
            g,a,eventId,operationId,schemaVersion==null?0:schemaVersion,
            HistorialSanitarioControlador.tipo(tipo,false),
            fecha,descripcion,responsable,producto,indicaciones,observaciones);}
        @Override public String toString(){return "RegistroSolicitud[datos=PROTEGIDOS]";}
    }
    record ResultadoRespuesta(UUID operationId,UUID eventId,String resultado,Instant confirmadoEn){}
    record EventoRespuesta(UUID eventId,String tipo,LocalDate fecha,String titulo,
            String descripcion,String responsable,String lote,String producto,
            String indicaciones,String observaciones,LocalDate proximaFecha,Instant creadoEn){
        static EventoRespuesta desde(EventoSanitario e){return new EventoRespuesta(e.id(),
            e.tipo().name(),e.fecha(),e.nombre()!=null?e.nombre():e.descripcion(),e.descripcion(),
            e.responsable(),e.lote(),e.producto(),e.indicaciones(),e.observaciones(),
            e.proximaFecha(),e.creadoEn());}
    }
}
