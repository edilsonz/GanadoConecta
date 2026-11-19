# T09 — Listado y ficha individual de animales

Fecha: 18 de noviembre de 2026, America/Lima. Estado: **backend implementado y
verificado; cliente React y funcionamiento offline pendientes**.

## Alcance implementado

- `GET /api/v1/ganaderias/{ganaderiaId}/animales` devuelve `items`,
  `siguienteCursor` y `consultadoEn`. Usa `limit=20` por defecto, admite 1–100
  y ordena por `creadoEn DESC, animalId DESC` mediante cursor opaco.
- `GET /api/v1/ganaderias/{ganaderiaId}/animales/{animalId}` devuelve la ficha
  básica aprobada, con ganadería, campos descriptivos, estado, versión y fecha
  del servidor; no expone `creadoPor` ni datos de otras ganaderías.
- Ambos casos obtienen el actor del JWT validado y reutilizan el autorizador de
  Identidad con permiso `LEER`. Propietario, editor y lector activos consultan;
  membresías revocadas y usuarios ajenos reciben rechazo seguro.
- La búsqueda usa siempre `(ganaderiaId, animalId)`. Un UUID inexistente y un
  animal de otra ganadería producen el mismo 404 sin revelar pertenencia.
- V5 reemplaza únicamente el índice de listado para alinear `id DESC` con el
  contrato. V1–V4 no se modificaron y no se alteraron datos existentes.

No se implementaron formulario/listado React, IndexedDB, sincronización,
invitaciones, vacunas, catálogo ni IA. La advertencia `ARETE_COINCIDENTE` sigue
formando parte del recibo de alta T08; T09 no introduce otra regla de aretes.

## Verificación real

PostgreSQL 17.12 en Docker se comprobó `healthy` en `127.0.0.1:5433`. Flyway
validó y aplicó cinco migraciones; el esquema quedó en V5 sin `clean`, borrado de
volúmenes ni modificación de migraciones ya aplicadas.

| Verificación | Resultado |
|---|---|
| Casos de uso T09 dirigidos | 4 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Integración HTTP T09 dirigida | 7 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Regresión relacionada de T08 | 9 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Regresión backend/PostgreSQL final | 124 aprobadas, 0 fallos, 0 errores y 0 omitidas. |

La integración cubre listado vacío y paginado, desempate estable, ficha,
propietario/editor/lector, miembro revocado, usuario ajeno, anónimo, animal
inexistente o de otra ganadería y parámetros/cursor inválidos. No se ejecutaron
pruebas frontend porque React no cambió. Tampoco se ejecutó PC01 de carga: queda
pendiente hasta disponer del flujo de usuario y del dataset reproducible aprobado.

## Trazabilidad y límites

| Elemento | Estado de este incremento |
|---|---|
| T09 | Completada en backend y PostgreSQL. |
| PI03 / CA15 | Parciales: listado/ficha, aislamiento, orden y paginación online cubiertos; falta cliente/offline. |
| CA03 / PA06 / PA08 | Parciales: tres roles activos y revocación comprobados con fixtures; no existe alta familiar productiva. |
| CA13 | La advertencia de arete continúa verificada en T08; T09 no fusiona ni publica coincidencias. |
| CA18 / PC01 | Pendientes; no se afirma rendimiento p95 ni carga concurrente. |
