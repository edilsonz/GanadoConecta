package pe.ganadoconecta.modulos.identidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

public interface TokensAcceso {
    TokenEmitido emitir(UUID usuarioId, UUID sesionId, Instant emitidoEn, Instant expiraEn);

    IdentidadToken verificar(String token);

    record TokenEmitido(String valor, Instant expiraEn) {
        @Override
        public String toString() {
            return "TokenEmitido[valor=PROTEGIDO, expiraEn=" + expiraEn + "]";
        }
    }

    record IdentidadToken(UUID usuarioId, UUID sesionId, UUID tokenId,
            Instant emitidoEn, Instant expiraEn) {
    }
}
