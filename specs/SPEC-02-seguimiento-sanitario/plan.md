# Plan técnico — SPEC-02

- Módulo backend `sanidad`, con dominio y casos de uso sin dependencias HTTP/JDBC.
- PostgreSQL es la fuente de verdad. `V7` crea eventos y comprobantes idempotentes
  con claves foráneas compuestas hacia animal y ganadería.
- REST bajo `/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}`; JWT identifica
  al actor, CSRF protege escrituras y las respuestas privadas usan `no-store`.
- Las escrituras usan la unidad de trabajo existente y el autorizador bloqueante
  para impedir una escritura posterior a una revocación confirmada.
- React amplía la ficha individual y conserva JWT solo en memoria.
- El motor offline único procesa las colas de animales y sanidad mediante los
  mismos Web Locks, leases, tiempos límite, reintentos y barreras de
  autorización. IndexedDB V3 conserva stores separados y compatibles; el
  orquestador discrimina `CREAR_ANIMAL`, vacunación y evento sanitario sin
  alterar los sobres existentes. Solo una respuesta HTTP válida con los mismos
  `operationId`/identificador de entidad y resultado `APLICADO` o `REPROCESADO`
  permite persistir el comprobante y marcar `SINCRONIZADO`.

## Contrato HTTP de vacunaciones

GET/POST `/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}/vacunaciones`.
POST acepta `operationId`, `eventId` UUIDv4, `schemaVersion: 1`,
`nombreVacuna` (1–150 caracteres), `fechaAplicacion` (fecha no futura en Lima),
`responsable` (opcional, máximo 150), `lote` (100), `observaciones` (2000) y
`proximaFecha` opcional igual o posterior a la aplicación. No acepta campos extra.
201 aplicado/200 reprocesado devuelven `operationId`, `eventId`, `resultado` y
`confirmadoEn`. 409 indica conflicto; 422 datos inválidos; 401/403 acceso;
404 animal ausente en el ámbito autorizado. GET devuelve un arreglo ordenado por
fecha de aplicación descendente, creación descendente e ID, con esos datos
descriptivos, `eventId` y `creadoEn`; no expone información de la cuenta.

Pruebas: JUnit/MockMvc con PostgreSQL aislado y rollback para fixtures;
Vitest/Testing Library para estados, permisos y reintentos React.
