package pe.ganadoconecta.modulos.identidad.aplicacion;

import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

public interface RepositorioUsuarios {
    void crear(UsuarioNuevo usuario);
}
