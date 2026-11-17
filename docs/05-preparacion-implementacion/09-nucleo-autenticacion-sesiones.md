# AU02/AU03/AU04 — Autenticación y sesiones backend

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **backend HTTP implementado
y verificado; la interfaz React de login y AU05–AU08 siguen pendientes**.

## Alcance realizado

- Verificación de correo y contraseña contra el hash Argon2id de AU01.
- Comparación Argon2id con hash sintético cuando la cuenta no existe, para no
  omitir deliberadamente el coste; cuenta inexistente, inactiva y contraseña
  incorrecta producen el mismo error de aplicación.
- Contadores persistentes de fallos por huellas SHA-256 de correo normalizado y
  origen. Diez fallos dentro de quince minutos activan bloqueo temporal.
- Creación transaccional de `sesion_dispositivo` por 24 horas y un único
  verificador ACTIVO, sin guardar la credencial de renovación en claro.
- Credenciales opacas aleatorias de 256 bits y huellas SHA-256 de 32 bytes.
- JWT RS256 con `sub`, `sid`, `jti`, `iat`, `exp`, emisor y audiencia; duración
  máxima de diez minutos y validación adicional de sesión/cuenta en PostgreSQL.
- Rotación transaccional: el verificador actual pasa a CONSUMIDO y se crea un
  sucesor ACTIVO. Reutilizar el anterior revoca la sesión y toda la familia.
- DTO y resultados internos protegen credenciales en sus representaciones de
  diagnóstico. Las pruebas RSA generan claves efímeras y no versionan claves.
- `POST /api/v1/auth/login` devuelve JWT en JSON y renovación únicamente en
  `__Host-GC-Refresh`, con Secure, HttpOnly, SameSite=Strict, Path=/ y sin Domain.
- `POST /api/v1/auth/renovar` rota la cookie; dos solicitudes concurrentes no
  conservan dos sucesores y reutilizar el valor anterior revoca toda la familia.
- `POST /api/v1/auth/logout` es idempotente, reconoce tokens activos o históricos,
  revoca sesión/verificadores y elimina la cookie con los mismos atributos.
- `GET /api/v1/auth/me` exige Bearer válido y vuelve a comprobar en PostgreSQL
  que la sesión no esté vencida/revocada y que la cuenta permanezca activa.
- Login, renovación y logout exigen CSRF y Origin/Referer del origen configurado.
  Respuestas de sesión y errores llevan `Cache-Control: no-store`.
- Las claves RSA de ejecución se cargan obligatoriamente desde archivos externos
  X.509/PKCS#8; no hay clave privada ni valor predeterminado en el repositorio.

V1 y V2 no se modificaron. V3 crea únicamente `identidad.intento_login`, con
restricciones, clave primaria por dimensión/huella e índice de bloqueos. Flyway
la aplicó sobre la base existente sin `clean`, borrado de volumen ni recreación.

## Trazabilidad

| Elemento | Estado de este incremento |
|---|---|
| AU02 / T02.2 / PA02 | Backend HTTP y PostgreSQL: login, errores uniformes, límite, JWT y filtro probados. Falta React. |
| AU03 / T02.3 / PA03 | Backend HTTP: 10 min/24 h, cookie, rotación, concurrencia, reutilización y respuesta perdida segura probadas. |
| AU04 / T02.3 / PA04 | Logout online y rechazo del JWT anterior probados; cierre offline/React y membresías pendientes. |
| PA07 | CSRF/origen, cookie y `no-store` verificados en backend; navegador real queda pendiente. |
| CA03 | Parcial: login backend existe; faltan ganadería, membresías y aislamiento integral. |
| CA12 | Backend de tiempos, revocación y errores verificado; falta el flujo React/offline. |
| ADR-04 / ADR-07 | PostgreSQL como fuente y Spring Security JOSE/RS256, sin claves en repositorio. |

## Evidencia ejecutada

