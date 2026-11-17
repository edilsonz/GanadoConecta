package pe.ganadoconecta.modulos.identidad.adaptadores.salida;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.ganadoconecta.modulos.identidad.aplicacion.PersistenciaIdentidadException;
import pe.ganadoconecta.modulos.identidad.aplicacion.RepositorioCredenciales;
import pe.ganadoconecta.modulos.identidad.dominio.CorreoElectronico;
import pe.ganadoconecta.modulos.identidad.dominio.UsuarioAutenticable;

public final class RepositorioCredencialesPostgres implements RepositorioCredenciales {
    private final JdbcTemplate jdbc;

    public RepositorioCredencialesPostgres(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UsuarioAutenticable> buscarPorCorreo(CorreoElectronico correo) {
        try {
            return jdbc.query("""
                    SELECT id, email_normalizado, password_hash, estado
                    FROM identidad.usuario
                    WHERE email_normalizado = ?
                    """, (rs, row) -> new UsuarioAutenticable(
                            rs.getObject("id", java.util.UUID.class),
                            CorreoElectronico.desde(rs.getString("email_normalizado")),
                            rs.getString("password_hash"),
                            UsuarioAutenticable.Estado.valueOf(rs.getString("estado"))),
                    correo.valor()).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new PersistenciaIdentidadException(exception);
        }
    }
}
