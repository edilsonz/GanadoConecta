# T19-ID — Identificación externa con aretes y lana/cinta

Fecha: 19 de noviembre de 2026, America/Lima. Estado: **implementada y
verificada automáticamente; revisión manual de los campos nuevos pendiente**.

## Cambio controlado

- El UUIDv4 continúa siendo la identidad técnica única del animal.
- Arete frontal y reverso siguen siendo opcionales e independientes.
- `coloresIdentificacionTextil` y `ubicacionIdentificacionTextil` forman un par
  opcional. La ubicación admite `IZQUIERDA`, `DERECHA` o `AMBAS`.
- Se permiten las cuatro combinaciones: solo arete, solo lana/cinta, ambos o
  ninguna identificación externa.
- `schemaVersion=1` conserva exactamente la canonicalización histórica.
  `schemaVersion=2` incorpora el par textil, por lo que los reintentos previos
  mantienen su huella e idempotencia.
- Flyway V6 añade dos columnas anulables y restricciones compatibles. No
  actualiza ni elimina animales existentes y no modifica V1–V5.
- React presenta los campos nuevos en el alta y en la ficha. La autorización
  por ganadería, sexo obligatorio y protección CSRF/JWT no cambiaron.

## Verificación real

| Verificación | Resultado |
|---|---|
| Pruebas unitarias dirigidas de dominio, canonicalización y registro | 13 aprobadas, 0 fallos. |
| Pruebas dirigidas PostgreSQL/HTTP de V6, alta y consulta | 25 aprobadas, 0 fallos. |
| Regresión backend con PostgreSQL 17.12 | 129 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Regresión frontend | 53 aprobadas en 8 archivos, 0 fallos. |
| TypeScript `tsc -b` y build Vite | Correctos; 25 módulos transformados. |
| Flyway | 6 migraciones validadas; esquema en V6, sin `clean` ni recrear volúmenes. |
| Compatibilidad de FIONA después de V6 | Una fila conservada; ambos campos textiles permanecen `null`. |

Las pruebas cubren las cuatro combinaciones de identificación, par textil
incompleto, ubicación desconocida, límite de colores, persistencia, consulta,
roles, aislamiento, idempotencia v2 y compatibilidad de huella v1. PostgreSQL
continuó en Docker con estado `healthy` y puerto externo 5433.

La comprobación manual anterior observó que FIONA seguía registrada después de
F5 y mostraba sus datos. Esa observación acredita el flujo T10 previo, no una
prueba manual de los campos textiles nuevos. No se ejecutó en este incremento
Playwright ni una nueva revisión visual en Chrome.

## Trazabilidad y pendientes

| Elemento | Estado |
|---|---|
| T19-ID | Implementada en dominio, API, PostgreSQL y React. El sufijo distingue este cambio controlado de la tarea superior T19 de carga. |
| CA01 / CA02 | Las cuatro combinaciones y validaciones textiles están cubiertas online. |
| CA05 / CA09 | Replay v1 compatible y replay v2 idempotente verificados. |
| CA03 / CA13 / CA15 | Permisos, no deduplicación por textil y exposición en listado/ficha conservados. |
| T11–T15 | IndexedDB, cola persistente, sincronización y autorización offline siguen pendientes. |

Pendiente para revisión humana: comprobar en Chrome la ergonomía de los campos
de colores/ubicación y la visualización de una ficha creada con lana/cinta. No
se implementaron vacunas, ventas, IA ni otras funciones fuera de T19-ID.
