# SPEC-01 — Tareas incrementales propuestas

**Estado: Aprobada — Implementación en curso.** T01 documental fue completada por autorización del responsable el 16 de noviembre de 2026. El 17 de noviembre se implementaron AU01 y el backend AU02–AU04; T02 sigue abierta por faltar React, ganadería, membresías y autorización integral. No hay animales implementados. Depende de [spec.md](spec.md) y [plan.md](plan.md).

## 0. Aprobación y dependencias

| ID | Tarea | Depende de | Entregable / verificación futura |
|---|---|---|---|
| T01 | COMPLETADA DOCUMENTALMENTE: aprobación de DP01–DP08, SPEC-01/CA01–CA18 y plan/tareas con rotación obligatoria. | Autorización expresa del responsable, 16 de noviembre de 2026 | Aprobación registrada; no equivale a código, pruebas ejecutadas ni habilitación de datos reales. |
| T02 | Implementar y verificar prerrequisito mínimo DP01 según subtareas T02.1–T02.8; no asumir acceso existente. | T01, T03 | Registro/login/sesión, ganadería/propietario inicial, validación por petición y revocación. Solo miembros de test para editor/lector; no alta familiar real. PA01–PA08 aprobadas antes de animales reales. |
| T03 | Fijar versiones de Java/Spring, Node/React, PostgreSQL y herramientas previstas por DP07, coste Argon2id y navegadores objetivo y comandos reproducibles. | T01 | Decisiones registradas, archivos de dependencias en tarea de implementación posterior; sin secretos. |


**Bloqueo de habilitación:** T02 debe estar cumplida antes de cualquier registro,
consulta o preparación offline con datos reales. Las tareas de dominio y UI de animales
pueden trabajarse con dobles/datos sintéticos sin habilitar ese flujo. La falta
de contrato o implementación del acceso familiar no se resuelve desactivando
seguridad. Invitaciones, incorporación real de editores/lectores y registro masivo se gestionarán mediante otras SPEC. Las pruebas de roles familiares usan autenticación real y fixtures aislados, nunca un endpoint de test en producción.
La caducidad offline nunca es una tarea de limpieza de registros pendientes.

### T02 — Desglose obligatorio del prerrequisito de acceso

Se conservan 23 tareas de nivel superior; T02 se desglosa en ocho subtareas.
Existe evidencia de persistencia, registro y autenticación backend para T02.1–T02.3;
ninguna subtarea integral está completa. T01 registra la aprobación documental; T02.8 completa
la base de acceso, sin habilitar animales reales hasta pasar las pruebas conjuntas.

