package pe.ganadoconecta.modulos.identidad.aplicacion;

public interface GeneradorCredencialesRenovacion {
    CredencialRenovacion generar();

    String calcularHuella(String valor);

    record CredencialRenovacion(String valor, String huellaSha256) {
        @Override
        public String toString() {
            return "CredencialRenovacion[valor=PROTEGIDO, huella=PROTEGIDA]";
        }
    }
}
