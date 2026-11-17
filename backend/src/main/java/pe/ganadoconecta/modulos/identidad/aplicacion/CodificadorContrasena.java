package pe.ganadoconecta.modulos.identidad.aplicacion;

public interface CodificadorContrasena {
    String codificar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
