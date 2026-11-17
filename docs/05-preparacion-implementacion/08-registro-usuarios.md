# AU01 — Registro seguro de cuentas y evidencia

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **AU01 implementada y
verificada desde React hasta PostgreSQL; T02.1/T02.2/T02.6 y CA03 continúan en curso**.

## Alcance implementado

El backend expone `GET /api/v1/auth/csrf` y la operación de cambio
`POST /api/v1/auth/registro` con el perfil `postgres`. Esta última recibe `correo` y
`contrasena`, crea una cuenta ACTIVO y devuelve id, correo normalizado y estado.
No crea sesión, JWT, cookie de renovación, ganadería, membresía ni rol. Los campos
`rol`, `ganaderiaId`, `estado` y cualquier otro campo desconocido se rechazan.

La implementación conserva la dirección de dependencias:

- dominio: correo normalizado y contraseña de 15–128 caracteres Unicode;
- aplicación: caso de uso y puertos de repositorio, codificación e ID;
- adaptadores de salida: Argon2id y JDBC/PostgreSQL;
- adaptador de entrada: DTO, endpoint y errores seguros.

No se consulta primero si el correo existe. El `INSERT` y la restricción única
`uq_usuario_email_normalizado` resuelven también solicitudes concurrentes; el
adaptador traduce el conflicto a HTTP 409 sin devolver datos de la cuenta previa.
Un fallo de restricción no deja una fila parcial porque AU01 realiza una sola
escritura atómica.

## Contraseñas y dependencias

Se usa `Argon2PasswordEncoder` de Spring Security con Bouncy Castle
`bcprov-jdk18on` 1.86. Los parámetros fijados son:

| Parámetro | Valor |
|---|---:|
| Salt | 16 bytes, aleatorio por codificación |
| Hash | 32 bytes |
| Paralelismo | 1 |
| Memoria | 65 536 KiB |
| Iteraciones | 3 |

Dos codificaciones de la misma contraseña generan hashes diferentes y ambos se
pueden verificar. Las ejecuciones midieron 537–766 ms por primera codificación en JDK
22.0.1 y este equipo; no es un objetivo garantizado. Debe medirse otra vez con
JDK 21 y recursos equivalentes al despliegue. La contraseña solo se conserva el
tiempo necesario para validar/codificar y no se registra ni devuelve. La tabla
acepta únicamente formato Argon2id.

Spring Security documenta que su implementación Argon2 requiere Bouncy Castle y
recomienda ajustar funciones adaptativas al sistema real. La versión 1.86 fue
seleccionada de la publicación estable del proveedor vigente durante esta tarea.

## Validación y protección

- Correo: obligatorio, `strip`, minúsculas con locale neutro, una arroba con
  partes no vacías, sin espacios/controles y máximo 254 caracteres.
- Contraseña: obligatoria, 15–128 puntos de código Unicode, sin trim,
  normalización, truncamiento ni reglas de composición añadidas.
- JSON: campos desconocidos rechazados y cuerpo máximo de 16 KiB.
- Respuestas: `Cache-Control: no-store`, sin hashes, trazas ni datos de terceros.
- Estado/rol/ganadería no se aceptan y no existen valores implícitos fuera del
  estado ACTIVO fijado por el caso de uso y la base.
- CSRF de Spring Security permanece activo y se valida junto con el origen.
- GET CSRF devuelve el token no autenticador con `Cache-Control: no-store` y una
  cookie `__Host-GC-CSRF` Secure, HttpOnly, SameSite=Strict y Path=/.
- React mantiene el token solo en memoria durante la solicitud, lo envía en
  `X-CSRF-TOKEN` y renueva/reintenta una sola vez ante `CSRF_INVALIDO`.
- El formulario no usa localStorage ni IndexedDB, impide doble envío y no presenta
  el registro como login o sesión iniciada.
- Los DTO de entrada/salida y CSRF ocultan correo, contraseña y token en su
  representación textual; los procesadores de cuerpos HTTP permanecen en INFO
  aunque el entorno local eleve el logging general.

