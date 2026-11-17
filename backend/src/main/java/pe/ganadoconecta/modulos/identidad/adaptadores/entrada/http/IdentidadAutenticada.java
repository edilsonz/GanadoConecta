package pe.ganadoconecta.modulos.identidad.adaptadores.entrada.http;

import java.util.UUID;

/** Principal mínimo: los roles y membresías nunca se toman del JWT. */
public record IdentidadAutenticada(UUID usuarioId, UUID sesionId) {
}
