# AU07 — Revocación segura de membresías

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **incremento backend
implementado y verificado con PostgreSQL**.

## Alcance realizado

- `DELETE /api/v1/ganaderias/{ganaderiaId}/membresias/{usuarioId}` exige sesión
  vigente, JWT, CSRF y origen permitido.
- El actor procede exclusivamente del principal autenticado. Solo una membresía
  `PROPIETARIO` activa de la ganadería activa puede revocar una membresía
  `EDITOR` o `LECTOR` existente.
- La revocación conserva la fila, cambia su estado a `REVOCADA` y registra el
  instante UTC. Repetirla por el propietario es idempotente y responde 204.
- El propietario está protegido con 409. Un miembro inexistente o perteneciente
  a otro ámbito produce el mismo 404 seguro; un actor sin permiso recibe 403.
- La transacción bloquea en orden estable la sesión/cuenta, la ganadería, la
  membresía propietaria del actor y la membresía objetivo. Esto coordina logout
  y deja preparado el orden que deberá reutilizar el alta de animales.
- Tras confirmar la revocación, el mismo JWT del miembro deja de autorizar
  `/acceso`, porque la membresía se consulta de nuevo en PostgreSQL.
- `AutorizarAccesoGanaderia` centraliza la matriz reutilizable: propietario y
  editor pueden leer/escribir; lector solo puede leer. La selección React y los
  roles del JWT no intervienen en la decisión.

No se modificaron las migraciones V1–V3 ni se añadió una vía para crear
editores/lectores. Esos miembros existen únicamente como fixtures sintéticos de
integración; las invitaciones familiares pertenecen a otra SPEC.

## Pruebas ejecutadas

| Conjunto | Resultado real |
|---|---|
| Núcleo y autorización dirigidos | 7 aprobadas, 0 fallos y 0 errores: matriz de lectura/escritura, rechazo seguro, contexto AU06 y traducción idempotente de AU07. |
| Integración dirigida PostgreSQL | 16 aprobadas, 0 fallos y 0 errores en `CrearGanaderiaPostgresIntegrationTest`; 4 escenarios AU07 cubren propietario, editor, objetivo propietario/ajeno/inexistente, idempotencia, pérdida inmediata de acceso, autenticación, CSRF y origen. |
| Regresión backend final | 82 aprobadas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.12 `healthy` en `127.0.0.1:5433`; Flyway validó V1–V3 sin cambios. |

No se ejecutaron pruebas frontend porque este incremento no modifica React. No
se ejecutó una prueba manual del endpoint ni la carrera con alta de animal,
porque todavía no existe persistencia ni API de animales.

## Trazabilidad y pendientes

| Elemento | Estado |
|---|---|
| AU07 | Implementado en backend para revocar editor/lector existente; propietario protegido e idempotencia verificadas. |
| AU06 / PA06 | Autorizador reutilizable y revocación inmediata verificados; falta aplicar la matriz a los recursos de animales. |
| T02.5 | Parte autónoma completada. La carrera revocación/alta se verificará con T07 usando el mismo orden de bloqueos. |
| CA03 / CA06 / PA08 | Cobertura parcial ampliada; no se declaran completos hasta existir animales, cliente offline y pruebas conjuntas. |

La base de autorización permite comenzar T04–T06 (esquema, dominio y contratos
de registro individual). No permite exponer aún un alta real: T07 debe integrar
el autorizador y los bloqueos, y probar la carrera con revocación antes de T08.
