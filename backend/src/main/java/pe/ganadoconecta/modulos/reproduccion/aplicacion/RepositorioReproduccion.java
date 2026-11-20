package pe.ganadoconecta.modulos.reproduccion.aplicacion;
import java.util.*; import pe.ganadoconecta.modulos.reproduccion.dominio.EventoReproductivo;
public interface RepositorioReproduccion{ResultadoEventoReproductivo registrar(EventoReproductivo e,UUID op,byte[] huella,int schema);List<EventoReproductivo> listar(UUID ganaderia,UUID animal);}
