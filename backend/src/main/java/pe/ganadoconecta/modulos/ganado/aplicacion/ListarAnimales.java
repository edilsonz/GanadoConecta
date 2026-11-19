package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.dominio.Animal;
import pe.ganadoconecta.modulos.ganado.dominio.GanaderiaId;

public final class ListarAnimales {
    public static final int LIMITE_PREDETERMINADO = 20;
    public static final int LIMITE_MAXIMO = 100;

    private final AutorizarLecturaGanaderia autorizador;
    private final ConsultasAnimales consultas;
    private final Clock reloj;

    public ListarAnimales(AutorizarLecturaGanaderia autorizador,
            ConsultasAnimales consultas, Clock reloj) {
        this.autorizador = Objects.requireNonNull(autorizador);
        this.consultas = Objects.requireNonNull(consultas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public PaginaAnimales ejecutar(UUID actorUsuarioId, GanaderiaId ganaderiaId,
            int limite, CursorListadoAnimales cursor) {
        Objects.requireNonNull(actorUsuarioId);
        Objects.requireNonNull(ganaderiaId);
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new ConsultaAnimalesInvalidaException();
        }
        autorizador.autorizar(actorUsuarioId, ganaderiaId.valor());
        List<Animal> encontrados = consultas.listar(ganaderiaId, limite + 1, cursor);
        boolean haySiguiente = encontrados.size() > limite;
        List<Animal> items = haySiguiente
                ? List.copyOf(encontrados.subList(0, limite))
                : List.copyOf(encontrados);
        CursorListadoAnimales siguiente = haySiguiente
                ? cursorDe(items.getLast()) : null;
        return new PaginaAnimales(items, siguiente, reloj.instant());
    }

    private static CursorListadoAnimales cursorDe(Animal animal) {
        return new CursorListadoAnimales(animal.creadoEn(), animal.id());
    }
}
