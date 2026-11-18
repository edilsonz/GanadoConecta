package pe.ganadoconecta.modulos.ganado.dominio;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Objects;

public record DatosAnimal(
        Sexo sexo,
        String nombre,
        String areteFrente,
        String areteReverso,
        String raza,
        LocalDate fechaNacimiento,
        Boolean nacimientoEstimado,
        Categoria categoria,
        String observaciones) {

    public DatosAnimal {
        if (sexo == null) {
            throw new DatoAnimalInvalidoException("sexo");
        }
        nombre = normalizar(nombre, 100, false, "nombre");
        areteFrente = normalizar(areteFrente, 100, false, "areteFrente");
        areteReverso = normalizar(areteReverso, 100, false, "areteReverso");
        raza = normalizar(raza, 100, false, "raza");
        observaciones = normalizar(observaciones, 2000, true, "observaciones");
        if ((fechaNacimiento == null) != (nacimientoEstimado == null)) {
            throw new DatoAnimalInvalidoException("nacimientoEstimado");
        }
        if (categoria != null && !categoria.correspondeA(sexo)) {
            throw new DatoAnimalInvalidoException("categoria");
        }
    }

    public void validarFechaNoFutura(LocalDate hoyNegocio) {
        Objects.requireNonNull(hoyNegocio);
        if (fechaNacimiento != null && fechaNacimiento.isAfter(hoyNegocio)) {
            throw new DatoAnimalInvalidoException("fechaNacimiento");
        }
    }

    private static String normalizar(String valor, int maximo, boolean admiteNuevaLinea,
            String campo) {
        if (valor == null) {
            return null;
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFC).strip();
        if (normalizado.isEmpty()) {
            return null;
        }
        if (normalizado.codePointCount(0, normalizado.length()) > maximo
                || contieneControlNoPermitido(normalizado, admiteNuevaLinea)) {
            throw new DatoAnimalInvalidoException(campo);
        }
        return normalizado;
    }

    private static boolean contieneControlNoPermitido(String valor,
            boolean admiteNuevaLinea) {
        return valor.codePoints().anyMatch(codigo -> {
            boolean nuevaLineaPermitida = admiteNuevaLinea
                    && (codigo == '\n' || codigo == '\r');
            return !nuevaLineaPermitida && (Character.isISOControl(codigo)
                    || Character.getType(codigo) == Character.FORMAT);
        });
    }
}
