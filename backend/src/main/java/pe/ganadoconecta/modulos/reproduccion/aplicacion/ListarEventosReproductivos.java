package pe.ganadoconecta.modulos.reproduccion.aplicacion;
import java.util.*;import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;import pe.ganadoconecta.modulos.reproduccion.dominio.EventoReproductivo;
public final class ListarEventosReproductivos{private final AutorizarLecturaGanaderia auth;private final RepositorioReproduccion repo;public ListarEventosReproductivos(AutorizarLecturaGanaderia a,RepositorioReproduccion r){auth=a;repo=r;}public List<EventoReproductivo> ejecutar(UUID actor,UUID g,UUID a){auth.autorizar(actor,g);return repo.listar(g,a);}}
