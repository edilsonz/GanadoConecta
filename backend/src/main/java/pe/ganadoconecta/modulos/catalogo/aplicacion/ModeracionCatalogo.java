package pe.ganadoconecta.modulos.catalogo.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Fallo;
import pe.ganadoconecta.modulos.ganado.aplicacion.UnidadDeTrabajo;

public final class ModeracionCatalogo {
    public enum Decision { DESESTIMADO, RETIRADO }
    public record Reporte(UUID id, UUID anuncioId, String titulo, String estadoAnuncio, String motivo,
            String estado, Instant recibidoEn, Instant revisadoEn) {}
    public static final class AccesoDenegado extends RuntimeException {}
    public interface Repositorio {
        boolean cuentaVigente(UUID actor, UUID sesion, Instant ahora);
        boolean permisoVigente(UUID actor);
        void reportar(UUID id, UUID anuncio, UUID actor, String motivo, Instant ahora);
        List<Reporte> bandeja();
        void revisar(UUID reporte, UUID actor, Decision decision, Instant ahora);
    }
    private final UnidadDeTrabajo unidad;
    private final Repositorio repo;
    private final Clock reloj;
    private final boolean recepcionExperimental;

    public ModeracionCatalogo(UnidadDeTrabajo unidad,Repositorio repo,Clock reloj,boolean recepcionExperimental) {
        this.unidad=unidad; this.repo=repo; this.reloj=reloj; this.recepcionExperimental=recepcionExperimental;
    }

    public boolean recepcionExperimental() { return recepcionExperimental; }

    public UUID reportar(UUID anuncio, UUID actor, UUID sesion, String motivo) {
        if (!recepcionExperimental) throw new Fallo(Error.AUSENTE);
        String texto = CatalogoAnuncios.texto(motivo,1000,true);
        if (texto.codePointCount(0,texto.length()) < 5) throw new Fallo(Error.INVALIDO);
        return unidad.ejecutar(() -> {
            Instant ahora = reloj.instant();
            if (!repo.cuentaVigente(actor,sesion,ahora)) throw new AccesoDenegado();
            UUID id=UUID.randomUUID();
            repo.reportar(id,anuncio,actor,texto,ahora);
            return id;
        });
    }

    public List<Reporte> bandeja(UUID actor,UUID sesion) {
        return unidad.ejecutar(() -> { autorizar(actor,sesion); return repo.bandeja(); });
    }

    public void revisar(UUID id,UUID actor,UUID sesion,Decision decision) {
        if (decision == null) throw new Fallo(Error.INVALIDO);
        unidad.ejecutar(() -> {
            autorizar(actor,sesion);
            repo.revisar(id,actor,decision,reloj.instant());
            return true;
        });
    }

    private void autorizar(UUID actor,UUID sesion) {
        if (!repo.cuentaVigente(actor,sesion,reloj.instant()) || !repo.permisoVigente(actor)) throw new AccesoDenegado();
    }
}
