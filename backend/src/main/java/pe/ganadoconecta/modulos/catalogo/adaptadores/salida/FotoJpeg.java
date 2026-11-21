package pe.ganadoconecta.modulos.catalogo.adaptadores.salida;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.*;
import pe.ganadoconecta.modulos.catalogo.aplicacion.CatalogoAnuncios.Error;

public final class FotoJpeg implements PrepararFoto {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PIXELES = 12_000_000;

    @Override public byte[] aJpeg(byte[] archivo, String tipo) {
        if (archivo.length == 0 || archivo.length > MAX_BYTES
                || (!"image/png".equals(tipo) && !"image/jpeg".equals(tipo))) throw invalido();
        try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(archivo))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw invalido();
            var lector = lectores.next();
            try {
                String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                if (!tipo.equals("image/" + formato)) throw invalido();
                lector.setInput(entrada, true, true);
                int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho > 6000 || alto > 6000
                        || (long) ancho * alto > MAX_PIXELES) throw invalido();
                BufferedImage limpia = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
                var grafico = limpia.createGraphics();
                try {
                    grafico.setColor(Color.WHITE);
                    grafico.fillRect(0, 0, ancho, alto);
                    grafico.drawImage(lector.read(0), 0, 0, null);
                } finally { grafico.dispose(); }
                var salida = new ByteArrayOutputStream();
                if (!ImageIO.write(limpia, "jpeg", salida) || salida.size() > MAX_BYTES) throw invalido();
                return salida.toByteArray();
            } finally { lector.dispose(); }
        } catch (IOException | IllegalArgumentException e) { throw invalido(); }
    }

    private static Fallo invalido() { return new Fallo(Error.INVALIDO); }
}