PostgreSQL `17.12` permaneció healthy en Docker y accesible exclusivamente por
`127.0.0.1:5433` desde el host.

| Comando / escenario | Resultado real |
|---|---|
| `mvn.cmd -B -ntp test` | BUILD SUCCESS: 45 pruebas descubiertas; 21 ejecutadas y 24 PostgreSQL omitidas expresamente. |
| `mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify` | BUILD SUCCESS final: 45 pruebas, 0 fallos, 0 errores y 0 omitidas. |
| `npm.cmd test` | Regresión frontend: 11 pruebas Vitest, 0 fallos. |
| `npm.cmd run typecheck` / `npm.cmd run build` | TypeScript correcto y build Vite exitoso con 19 módulos. |
| Credenciales | Válida crea sesión; incorrecta, inexistente e inactiva devuelven el mismo error interno y no crean sesión. |
| Límite | El décimo fallo bloquea temporalmente correo y origen; solo se almacenan huellas. |
| JWT | RS256 válido; alteración, expiración, emisor/audiencia incorrectos y sesión vencida/revocada se rechazan. |
| Renovación | Una rotación deja un CONSUMIDO y un ACTIVO; reutilizar el consumido revoca sesión/familia y el JWT posterior falla por estado. |
| Atomicidad | Una huella inválida al crear el verificador revierte también la fila de sesión. |
| Regresión | Registro AU01, CSRF, esquema, JDBC y rutas no autorizadas continúan pasando. |

En el incremento HTTP se ejecutaron primero las pruebas relacionadas. La clase
`AutenticacionHttpPostgresIntegrationTest` terminó con 9 pruebas, 0 fallos,
0 errores y 0 omitidas; `DtosHttpSegurosTest` añadió 1 comprobación aprobada.
La regresión completa final se registró al cerrar este incremento:

| Cierre del incremento HTTP | Resultado real |
|---|---|
| `mvn.cmd -q -o "-Dganadoconecta.postgres.integration-test=true" verify` | 54 pruebas backend/PostgreSQL, 0 fallos, 0 errores y 0 omitidas. |
| Frontend | No se repitió: no hubo cambios en React; sus 11 pruebas aprobadas pertenecen al incremento anterior. |

La primera ejecución PostgreSQL detectó que `SET CONSTRAINTS` buscaba la FK
diferible en `public`; la transacción se revirtió sin cambio parcial. Se corrigió
la referencia a `identidad.fk_verificador_sucesor`, se repitió el caso afectado
y después la suite completa terminó correctamente.

La primera carrera HTTP de dos renovaciones detectó otro caso real: la segunda
petición podía capturar su instante antes de esperar el bloqueo y revocar el
sucesor con una fecha anterior a `creado_en`. PostgreSQL rechazó el cambio sin
filtrar credenciales. La actualización usa como mínimo `creado_en`; se repitió
primero el método fallido y después la clase HTTP completa, ambos correctamente.
La primera regresión completa también encontró dos expectativas antiguas `403`
para acceso anónimo; el contrato vigente exige `401`. Se corrigieron solo esos
tests, ambos pasaron de forma dirigida y la regresión final de 54 pruebas quedó limpia.

## Pendientes explícitos

- Falta el formulario React de login/logout, JWT solo en memoria, renovación
  coordinada entre pestañas y tratamiento visual de sesión vencida.
- Falta aprovisionar y rotar claves RSA en un entorno de despliegue real; la carga
  externa está implementada y las pruebas solo generan claves efímeras en `target/`.
- Falta validar cookie Secure y el flujo completo en navegador HTTPS real.
- El cierre offline, la marca no secreta de cierre remoto pendiente y conservación
  de la cola corresponden al cliente offline aún no implementado.
- AU05–AU08, ganaderías, membresías, invitaciones y animales no se implementaron.

No se declara completo el acceso integral de SPEC-01: el backend AU02–AU04 está
disponible, pero no existe inicio de sesión utilizable desde React.