| ID | Trabajo futuro | Dependencias | Verificación / criterios |
|---|---|---|---|
| T02.1 | **EN CURSO:** modelar Usuario, SesionDispositivo, Ganaderia y Membresia; migraciones, unicidad y hashes, instantes UTC/fecha de negocio Lima. V1/V2 y V3, Usuario, sesión/verificadores y sus adaptadores JDBC están verificados; faltan casos de uso de ganadería y membresía. | T01, T03 | [Esquema](../../docs/05-preparacion-implementacion/07-esquema-identidad.md), [registro](../../docs/05-preparacion-implementacion/08-registro-usuarios.md) y [núcleo de sesión](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md); PA05/PA08 y CA03 siguen pendientes de cumplimiento integral. |
| T02.2 | **BACKEND COMPLETADO; CLIENTE PENDIENTE:** AU01 real funciona de React a PostgreSQL; AU02 verifica Argon2id con error genérico, límite persistente y login HTTP 200/401/429 sin asignar roles. Falta formulario React de login. | T02.1 | PA01 ejecutada; PA02 verificada en backend/PostgreSQL. CA03/CA12 siguen pendientes integralmente. [Evidencia AU01](../../docs/05-preparacion-implementacion/08-registro-usuarios.md) y [AU02](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md). |
| T02.3 | **BACKEND COMPLETADO; CLIENTE/DESPLIEGUE PENDIENTES:** sesión 24 h, JWT 10 min, filtro con estado PostgreSQL, cookie segura, rotación, concurrencia, reutilización, respuesta perdida y logout verificados. Claves externas son obligatorias; falta su aprovisionamiento/rotación operativa y el flujo React. | T02.2 | PA02/PA03 backend ejecutadas; PA04/PA07 parciales por faltar cliente/offline/navegador HTTPS. CA03/CA12/CA14 continúan pendientes integralmente. [Evidencia](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md). |
| T02.4 | Implementar alta online de ganadería+propietario atómica, listado propio y GET /acceso; validar pertenencia/rol en toda API protegida. | T02.1, T02.3 | PA05/PA06; CA03. |
| T02.5 | Implementar revocación de editor/lector existente solo por propietario, protegiendo propietario y orden de bloqueos con registro de animal. | T02.4 | PA06/PA08; CA03/CA06; cierre de pruebas de carrera con T07 antes de habilitar altas. |
| T02.6 | **EN CURSO:** pantalla React de registro AU01 implementada; faltan login/logout/selección-creación de ganadería, renovación serializada entre pestañas/recarga y cierre remoto pendiente. No reintentar refresh incierto, exigir login sin perder cola; sin invitaciones. | T02.2 para registro; T02.3, T02.4, T02.5 para el resto | PA01 parcial en cliente; PA03/PA04/PA07, E2E del propietario inicial, CA03/CA12/CA14 y pruebas conjuntas de cola con T15/T18 siguen pendientes. |
| T02.7 | Ejecutar la parte de PA01–PA08 correspondiente al acceso sin animales, con API/PostgreSQL/navegador y fixtures solo test; reservar carreras con altas y conservación de cola para T17/T18. | T02.2–T02.6 | Permisos de propietario/editor/lector, IDOR, rotación/reutilización de refresh, dos renovaciones concurrentes, rollback, respuesta perdida, rechazo de JWT previo por estado, CSRF y transacciones; CA03/CA06/CA12/CA14. Sin afirmar que invitaciones o altas reales de familiares existen. |
| T02.8 | Comprobar gate de acceso antes de habilitar cualquier animal real: login/sesión, propietario inicial y autorización probados. | T02.7 | Gate de base de acceso cumplido permite desarrollar T04–T18; no habilita producción. Mantener animales reales cerrados hasta completar T07/T15/T17/T18 y todas las PA conjuntas. |

T02 cubre su base de acceso antes de T04/T06/T08; las pruebas conjuntas de carreras
con animales y conservación offline se completan luego en T17/T18. No se crea
un ciclo de dependencias: habilitar acceso real es la condición final, no un
requisito para ejecutar pruebas de integración en entornos aislados.

## 1. Base de datos y núcleo backend

| ID | Tarea | Depende de | Verificación / CA |
|---|---|---|---|
| T04 | Diseñar migración Animal/OperacionConfirmada, FK diferible de recibo a Animal, restricción sexo NOT NULL y enum MACHO/HEMBRA e índices; integrar FK/membresía existente. | T02, T03 | Migración y rollback/estrategia de evolución verificables con PostgreSQL controlado. CA01/CA05/CA09/CA11. |
| T05 | Implementar entidades/objetos de valor y validaciones de sexo obligatorio MACHO/HEMBRA, opcionales, categoría/fecha y estados. | T01, T03 | PU01/PU02, sin BD/framework real en núcleo. CA01/CA02/CA03/CA08. |
| T06 | Definir contratos de repositorio, autorización, reloj y unidad de trabajo; canonicalización versionada y huella del servidor. | T02, T05 | PU04, sin tipos JPA/HTTP en contratos del núcleo. CA05/CA09/CA10. |
| T07 | Implementar RegistrarAnimal y adaptadores transaccionales; bloqueos de permisos, reserva única, insert y recibo en un commit. | T04, T06 | PI02: dos solicitudes, rollback, colisión y revocación concurrente ordenada. CA05/CA06/CA09/CA11. |
| T08 | Exponer POST único con DTO/errores/recibo; integrar autenticación real, límites y validación sin filtrar datos ajenos. | T02, T07 | PI01/PI03; CA02/CA03/CA06/CA09/CA10/CA12/CA16. |
| T09 | Implementar GET listado/ficha por ganadería, cursor y advertencia arete no bloqueante. | T04, T06 | PI03 y revisión de índices. CA03/CA13/CA15. |

## 2. Cliente React y almacenamiento offline

