package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.CorreoNoDisponibleException;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaUsuarioException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioUsuarios;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioNuevo;

/** Adaptador JDBC; la restricción única resuelve también carreras concurrentes. */
public final class RepositorioUsuariosPostgres implements RepositorioUsuarios {
    private final JdbcTemplate jdbc;

    public RepositorioUsuariosPostgres(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void crear(UsuarioNuevo usuario) {
        try {
            jdbc.update("""
                    INSERT INTO identidad.usuario (id, email_normalizado, password_hash)
                    VALUES (?, ?, ?)
                    """, usuario.id(), usuario.correo().valor(), usuario.passwordHash());
        } catch (DuplicateKeyException exception) {
            throw new CorreoNoDisponibleException();
        } catch (DataAccessException exception) {
            throw new PersistenciaUsuarioException(exception);
        }
    }
}
