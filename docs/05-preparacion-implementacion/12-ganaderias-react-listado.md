# AU05–AU06 — Creación, listado y selección de ganaderías

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **incremento implementado
y verificado con PostgreSQL**.

## Alcance realizado

- `GET /api/v1/ganaderias` exige JWT válido y obtiene el usuario exclusivamente
  de la identidad autenticada.
- La consulta filtra en PostgreSQL por usuario, membresía `ACTIVA` y ganadería
  `ACTIVA`; devuelve solo `id`, `nombre`, `estado` y rol vigente.
- Una membresía revocada y una ganadería ajena no aparecen. La respuesta usa
  `Cache-Control: no-store`.
- React comparte la sesión ya existente, lista las ganaderías autorizadas y
  conecta la creación al `POST /api/v1/ganaderias` implementado en AU05.
- El cliente genera un UUIDv4 una sola vez por intento lógico. Si renueva un
  CSRF inválido, reenvía exactamente el mismo UUID y cuerpo para conservar la
  idempotencia.
- La selección se conserva únicamente en memoria de React. No se guarda JWT,
  credencial, ganadería seleccionada ni rol en `localStorage`, `sessionStorage`
  o IndexedDB.
- La interfaz representa carga, lista vacía, validación, conflicto, sesión
  vencida y errores de red/servicio; «Mi ganado» continúa deshabilitado.

No se modificaron las migraciones V1–V3. El contrato
`GET /ganaderias/{id}/acceso` no se implementa en este incremento: pertenece a
la validación de contexto de AU06 necesaria antes de preparar autorización
offline o ejecutar operaciones futuras por ganadería. La selección visual no
lo sustituye ni concede permisos.

## Pruebas ejecutadas

| Conjunto | Resultado real |
|---|---|
| Unitarias backend dirigidas | `CrearGanaderiaTest` y `ListarGanaderiasTest`: 5 aprobadas. |
| Frontend dirigido | 15 aprobadas para API/panel/App: JWT y CSRF, UUID estable en reintento, vacío, listado, selección, creación, conflicto y sesión inválida. |
| Regresión frontend final | 36 pruebas aprobadas, 0 fallos, en 6 archivos. |
| TypeScript y build | `npm.cmd run typecheck` y `npm.cmd run build`: correctos; Vite transformó 23 módulos. |
| `CrearGanaderiaPostgresIntegrationTest` | 8 pruebas aprobadas: creación/atomicidad, autenticación y CSRF, listado vacío, aislamiento entre usuarios y exclusión de membresías revocadas. |
| Regresión backend final con PostgreSQL | 67 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.12 `healthy` en `127.0.0.1:5433`; Flyway validó V1–V3 sin aplicar cambios. |
| Revisión manual HTTPS | El responsable creó una ganadería desde React, la vio con rol Propietario, la seleccionó y comprobó que tras F5 seguía en el listado. La selección se perdió al recargar, conforme a su almacenamiento únicamente en memoria. |

Esta revisión manual no acredita inspección directa de cookies, códigos HTTP ni
almacenamiento mediante DevTools; esas comprobaciones no se declaran realizadas.

## Trazabilidad

| Elemento | Estado de este incremento |
|---|---|
| AU05 / PA05 | Backend y cliente React crean con UUIDv4, JWT y CSRF; persistencia y regresión PostgreSQL verificadas. |
| AU06 / PA06 | Parcial: listado aislado por membresía activa y rol; faltan `/acceso`, autorización de recursos por ganadería y revocación. |
| T02.4 | Creación y listado propio implementados; contexto `/acceso` pendiente. |
| T02.6 | Creación, lista y selección en memoria implementadas; offline y multipestaña pendientes. |
| CA03 | Parcial: propietario inicial y aislamiento del listado; no equivale a matriz integral ni habilita animales. |

## Pendientes explícitos

- Implementar y verificar `/ganaderias/{id}/acceso`, autorización por recurso,
  revocación y matriz completa de roles en sus incrementos correspondientes.
- No existen invitaciones, alta real de editor/lector, animales ni autorización
  offline.
