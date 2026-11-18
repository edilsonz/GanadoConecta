# AU06 — Consulta de acceso por ganadería

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **incremento backend
implementado y verificado con PostgreSQL**.

## Alcance realizado

- `GET /api/v1/ganaderias/{ganaderiaId}/acceso` exige un JWT válido; el filtro
  revalida sesión y cuenta en PostgreSQL antes de crear la identidad autenticada.
- El caso de uso toma el usuario exclusivamente de esa identidad. No acepta
  usuario, membresía ni rol desde la ruta, el cuerpo, React o los claims JWT.
- La consulta PostgreSQL exige simultáneamente la ganadería solicitada, una
  membresía `ACTIVA` del actor y una ganadería `ACTIVA`.
- El éxito devuelve únicamente `ganaderiaId`, `rol`, `estado` y `validadoEn` UTC,
  con `Cache-Control: no-store`.
- Ganadería inexistente, ajena o inactiva y membresía revocada producen el mismo
  HTTP 403 y código seguro `ACCESO_GANADERIA_DENEGADO`, sin revelar nombres,
  miembros ni identificadores ajenos. Un UUID inválido produce 400 seguro y una
  solicitud sin sesión produce 401.
- La operación es de lectura y conserva la configuración CSRF existente; no se
  desactiva ninguna protección ni se modifica el flujo de cookies o sesiones.

La selección de React continúa siendo únicamente visual y en memoria. Este
endpoint constituye la revalidación online; no crea por sí mismo un contexto
offline, no amplía permisos y deberá invocarse nuevamente en cada futura
operación privada que corresponda.

No se modificaron las migraciones V1–V3.

## Pruebas ejecutadas

| Conjunto | Resultado real |
|---|---|
| Unitarias dirigidas | 7 aprobadas entre creación, listado y consulta; 2 corresponden al nuevo caso de uso y verifican contexto/UTC y denegación indistinguible. |
| Integración dirigida PostgreSQL | `CrearGanaderiaPostgresIntegrationTest`: 12 aprobadas; 4 escenarios nuevos cubren propietario, editor, lector, anónimo, UUID inválido, ajeno, revocado, inactivo e inexistente. |
| Regresión backend final con PostgreSQL | 73 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.12 en `127.0.0.1:5433`; Flyway validó V1–V3 sin cambios. |

No se repitieron las 36 pruebas frontend, TypeScript ni el build porque no se
modificó React. No se ejecutó prueba manual del nuevo endpoint en navegador.

## Trazabilidad y pendientes

| Elemento | Estado |
|---|---|
| AU06 / PA06 | Contexto online e aislamiento por membresía activa verificados; matriz parcial de PROPIETARIO/EDITOR/LECTOR. |
| T02.4 | Creación, listado y `GET /acceso` implementados; futuras APIs aún deben reutilizar autorización por recurso. |
| CA03 / CA06 | Parciales: acceso ajeno/revocado rechazado; faltan recursos de animales y carrera con revocación. |

Permanecen pendientes AU07, revocación de editor/lector, integración React del
contexto, preparación offline, animales e invitaciones. No se declara completo
RF21 ni el gate T02.
