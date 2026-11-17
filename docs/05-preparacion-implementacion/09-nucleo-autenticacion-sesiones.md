# AU02/AU03 — Núcleo de autenticación y sesiones

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **núcleo backend parcial
implementado y verificado; login y renovación no están expuestos por HTTP**.

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

V1 y V2 no se modificaron. V3 crea únicamente `identidad.intento_login`, con
restricciones, clave primaria por dimensión/huella e índice de bloqueos. Flyway
la aplicó sobre la base existente sin `clean`, borrado de volumen ni recreación.

## Trazabilidad

| Elemento | Estado de este incremento |
|---|---|
| AU02 / T02.2 / PA02 | Núcleo y PostgreSQL parciales: credenciales, estados, límite y JWT probados. Falta HTTP. |
| AU03 / T02.3 / PA03 | Núcleo y PostgreSQL parciales: 10 min/24 h, rotación y reutilización probados. Faltan cookie/endpoints y concurrencia. |
| CA03 | Sigue incompleto: no hay login público, membresías ni aislamiento integral. |
| CA12 | Evidencia parcial de tiempos, revocación y errores; falta el flujo navegador/offline. |
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

La primera ejecución PostgreSQL detectó que `SET CONSTRAINTS` buscaba la FK
diferible en `public`; la transacción se revirtió sin cambio parcial. Se corrigió
la referencia a `identidad.fk_verificador_sucesor`, se repitió el caso afectado
y después la suite completa terminó correctamente.

## Pendientes explícitos

- No existen aún `POST /auth/login`, `POST /auth/renovar`, logout ni `/auth/me`.
- No se emite `__Host-GC-Refresh`; faltan la cookie, respuestas HTTP seguras,
  CSRF/origen en esas rutas y la revocación de una cookie previa al nuevo login.
- Las claves RSA de despliegue, su carga desde configuración externa y rotación
  operativa siguen pendientes; no se incluye ninguna clave real o de prueba.
- Falta integrar la validación JWT como filtro de Spring Security sobre rutas
  privadas y probar 401/429 HTTP sin filtrar existencia de cuentas.
- Faltan carreras de dos renovaciones, respuesta perdida tras commit, logout,
  navegador/React y coordinación entre pestañas.
- AU04–AU08, ganaderías, membresías, invitaciones y animales no se implementaron.

No se declara AU02 ni AU03 completada y no existe inicio de sesión utilizable por
usuarios finales en este estado.
