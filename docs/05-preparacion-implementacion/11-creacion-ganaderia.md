# AU05 — Creación de ganadería y propietario inicial

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **primer incremento backend
implementado y verificado; interfaz React, listado y contexto de acceso pendientes**.

## Alcance realizado

- `POST /api/v1/ganaderias` exige JWT válido y CSRF.
- El cuerpo acepta únicamente `id` UUIDv4 y `nombre`; no acepta usuario,
  propietario, rol ni estado.
- El usuario propietario se obtiene de `IdentidadAutenticada`, creada después de
  validar JWT, sesión y cuenta en PostgreSQL.
- El nombre se recorta, normaliza a NFC y exige entre 1 y 100 caracteres Unicode.
- Ganadería ACTIVA y membresía ACTIVA/PROPIETARIO se confirman en una sola
  transacción PostgreSQL.
- Repetir el mismo id, actor y nombre devuelve la representación existente sin
  duplicar filas; cambiar actor o contenido produce conflicto seguro.
- Las respuestas privadas usan `Cache-Control: no-store` y no exponen datos de
  otra cuenta ni detalles SQL.

V1 ya contenía tablas, claves foráneas, unicidad y triggers diferibles suficientes
para AU05. V1–V3 permanecieron intactas; no se creó una migración nueva.

## Pruebas ejecutadas

| Conjunto | Resultado real |
|---|---|
| `CrearGanaderiaTest` | 4 pruebas unitarias aprobadas: actor, NFC/trim, replay, nombre e id inválidos. |
| `CrearGanaderiaPostgresIntegrationTest` | 5 pruebas aprobadas con PostgreSQL 17.12: creación/asociación, replay/conflicto, anónimo, CSRF, datos/campos inválidos y rollback. |
| Regresión backend final con PostgreSQL | 63 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas. |

La prueba de atomicidad fuerza un fallo de membresía después del intento de alta
de ganadería y comprueba que la segunda ganadería no queda persistida. Los datos
sintéticos se eliminan al finalizar.
No se repitió la suite frontend porque este incremento no modificó React.

## Trazabilidad

| Elemento | Estado de este incremento |
|---|---|
| AU05 / PA05 | Creación backend, propietario inicial, atomicidad e idempotencia verificadas. |
| T02.1 | Modelo y persistencia de ganadería/membresía usados; faltan operaciones restantes de membresía. |
| T02.4 | Parcial: creación implementada; GET de listado y `/acceso` siguen pendientes. |
| CA03 | Parcial: propietario inicial real; faltan matriz completa de roles, dos ganaderías aisladas e integración con animales. |
| ADR-04 / ADR-07 / ADR-13 | PostgreSQL fuente de verdad, JWT/sesión revalidados y pertenencia explícita por membresía. |

## Pendientes explícitos

- No existe todavía formulario React para crear o seleccionar ganaderías.
- No están implementados `GET /ganaderias` ni `GET /ganaderias/{id}/acceso`.
- AU06–AU08, revocación, fixtures de editor/lector e invitaciones siguen pendientes.
- No se habilitan animales, offline, catálogo ni IA.
- No se realizó prueba manual de este endpoint en navegador.
