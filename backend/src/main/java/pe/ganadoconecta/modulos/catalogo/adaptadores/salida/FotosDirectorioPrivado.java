package pe.ganadoconecta.modulos.catalogo.adaptadores.salida;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;

public final class FotosDirectorioPrivado implements Fotos {
    private final Path directorio;

    public FotosDirectorioPrivado(String ruta) {
        directorio = ruta.isBlank() ? null : Path.of(ruta).normalize();
        if (directorio != null && !directorio.isAbsolute()) throw new IllegalArgumentException("Ruta de fotos absoluta requerida");
    }

    @Override public void guardar(UUID id, byte[] jpeg) {
        Path archivo = archivo(id);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Transacción requerida");
        try (var salida = Files.newOutputStream(archivo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int estado) {
                    if (estado == STATUS_ROLLED_BACK) borrarCargaFallida(archivo);
                }
            });
            salida.write(jpeg);
        } catch (IOException e) { throw new Fallo(Error.ALMACEN_NO_DISPONIBLE); }
    }

    @Override public byte[] leer(UUID id) {
        Path archivo = archivo(id);
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) throw new Fallo(Error.AUSENTE);
        try (var entrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            byte[] jpeg = entrada.readNBytes(FotoJpeg.MAX_BYTES + 1);
            if (jpeg.length == 0 || jpeg.length > FotoJpeg.MAX_BYTES) throw new Fallo(Error.ALMACEN_NO_DISPONIBLE);
            return jpeg;
        } catch (IOException e) { throw new Fallo(Error.ALMACEN_NO_DISPONIBLE); }
    }

    private Path archivo(UUID id) {
        if (directorio == null || !Files.isDirectory(directorio, LinkOption.NOFOLLOW_LINKS)) {
            throw new Fallo(Error.ALMACEN_NO_DISPONIBLE);
        }
        return directorio.resolve(id + ".jpg");
    }

    private static void borrarCargaFallida(Path archivo) {
        try { Files.deleteIfExists(archivo); }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(FotosDirectorioPrivado.class)
                    .warn("No se pudo limpiar una foto tras revertir la transacción");
        }
    }
}
