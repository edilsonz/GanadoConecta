# T08 — Endpoint seguro de registro individual de animales

Fecha: 18 de noviembre de 2026, America/Lima. Estado: **backend implementado y
verificado; listado/ficha, cliente React y offline pendientes**.

## Alcance implementado

- `POST /api/v1/ganaderias/{ganaderiaId}/animales` recibe el contrato plano de
  SPEC-01. El actor procede exclusivamente del JWT y de la sesión vigentes.
- El adaptador valida la forma HTTP y delega UUIDv4, sexo `MACHO`/`HEMBRA`,
  campos opcionales, categoría, fecha, versiones e idempotencia al núcleo T04–T07.
- La primera confirmación responde `201 APLICADO`; un reintento idéntico responde
  `200 REPROCESADO` con el recibo original. Conflictos no exponen la fila existente.
- Spring Security exige autenticación y CSRF. El filtro existente comprueba el
  origen permitido y limita el cuerpo a 16 KiB; no se inventó un nuevo umbral de
  tasa para animales porque SPEC-01 solo fija numéricamente el de autenticación.
- La autorización se revalida dentro de la transacción. `PROPIETARIO` y `EDITOR`
  escriben; `LECTOR` y membresías revocadas reciben códigos seguros. Ganaderías
  ajenas o inexistentes usan una respuesta indistinguible sin revelar datos.
- Los errores de formato, esquema, versión, campos y persistencia usan respuestas
  seguras con `no-store` y correlación; no incluyen SQL, tokens ni datos ajenos.

No se implementaron T09, formulario React, IndexedDB, sincronización ni una vía
productiva para invitar editores o lectores. Los roles distintos del propietario
se probaron únicamente con fixtures sintéticos.

## Verificación real

PostgreSQL 17.12 en Docker se comprobó `healthy` en `127.0.0.1:5433`. Las
variables privadas ignoradas por Git se cargaron solo en el proceso y no se
imprimieron. Flyway validó V1–V4 sin modificar migraciones ni recrear la base.

| Verificación | Resultado |
|---|---|
| Integración HTTP T08 dirigida | 9 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Pruebas relacionadas de HTTP, núcleo, persistencia y autorización | 25 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Regresión backend/PostgreSQL final | 113 aprobadas, 0 fallos, 0 errores y 0 omitidas. |

La integración cubre alta válida, recibo original, UUID/esquema/versiones,
sexo y opcionales inválidos, propietario/editor/lector, miembro revocado,
ganadería ajena, anónimo, CSRF, origen, tamaño, advertencia de arete, idempotencia
y conflictos. No se ejecutaron pruebas frontend porque React no cambió.

## Trazabilidad y límites

| Elemento | Estado de este incremento |
|---|---|
| T08 | Completada en backend y PostgreSQL. |
| PI01 / PI03 | Parciales: POST, validaciones y autorización cubiertos; listado/ficha de T09 siguen pendientes. |
| PA06 / CA03 / CA06 | Parciales: matriz de escritura y aislamiento comprobados por HTTP con fixtures; falta flujo familiar/productivo integral y offline. |
| CA09 / CA10 / CA12 / CA16 | Parciales: recibos, conflictos, autenticación, CSRF, origen y protocolo HTTP cubiertos; falta el cliente offline. |
| T09 y T10–T18 | Pendientes según dependencias; el alta aún no está disponible desde React. |
