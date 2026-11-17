# Esquema inicial de Identidad — implementación y evidencia

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **persistencia inicial de
T02.1 implementada y verificada; AU01 se implementó después, mientras login,
sesiones y membresías operativas siguen pendientes**.

## Alcance

Esta iteración tradujo a PostgreSQL la parte de almacenamiento del contrato
AU01–AU08 y DP01 de SPEC-01. El registro AU01 se implementó posteriormente y se
documenta en [08-registro-usuarios.md](08-registro-usuarios.md). Aún no existen
login, renovación, logout, membresías operativas, UI, invitaciones ni animales.

Flyway fue seleccionado como herramienta de migraciones porque Spring Boot lo
integra al arranque y su módulo PostgreSQL es compatible con la versión 17. La
versión 11.7.2 queda gestionada por el parent Spring Boot 3.5.16, sin una versión
paralela en el proyecto. Las migraciones se aplican solo con el perfil `postgres`.

## Migraciones

| Versión | Archivo | Resultado |
|---|---|---|
| V1 | `V1__crear_esquema_identidad.sql` | Crea el esquema `identidad`, cinco tablas, claves, índices, restricciones y validación diferible del propietario. |
| V2 | `V2__limitar_duracion_sesion.sql` | Limita `sesion_dispositivo` a un máximo de 24 horas, separado de la autorización local offline de 168 horas. |

Las tablas son `usuario`, `ganaderia`, `membresia`, `sesion_dispositivo` y
`verificador_renovacion`. Sus identificadores son UUID. Las relaciones usan FKs
restrictivas para evitar borrados en cascada accidentales. El correo normalizado
y la pareja ganadería/usuario son únicos. Una ganadería debe cerrar la transacción
con su propietario como miembro PROPIETARIO activo; solo puede existir uno.

`usuario.password_hash` exige un formato Argon2id, pero la generación segura del
hash pertenece al caso de uso aún pendiente. `verificador_renovacion` conserva
solo una huella SHA-256 de 32 bytes, nunca el token opaco; registra estados
ACTIVO/CONSUMIDO/REVOCADO y permite un único verificador activo por sesión. La
rotación atómica, detección de reutilización y revocación de familia aún no están
implementadas.

Flyway valida checksums al arrancar. `clean` está deshabilitado y
`baseline-on-migrate` permanece desactivado. Una migración aplicada no se edita:
cualquier evolución se incorpora con una nueva versión. No se borró ni recreó la
base o el volumen existente.

## Pruebas y resultados reales

El contenedor `postgres:17.12` continuó healthy en `127.0.0.1:5433 -> 5432`.
La base existente no tenía tablas de usuario antes de V1. Los comandos usaron el
`.env` local ignorado, sin imprimir sus valores.

| Comprobación | Resultado real |
|---|---|
| `mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify` | BUILD SUCCESS: 12 pruebas, 0 fallos, 0 errores y 0 omitidas. |
| `mvn.cmd -B -ntp verify` sin propiedad PostgreSQL | BUILD SUCCESS: 3 pruebas base ejecutadas y 9 de entorno PostgreSQL omitidas expresamente. |
| Historial Flyway consultado con `psql` | V1 y V2 registradas una vez y con `success=true`. |
| Catálogo PostgreSQL | Cinco tablas de Identidad, 35 restricciones de tabla/relación y 16 índices registrados. |
| Repetición de arranque/pruebas | Flyway validó el esquema y determinó que no había migraciones pendientes. |
| Limpieza de fixtures | Las cinco tablas quedaron con cero filas; cada prueba revierte su transacción. |

Las siete pruebas nuevas verifican: versión del esquema y ausencia de columnas
para contraseña/token en claro; unicidad del correo y formato Argon2id; creación
atómica del propietario; unicidad de membresía; propietario activo único y FKs;
huella y único verificador activo; y duración online máxima de 24 horas. Las tres pruebas HTTP base y las dos de
conectividad PostgreSQL también pasaron.

El primer intento de la nueva suite no se declaró exitoso: la prueba inició Spring
sin contexto web y la configuración de seguridad requería `HttpSecurity`. Se
corrigió el arnés a puerto aleatorio, sin relajar seguridad ni cambiar producción,
y luego se ejecutó de nuevo con el resultado anterior.

## Pendientes y límites

- Completar los modelos y casos de uso de ganadería y membresía de T02.1.
- Continuar AU02–AU08 en T02.2–T02.8: el núcleo de login, sesiones, rotación y
  validación JWT se documenta en [09-nucleo-autenticacion-sesiones.md](09-nucleo-autenticacion-sesiones.md),
  pero faltan sus endpoints/cookies y los flujos de
  CSRF, respuestas HTTP 401/429, autorización y aislamiento entre ganaderías.
- Repetir la medición Argon2id de AU01 en el entorno de despliegue.
- Mantener fuera de este incremento invitaciones, alta real de editor/lector y animales.
- Ejecutar pruebas de concurrencia de rotación y autorización cuando existan los
  casos de uso; las restricciones de esquema no acreditan por sí solas PA01–PA08.

No se ejecutaron pruebas de carga porque todavía no existen los flujos que deben
medirse. No se modificó `main`, no se agregó staging y no se creó ningún commit.
