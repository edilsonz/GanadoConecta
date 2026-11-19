package pe.ganadoconecta.modulos.ganado.aplicacion;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
import pe.ganadoconecta.modulos.ganado.dominio.DatosAnimal;

/** Canonización estable v1/v2, independiente del JSON y con longitud prefijada. */
public final class CanonizadorAltaAnimal implements CalculadorHuellaOperacion {
    @Override
    public byte[] calcular(SolicitudRegistroAnimal solicitud, UUID actorUsuarioId) {
        DatosAnimal datos = solicitud.datos();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        agregar(salida, solicitud.tipo());
        agregar(salida, Integer.toString(solicitud.schemaVersion()));
        agregar(salida, Long.toString(solicitud.baseVersion()));
        agregar(salida, solicitud.ganaderiaId().valor().toString());
        agregar(salida, actorUsuarioId.toString());
        agregar(salida, solicitud.animalId().valor().toString());
        agregar(salida, datos.sexo().name());
        agregar(salida, datos.nombre());
        agregar(salida, datos.areteFrente());
        agregar(salida, datos.areteReverso());
        if (solicitud.schemaVersion() == 2) {
            agregar(salida, datos.coloresIdentificacionTextil());
            agregar(salida, datos.ubicacionIdentificacionTextil() == null
                    ? null : datos.ubicacionIdentificacionTextil().name());
        }
        agregar(salida, datos.raza());
        agregar(salida, texto(datos.fechaNacimiento()));
        agregar(salida, texto(datos.nacimientoEstimado()));
        agregar(salida, datos.categoria() == null ? null : datos.categoria().name());
        agregar(salida, datos.observaciones());
        try {
            return MessageDigest.getInstance("SHA-256").digest(salida.toByteArray());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof LocalDate fecha ? fecha.toString() : valor.toString();
    }

    private static void agregar(ByteArrayOutputStream salida, String valor) {
        if (valor == null) {
            salida.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = valor.getBytes(StandardCharsets.UTF_8);
        salida.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        salida.writeBytes(bytes);
    }
}
