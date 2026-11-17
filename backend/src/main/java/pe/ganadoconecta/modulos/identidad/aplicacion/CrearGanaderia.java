package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.DatoGanaderiaInvalidoException;
import pe.ganadoconecta.modulos.identidad.dominio.GanaderiaNueva;
import pe.ganadoconecta.modulos.identidad.dominio.NombreGanaderia;

/** AU05: crea una ganadería y hace propietario al usuario autenticado. */
public final class CrearGanaderia {
    private final RepositorioGanaderias repositorio;
    private final GeneradorIdMembresia generadorMembresia;

    public CrearGanaderia(RepositorioGanaderias repositorio,
            GeneradorIdMembresia generadorMembresia) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.generadorMembresia = Objects.requireNonNull(generadorMembresia);
    }

    public Resultado ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        UUID id = idValido(comando.id());
        UUID actor = Objects.requireNonNull(comando.actorUsuarioId());
        NombreGanaderia nombre = NombreGanaderia.desde(comando.nombre());
        GanaderiaNueva nueva = new GanaderiaNueva(id, nombre, actor);
        RepositorioGanaderias.ResultadoCreacion estado = repositorio.crear(
                nueva, generadorMembresia.generar());
        return new Resultado(id, nombre.valor(), "ACTIVA", "PROPIETARIO",
                estado == RepositorioGanaderias.ResultadoCreacion.CREADA);
    }

    private UUID idValido(String entrada) {
        if (entrada == null || !entrada.equals(entrada.strip())) {
            throw idInvalido();
        }
        try {
            UUID id = UUID.fromString(entrada.toLowerCase(Locale.ROOT));
            if (!id.toString().equals(entrada.toLowerCase(Locale.ROOT))
                    || id.version() != 4 || id.variant() != 2) {
                throw idInvalido();
            }
            return id;
        } catch (IllegalArgumentException exception) {
            throw idInvalido();
        }
    }

    private DatoGanaderiaInvalidoException idInvalido() {
        return new DatoGanaderiaInvalidoException(
                "id", "El identificador debe ser un UUIDv4 válido.");
    }

    public record Comando(String id, String nombre, UUID actorUsuarioId) {
    }

    public record Resultado(UUID id, String nombre, String estado, String rol,
            boolean creada) {
    }
}