SPEC-01 no fijó un umbral propio para abusos de registro. Como decisión técnica
provisional se configuraron 10 intentos por dirección remota cada 15 minutos,
con `Retry-After`, máximo acotado de orígenes y sin confiar en
`X-Forwarded-For`. Es local a cada instancia y se pierde al reiniciar; no se
presenta como límite distribuido ni como el contador persistente de login AU02.
El umbral, proxy confiable y estrategia multiinstancia requieren aprobación antes
de producción.

## Pruebas reales

PostgreSQL `17.12` permaneció healthy en Docker, publicado solo en
`127.0.0.1:5433`. Flyway validó V1/V2 sin modificarlas ni aplicar migraciones
nuevas.

| Comando / comprobación | Resultado real |
|---|---|
| `npm.cmd run typecheck` | Correcto, sin errores TypeScript. |
| `npm.cmd test` | 11 pruebas Vitest, 0 fallos. |
| `npm.cmd run build` | Build Vite correcto; 19 módulos transformados. |
| `mvn.cmd -B -ntp verify` | BUILD SUCCESS: 31 pruebas descubiertas; 13 ejecutadas y 18 PostgreSQL omitidas expresamente. |
| `mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify` | BUILD SUCCESS: 31 pruebas, 0 fallos, 0 errores y 0 omitidas. |
| Registro válido | 201; cuenta ACTIVO, correo normalizado, sin sesión/membresía y sin hash en respuesta. |
| Validaciones | Correo, contraseña y campos rol/ganadería/estado inválidos producen 422 sin fila. |
| Duplicado/concurrencia | 409 genérico; dos solicitudes concurrentes dejan exactamente una cuenta. |
| Persistencia | Una violación real de restricción deja cero filas para esa cuenta. |
| Argon2id | Hash distinto del texto, salt aleatorio, parámetros comprobados y verificación positiva/negativa. |
| Seguridad HTTP | Origen o CSRF ausentes producen 403; intento 11 del mismo origen produce 429; login permanece 403. |
| Flujo CSRF real | GET entrega JSON/cookie segura; POST por HTTP real envía cookie/cabecera y confirma una cuenta en PostgreSQL. El dato sintético se elimina al finalizar. |
| Cliente React | Validaciones, token CSRF, reintento único, éxito sin sesión, duplicado, 429, red y doble envío cubiertos en happy-dom. |
| Logging seguro | Prueba unitaria confirma que la representación de los DTO no contiene correo, contraseña ni token CSRF. |
| Revisión manual en Chrome | El registro desde React mostró «Cuenta creada correctamente» y una consulta posterior confirmó la cuenta sintética en PostgreSQL. |
| Revisión visual manual | La captura móvil de 320 px mostró navegación ordenada, título ajustado y formulario más cercano; la captura de escritorio conservó su distribución. |
| Limpieza de la cuenta sintética | Una consulta final exacta y de solo lectura devolvió 0 filas para el correo de revisión acordado. |

El primer `verify` de compilación falló porque Spring Security 6.5 no ofrece un
método de configuración CSRF usado inicialmente; se migró al manejador global
soportado sin desactivar CSRF. La primera integración PostgreSQL tampoco se
declaró exitosa: un dato llamado «demasiado-corta» tenía exactamente los 15
caracteres mínimos y el sistema lo aceptó correctamente. Se corrigió el caso de
prueba y luego pasó la suite completa.

## Límites pendientes

- Login, JWT, renovación, logout y sesiones pertenecen a AU02–AU04.
- Creación de ganadería, membresías, roles y revocación pertenecen a AU05–AU07.
- El límite de registro es básico y local; falta la decisión para despliegue.
- No se ejecutó un E2E automatizado con navegador real ni Playwright. Tampoco se
  ejecutó una matriz visual en 360, 390, 768 y 1440 px; la evidencia manual se
  limita a Chrome con captura móvil de 320 px y captura de escritorio. Playwright
  y los navegadores objetivo siguen pendientes de fijar según DP07.
- No se ejecutaron pruebas de carga porque todavía no existe el flujo completo
  de autenticación ni las consultas de animales.
- No se modificaron V1/V2. El incremento posterior de núcleo AU02/AU03 agregó V3,
  documentada por separado en [09-nucleo-autenticacion-sesiones.md](09-nucleo-autenticacion-sesiones.md).

No se agregó staging, commit ni push durante esta iteración.
