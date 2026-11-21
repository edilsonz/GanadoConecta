package pe.ganadoconecta.modulos.sanidad.adaptadores.salida;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.ganadoconecta.modulos.ganado.aplicacion.AnimalNoEncontradoException;
import pe.ganadoconecta.modulos.sanidad.aplicacion.*;
import pe.ganadoconecta.modulos.sanidad.dominio.*;

public final class RepositorioAgendaSanitariaPostgres implements RepositorioAgendaSanitaria {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String CONSULTA = """
            SELECT a.*,
              ARRAY(SELECT p.animal_id FROM sanidad.participante_agenda p
                    WHERE p.actividad_id=a.id) participantes,
              ARRAY(SELECT p.animal_id FROM sanidad.participante_agenda p
                    WHERE p.actividad_id=a.id AND p.evento_confirmado_id IS NOT NULL) atendidos
            FROM sanidad.actividad_agenda a
            """;
    private final JdbcTemplate jdbc;

    public RepositorioAgendaSanitariaPostgres(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public ActividadSanitaria crear(ActividadSanitaria a) {
        transaccion();
        try {
            var anterior = buscar(a.ganaderiaId(), a.id(), false);
            if (anterior != null) return verificarReenvio(a, anterior);
            a.validarFechaProgramable(a.creadoEn().atZone(LIMA).toLocalDate());
            if (a.animalId() != null) exigirAnimal(a.ganaderiaId(), a.animalId());
            for (UUID animal : a.participantes()) exigirAnimal(a.ganaderiaId(), animal);
            if (a.eventoOrigenId() != null && !Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM sanidad.evento_sanitario
                        WHERE id=? AND ganaderia_id=? AND animal_id=?)
                    """, Boolean.class, a.eventoOrigenId(), a.ganaderiaId(), a.animalId()))) {
                throw new DatoSanitarioInvalidoException("eventoOrigenId");
            }
            int insertadas = jdbc.update("""
                    INSERT INTO sanidad.actividad_agenda
                        (id,ganaderia_id,animal_id,evento_origen_id,tipo,fecha_prevista,descripcion,
                         observaciones,creado_por,creado_en)
                    VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING
                    """, a.id(), a.ganaderiaId(), a.animalId(), a.eventoOrigenId(), a.tipo().name(),
                    a.fechaPrevista(), a.descripcion(), a.observaciones(), a.creadoPor(), fecha(a.creadoEn()));
            if (insertadas == 0) throw new ConflictoSanitarioException();
            for (UUID animal : a.participantes()) {
                jdbc.update("""
                        INSERT INTO sanidad.participante_agenda(actividad_id,ganaderia_id,animal_id)
                        VALUES(?,?,?)
                        """, a.id(), a.ganaderiaId(), animal);
            }
            return a;
        } catch (DataAccessException e) { throw new PersistenciaSanitariaException(e); }
    }

    private ActividadSanitaria verificarReenvio(ActividadSanitaria a, ActividadSanitaria x) {
        if (!Objects.equals(x.animalId(), a.animalId()) || !x.participantes().equals(a.participantes())
                || !Objects.equals(x.eventoOrigenId(), a.eventoOrigenId()) || x.tipo() != a.tipo()
                || !x.fechaPrevista().equals(a.fechaPrevista()) || !x.descripcion().equals(a.descripcion())
                || !Objects.equals(x.observaciones(), a.observaciones()) || !x.creadoPor().equals(a.creadoPor())) {
            throw new ConflictoSanitarioException();
        }
        return x;
    }

    @Override public List<ActividadSanitaria> listar(UUID g, UUID animal, TipoActividadSanitaria tipo,
            EstadoActividadSanitaria estado) {
        try {
            String t = tipo == null ? null : tipo.name(), s = estado == null ? null : estado.name();
            return jdbc.query(CONSULTA + """
                    WHERE a.ganaderia_id=? AND (CAST(? AS UUID) IS NULL OR a.animal_id=?
                      OR EXISTS(SELECT 1 FROM sanidad.participante_agenda p
                        WHERE p.actividad_id=a.id AND p.animal_id=?))
                      AND (CAST(? AS VARCHAR) IS NULL OR a.tipo=?)
                      AND (CAST(? AS VARCHAR) IS NULL OR a.estado=?)
                    ORDER BY a.fecha_prevista,a.id
                    """, (rs,n) -> map(rs), g, animal, animal, animal, t, t, s, s);
        } catch (DataAccessException e) { throw new PersistenciaSanitariaException(e); }
    }

    @Override public ActividadSanitaria completar(UUID g, UUID id, UUID actor, Instant ahora,
            ConfirmacionActividad confirmacion) {
        transaccion();
        try {
            var actual = buscar(g, id, true);
            if (actual == null) throw new ActividadAgendaNoEncontradaException();
            var tipoEvento = actual.tipo().eventoDeCampana();
            if ((tipoEvento == null) != (confirmacion == null)) throw new DatoSanitarioInvalidoException("atendidos");
            if (actual.estado() == EstadoActividadSanitaria.COMPLETADA) {
                if (confirmacion != null && (!actual.atendidos().equals(confirmacion.atendidos())
                        || !Objects.equals(actual.fechaAtencion(), confirmacion.fechaAtencion())
                        || !Objects.equals(actual.nombreVacuna(), confirmacion.nombreVacuna()))) {
                    throw new ConflictoSanitarioException();
                }
                return actual;
            }
            LocalDate fechaAtencion = null;
            String vacuna = null;
            if (confirmacion != null) {
                fechaAtencion = confirmacion.fechaAtencion();
                vacuna = confirmacion.nombreVacuna();
                if (confirmacion.atendidos().isEmpty() || !actual.participantes().containsAll(confirmacion.atendidos())) {
                    throw new DatoSanitarioInvalidoException("atendidos");
                }
                if ((tipoEvento == TipoEventoSanitario.VACUNACION) != (vacuna != null)) {
                    throw new DatoSanitarioInvalidoException("nombreVacuna");
                }
                for (UUID animal : confirmacion.atendidos()) {
                    var evento = new EventoSanitario(UUID.randomUUID(), g, animal, tipoEvento, fechaAtencion,
                            vacuna, actual.descripcion(), null, null, null, null, actual.observaciones(), null, actor, ahora);
                    evento.validarRealizadoNoFuturo(ahora.atZone(LIMA).toLocalDate());
                    registrarAtencion(id, evento);
                }
            }
            jdbc.update("""
                    UPDATE sanidad.actividad_agenda
                    SET estado='COMPLETADA',version=2,completado_por=?,completado_en=?,fecha_atencion=?,nombre_vacuna=?
                    WHERE ganaderia_id=? AND id=?
                    """, actor, fecha(ahora), fechaAtencion, vacuna, g, id);
            return buscar(g, id, false);
        } catch (DataAccessException e) { throw new PersistenciaSanitariaException(e); }
    }

    private void registrarAtencion(UUID actividad, EventoSanitario e) {
        jdbc.update("""
                INSERT INTO sanidad.evento_sanitario
                    (id,ganaderia_id,animal_id,tipo,fecha_evento,nombre,descripcion,observaciones,creado_por,creado_en)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, e.id(), e.ganaderiaId(), e.animalId(), e.tipo().name(), e.fecha(), e.nombre(),
                e.descripcion(), e.observaciones(), e.creadoPor(), fecha(e.creadoEn()));
        jdbc.update("""
                UPDATE sanidad.participante_agenda SET evento_confirmado_id=?
                WHERE actividad_id=? AND ganaderia_id=? AND animal_id=?
                """, e.id(), actividad, e.ganaderiaId(), e.animalId());
    }

    private ActividadSanitaria buscar(UUID g, UUID id, boolean bloquear) {
        return jdbc.query(CONSULTA + "WHERE a.ganaderia_id=? AND a.id=?"
                + (bloquear ? " FOR UPDATE OF a" : ""), (rs,n) -> map(rs), g, id).stream().findFirst().orElse(null);
    }

    private void exigirAnimal(UUID g, UUID a) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM ganado.animal WHERE ganaderia_id=? AND id=?)",
                Boolean.class,g,a))) throw new AnimalNoEncontradoException();
    }

    private static ActividadSanitaria map(ResultSet r) throws SQLException {
        var completada = r.getObject("completado_en", OffsetDateTime.class);
        return new ActividadSanitaria(r.getObject("id",UUID.class), r.getObject("ganaderia_id",UUID.class),
                r.getObject("animal_id",UUID.class), r.getObject("evento_origen_id",UUID.class),
                TipoActividadSanitaria.valueOf(r.getString("tipo")), r.getObject("fecha_prevista",LocalDate.class),
                r.getString("descripcion"), r.getString("observaciones"), EstadoActividadSanitaria.valueOf(r.getString("estado")),
                r.getLong("version"), r.getObject("creado_por",UUID.class), r.getObject("creado_en",OffsetDateTime.class).toInstant(),
                r.getObject("completado_por",UUID.class), completada == null ? null : completada.toInstant(),
                Arrays.asList((UUID[])r.getArray("participantes").getArray()),
                Arrays.asList((UUID[])r.getArray("atendidos").getArray()), r.getObject("fecha_atencion",LocalDate.class),
                r.getString("nombre_vacuna"));
    }

    private static OffsetDateTime fecha(Instant ahora) { return ahora.atOffset(ZoneOffset.UTC); }

    private static void transaccion() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("La agenda requiere una transacción");
        }
    }
}
