package pe.ganadoconecta.modulos.identidad.dominio;

import java.text.Normalizer;

/** Nombre aprobado por AU05: NFC, recorte exterior y 1–100 caracteres. */
public record NombreGanaderia(String valor) {
    public static NombreGanaderia desde(String entrada) {
        if (entrada == null) {
            throw invalido();
        }
        String normalizado = Normalizer.normalize(entrada.strip(), Normalizer.Form.NFC);
        int longitud = normalizado.codePointCount(0, normalizado.length());
        if (longitud < 1 || longitud > 100) {
            throw invalido();
        }
        return new NombreGanaderia(normalizado);
    }

    private static DatoGanaderiaInvalidoException invalido() {
        return new DatoGanaderiaInvalidoException(
                "nombre", "El nombre debe tener entre 1 y 100 caracteres.");
    }
}
