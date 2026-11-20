package pe.ganadoconecta.modulos.sanidad.aplicacion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.aplicacion.AutorizarLecturaGanaderia;
import pe.ganadoconecta.modulos.sanidad.dominio.EventoSanitario;
import pe.ganadoconecta.modulos.sanidad.dominio.TipoEventoSanitario;

public final class ListarHistorialSanitario {
    private final AutorizarLecturaGanaderia autorizador;
    private final RepositorioEventosSanitarios repositorio;
    public ListarHistorialSanitario(AutorizarLecturaGanaderia autorizador,
            RepositorioEventosSanitarios repositorio) {
        this.autorizador=Objects.requireNonNull(autorizador);
        this.repositorio=Objects.requireNonNull(repositorio);
    }
    public List<EventoSanitario> ejecutar(UUID actor, UUID ganaderia, UUID animal,
            TipoEventoSanitario filtro) {
        autorizador.autorizar(actor, ganaderia);
        return repositorio.listar(ganaderia, animal, filtro);
    }
}