| ID | Tarea | Depende de | Verificación / CA |
|---|---|---|---|
| T10 | Construir formulario y lista/ficha responsivos en español; contexto de ganadería, alta mínima con sexo obligatorio, sin exigir nombre/arete/raza/edad/peso/foto y permisos visibles. | T01, T03 | EE01, teclado, 360 px/escritorio; CA01/CA02/CA03/CA08/CA15. |
| T11 | Implementar adaptador UUIDv4 y almacenamiento IndexedDB por cuenta/ganadería, transacción de provisional+cola y fallos de cuota. | T03, T06 | PU03/PI04; CA04/CA07/CA11/CA16. |
| T12 | Preparar recursos PWA estáticos y opt-in offline; páginas/fichas descargadas, contexto recordado y autorización de 168 h sin TTL ni borrado de pendientes. | T09, T10, T11 | EE02/EE03; sin cachear respuestas privadas en Service Worker. CA04/CA14/CA15/CA16. |
| T13 | Implementar cliente HTTP y orquestador de cola con cuerpo inmutable, lease entre pestañas, backoff/Retry-After y recuperación tras recarga. | T08, T11 | PU03/PU04/PI04; CA04/CA05/CA07/CA09/CA10/CA11/CA12/CA16. |
| T14 | Mostrar estados, errores y conflictos; confirmar localmente por IDs, no degradar versiones remotas y no reenviar como otra alta. | T10, T13 | EE02/EE04; CA05/CA07/CA09/CA10/CA11/CA15. |
| T15 | Implementar bloqueo por vencimiento/revocación, limpieza solo de copias sincronizadas al salir y conservación de pendientes por cuenta; descarte separado explícito. | T02, T12, T13, T14 | EE03; probar pérdida de token/contexto, límites de 168 h, cola intacta, recuperación al reautenticar, limpieza fallida y bloqueo ante otra cuenta. CA06/CA12/CA14. |

## 3. Pruebas e integración del flujo

Las pruebas se escriben junto al incremento correspondiente; esta fase integra
las suites, no posterga toda la verificación hasta terminar la aplicación.

| ID | Tarea | Depende de | Resultado requerido |
|---|---|---|---|
| T16 | Completar PU01–PU04 y comprobar todas las fronteras de validación y estados. | T05, T06, T11, T13 | Casos válidos, inválidos y límite; CA01/CA02/CA03/CA04/CA05/CA07/CA08/CA09/CA10/CA12/CA13/CA15/CA16. |
| T17 | Ejecutar PI01–PI05 y repetir PA01–PA08 contra API/PostgreSQL y navegador controlados. | T07, T08, T09, T12, T13, T15 | Autenticación real y pertenencia/roles como prerrequisitos, IDOR, sexo omitido/null/inválido sin escritura, concurrencia, rollback, respuesta perdida, cuota y ausencia de dependencia Redis/IA/notificaciones; CA02–CA17 aplicables. |
| T18 | Ejecutar EE01–EE04 y flujos PA con registro/login/ganadería del propietario real, editor/lector por fixtures y dos ganaderías sintéticas. | T10–T17 | Sexo obligatorio online/offline; ventana vencida/sesión perdida sin borrar cola; reautenticación de la misma cuenta y otra cuenta sin acceso; reapertura, doble clic/pestaña, 401/403/409/422/429/5xx, errores de protocolo y almacenamiento; CA01–CA16 aplicables. |
| T19 | Documentar entorno/dataset PC01 y ejecutar k6 cuando exista flujo funcional; analizar planes de consulta. | T03, T09, T17, T18 | CA18 y AC01: 100 concurrentes, p95 ≤ 2 s y errores nominales 0; registrar fallos sin ocultarlos. |
| T20 | Integrar suites disponibles a CI y registrar comandos/evidencias reales sin secretos. | T16, T17, T18 | Criterios 5/6/12/14; pipeline ejecutado solo cuando exista, sin prometer despliegue. |

## 4. Cierre del incremento

- [ ] T21: relacionar cada CA con sus resultados PU/PI/EE/PC/PA; documentar entorno,
  commit evaluado, fecha, comandos, datos sintéticos, errores y limitaciones.
- [ ] T22: actualizar documentación global y estado de SPEC solo tras revisar
  resultados; mantener explícito lo excluido y las fuentes C4 aún pendientes.
- [ ] T23: revisión final y demostración: registro/login y creación de ganadería/propietario, alta individual; editor/lector por fixtures con sus permisos,
  guardar sin red, recuperar respuesta perdida, reconectar sin duplicados y
  rechazar una membresía revocada. No afirmar que RF02/RF20/RF21 completos están cubiertos.

T21 depende de T16–T20; T22 y T23 dependen de T21. Registrar cualquier ajuste de
SPEC antes de modificar comportamiento. No hay autorización para git add,
commit, push, cambios en main ni implementación durante esta tarea documental.
