package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.util.Optional;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioAutenticable;

public interface RepositorioCredenciales {
    Optional<UsuarioAutenticable> buscarPorCorreo(CorreoElectronico correo);
}
