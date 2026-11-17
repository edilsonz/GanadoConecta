package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.Objects;
import java.util.UUID;
import pe.ganadoconecta.modulos.identidad.dominio.ContrasenaRegistro;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

/** Caso de uso AU01: crea una cuenta, nunca una sesión, rol o membresía. */
public final class RegistrarUsuario {
    private final RepositorioUsuarios repositorio;
    private final CodificadorContrasena codificador;
    private final GeneradorIdUsuario generadorId;

    public RegistrarUsuario(RepositorioUsuarios repositorio,
            CodificadorContrasena codificador, GeneradorIdUsuario generadorId) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.codificador = Objects.requireNonNull(codificador);
        this.generadorId = Objects.requireNonNull(generadorId);
    }

    public Resultado ejecutar(Comando comando) {
        Objects.requireNonNull(comando);
        CorreoElectronico correo = CorreoElectronico.desde(comando.correo());
        ContrasenaRegistro contrasena = ContrasenaRegistro.desde(comando.contrasena());
        UUID id = generadorId.generar();
        String hash = codificador.codificar(contrasena.valor());

        repositorio.crear(new UsuarioNuevo(id, correo, hash));
        return new Resultado(id, correo.valor(), "ACTIVO");
    }

    public record Comando(String correo, String contrasena) {
    }

    public record Resultado(UUID id, String correo, String estado) {
    }
}
