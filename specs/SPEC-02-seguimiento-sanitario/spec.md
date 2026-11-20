# SPEC-02 — Seguimiento sanitario individual

**Estado:** Aprobada para los incrementos no clínicos descritos — Implementación en curso

## Objetivo y trazabilidad

Permitir que la familia autorizada conserve hechos sanitarios realizados y
actividades futuras por animal, sin generar diagnósticos, recetas, dosis ni
intervalos veterinarios. Relacionada con RF03, RF04, RF07 y RF22; HU03 y HU04.

## Actores y permisos

- `PROPIETARIO` y `EDITOR`: registran hechos y programan o completan actividades.
- `LECTOR`: consulta historial y agenda, sin modificar información.
- Toda operación valida en el backend la sesión, ganadería, animal y membresía
  activa. Un identificador seleccionado en React nunca concede acceso.

## Incremento 1 — vacunaciones

Una vacunación representa una aplicación ya realizada. Requiere `eventId` UUIDv4,
`operationId` UUIDv4, nombre de vacuna y fecha de aplicación. Responsable,
número de lote, observaciones y próxima fecha prevista son opcionales. La próxima
fecha, si existe, no puede ser anterior a la aplicación y siempre es informada
por el usuario; el sistema no calcula esquemas médicos.

El mismo `operationId` y contenido se reprocesan sin duplicar. Reutilizar un
identificador con otro contenido produce conflicto. Los eventos son inmutables en
estos incrementos: no se eliminan ni corrigen silenciosamente.

## Incremento 2 — historial sanitario

Tipos adicionales: `DESPARASITACION`, `TRATAMIENTO_SANITARIO` y
`REVISION_VETERINARIA`. Requieren fecha y descripción del hecho. Responsable,
producto, indicaciones textuales y observaciones son opcionales y solo registran
lo informado por la persona responsable. El historial unificado se ordena por
fecha descendente y admite filtro por tipo.

## Incremento 3 — agenda

Una actividad futura requiere UUIDv4, animal, tipo, fecha prevista y descripción.
Puede referenciar opcionalmente un evento sanitario. Sus estados son
`PENDIENTE` y `COMPLETADA`; completar la actividad no crea por sí mismo un hecho
clínico. «Vencida» es una presentación de una pendiente cuya fecha ya pasó en
`America/Lima`.

## Offline

Solo se ampliará el protocolo local cuando preserve usuario, ganadería, animal,
UUID, `operationId`, autorización offline vigente, idempotencia y coordinación
entre pestañas. Los pendientes no se borran por vencer la autorización de 168 h.

## Criterios de aceptación

- CS01: propietario/editor registra y lector solo consulta vacunaciones.
- CS02: nombre y fecha se validan; campos opcionales se conservan.
- CS03: reintentos idénticos no duplican y conflictos no sobrescriben.
- CS04: el historial no expone datos entre ganaderías y se ordena por fecha.
- CS05: los otros tres tipos se registran y filtran sin consejo clínico generado.
- CS06: la agenda separa actividades futuras de hechos realizados.
- CS07: pendientes, vencidas y completadas se distinguen usando `America/Lima`.
- CS08: toda escritura revalida membresía activa dentro de la transacción.
- CS09: la ampliación offline, si se implementa, no altera pendientes de animales.

## Fuera de alcance

Campañas colectivas, dosis recomendadas, diagnósticos, prescripciones,
notificaciones externas, edición destructiva y borrado de historial.
