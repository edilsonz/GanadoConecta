# SPEC-01 — Tareas incrementales propuestas

**Estado: Aprobada — Implementación en curso.** T01 documental fue completada por autorización del responsable el 16 de noviembre de 2026. El 17 de noviembre se implementaron AU01, AU02–AU04 con cliente React, creación/listado/selección de ganaderías, contexto online `/acceso` y revocación AU07. El 18 de noviembre se completaron T04–T10: PostgreSQL/REST para alta, listado y ficha, más su cliente React online. El 19 se implementaron la persistencia T11 y el primer contexto local T12/T15; PWA, integración del formulario y sincronización siguen pendientes. Depende de [spec.md](spec.md) y [plan.md](plan.md).

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
Existe evidencia de persistencia, registro y autenticación backend/React para T02.1–T02.3;
ninguna subtarea integral está completa. T01 registra la aprobación documental; T02.8 completa
la base de acceso, sin habilitar animales reales hasta pasar las pruebas conjuntas.

| ID | Trabajo futuro | Dependencias | Verificación / criterios |
|---|---|---|---|
| T02.1 | **EN CURSO:** modelar Usuario, SesionDispositivo, Ganaderia y Membresia; migraciones, unicidad y hashes, instantes UTC/fecha de negocio Lima. V1–V3, Usuario, sesión/verificadores, creación atómica de ganadería/propietario y revocación sin borrado están verificados. | T01, T03 | [Esquema](../../docs/05-preparacion-implementacion/07-esquema-identidad.md), [registro](../../docs/05-preparacion-implementacion/08-registro-usuarios.md), [sesión](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [AU05](../../docs/05-preparacion-implementacion/11-creacion-ganaderia.md) y [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md); PA08 y CA03 siguen pendientes de cumplimiento integral. |
| T02.2 | **BACKEND Y CLIENTE AUTOMATIZADO:** AU01 real funciona de React a PostgreSQL; AU02 verifica Argon2id con error genérico, límite persistente y login HTTP 200/401/429 sin asignar roles. React cubre formulario, estados y JWT en memoria; falta E2E HTTPS real. | T02.1 | PA01 ejecutada; PA02 verificada en backend/PostgreSQL y cliente aislado. CA03/CA12 siguen pendientes integralmente. [AU01](../../docs/05-preparacion-implementacion/08-registro-usuarios.md), [backend AU02](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md) y [React](../../docs/05-preparacion-implementacion/10-login-react.md). |
| T02.3 | **BACKEND Y CLIENTE AUTOMATIZADO; DESPLIEGUE PENDIENTE:** sesión 24 h, JWT 10 min, filtro con estado PostgreSQL, cookie segura, rotación, concurrencia, reutilización, respuesta perdida y logout verificados. React restaura/renueva/cierra sin persistir tokens. Faltan aprovisionamiento/rotación operativa de claves, certificado externo y navegador HTTPS/multipestaña real. | T02.2 | PA02/PA03 backend y cliente ejecutadas; PA04/PA07 parciales por faltar cola offline, membresías y navegador HTTPS. CA03/CA12/CA14 continúan pendientes integralmente. [Backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md). |
| T02.4 | **EN CURSO:** alta online de ganadería+propietario, listado propio, GET `/acceso` y recursos backend de animales implementados con revalidación desde PostgreSQL. La matriz leer/escribir se reutiliza en T07–T09. | T02.1, T02.3 | PA05 y contexto/aislamiento parcial de PA06 ejecutados; CA03 sigue parcial. [Evidencia AU05](../../docs/05-preparacion-implementacion/11-creacion-ganaderia.md), [AU06](../../docs/05-preparacion-implementacion/13-acceso-ganaderia.md), [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md), [T04–T07](../../docs/05-preparacion-implementacion/15-nucleo-registro-animales.md), [T08](../../docs/05-preparacion-implementacion/16-endpoint-registro-animales.md) y [T09](../../docs/05-preparacion-implementacion/17-consulta-animales.md). |
| T02.5 | **PARTE AUTÓNOMA COMPLETADA:** revocación transaccional de editor/lector existente solo por propietario, propietario protegido, repetición idempotente y pérdida inmediata de acceso. T07 reutiliza el mismo orden de bloqueos y verificó ambos órdenes de la carrera revocación/alta. | T02.4 | PA06/PA08 y CA03/CA06 parciales ejecutadas; no equivale a endpoint de alta ni acceso familiar completo. [Evidencia AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md) y [T07](../../docs/05-preparacion-implementacion/15-nucleo-registro-animales.md). |
| T02.6 | **EN CURSO:** React integra registro, login/logout, creación/listado/selección de ganadería y `/acceso` al abrir «Mis animales»; JWT, rol verificado y selección solo viven en memoria. Restauración/renovación y marca no secreta de cierre remoto están cubiertas unitariamente. Faltan cola offline y validación multipestaña/HTTPS real. No reintentar refresh incierto, exigir login sin perder cola; sin invitaciones. | T02.2 para registro/login; T02.3, T02.4, T02.5 para el resto | PA01, cliente de PA02/PA03, UI de PA05 y contexto online de PA06 ejecutados parcialmente. E2E del propietario inicial, CA03/CA12/CA14 y pruebas conjuntas de cola con T15/T18 siguen pendientes. [Acceso](../../docs/05-preparacion-implementacion/13-acceso-ganaderia.md) y [React T10](../../docs/05-preparacion-implementacion/18-animales-react.md). |
| T02.7 | Ejecutar la parte de PA01–PA08 correspondiente al acceso sin animales, con API/PostgreSQL/navegador y fixtures solo test; reservar carreras con altas y conservación de cola para T17/T18. | T02.2–T02.6 | Permisos de propietario/editor/lector, IDOR, rotación/reutilización de refresh, dos renovaciones concurrentes, rollback, respuesta perdida, rechazo de JWT previo por estado, CSRF y transacciones; CA03/CA06/CA12/CA14. Sin afirmar que invitaciones o altas reales de familiares existen. |
| T02.8 | Comprobar gate de acceso antes de habilitar cualquier animal real: login/sesión, propietario inicial y autorización probados. | T02.7 | Gate de base de acceso cumplido permite desarrollar T04–T18; no habilita producción. Mantener animales reales cerrados hasta completar T07/T15/T17/T18 y todas las PA conjuntas. |

T02 cubre su base de acceso antes de T04/T06/T08; las pruebas conjuntas de carreras
con animales y conservación offline se completan luego en T17/T18. No se crea
un ciclo de dependencias: habilitar acceso real es la condición final, no un
requisito para ejecutar pruebas de integración en entornos aislados.

## 1. Base de datos y núcleo backend

| ID | Tarea | Depende de | Verificación / CA |
|---|---|---|---|
| T04 | **COMPLETADA:** Flyway V4 crea Animal/OperacionConfirmada, FK diferible, sexo NOT NULL MACHO/HEMBRA, integridad e índices; V1–V3 permanecen intactas. | T02, T03 | Migración aplicada y validada con PostgreSQL 17.12. CA01/CA05/CA09/CA11 parciales. [Evidencia](../../docs/05-preparacion-implementacion/15-nucleo-registro-animales.md). |
| T05 | **COMPLETADA:** entidades/objetos de valor y validaciones de sexo obligatorio, opcionales, categoría, fecha y estados. | T01, T03 | Pruebas unitarias ejecutadas sin BD/framework en el dominio. CA01/CA02/CA03/CA08 parciales. |
| T06 | **COMPLETADA PARA EL ALTA:** contratos de repositorio, autorización transaccional, reloj y unidad de trabajo; canonicalización v1 y SHA-256 del servidor. | T02, T05 | Contratos del núcleo sin tipos HTTP/JDBC; pruebas dirigidas ejecutadas. CA05/CA09/CA10 parciales. |
| T07 | **COMPLETADA EN NÚCLEO BACKEND:** RegistrarAnimal y adaptadores transaccionales revalidan sesión/membresía, reservan operación y confirman animal+recibo en un commit. | T04, T06 | Reintentos secuenciales/concurrentes, rollback, colisión, roles, aislamiento y carrera revocación/alta verificados. CA05/CA06/CA09/CA11 parciales. |
| T08 | **COMPLETADA EN BACKEND:** POST único con DTO estricto, recibo 201/200 y errores seguros; autenticación, CSRF, origen, 16 KiB, roles y revalidación transaccional verificados sin filtrar datos ajenos. | T02, T07 | PI01/PI03 parciales; CA02/CA03/CA06/CA09/CA10/CA12/CA16. [Evidencia](../../docs/05-preparacion-implementacion/16-endpoint-registro-animales.md). |
| T09 | **COMPLETADA EN BACKEND:** GET listado/ficha por ganadería, cursor opaco, orden estable, tres roles lectores y aislamiento; V5 corrige el orden del índice sin modificar V1–V4. La advertencia no bloqueante permanece en el recibo T08 conforme al contrato. | T04, T06 | PI03 y revisión de índices ejecutadas parcialmente. CA03/CA13/CA15. [Evidencia](../../docs/05-preparacion-implementacion/17-consulta-animales.md). |

## 2. Cliente React y almacenamiento offline

| ID | Tarea | Depende de | Verificación / CA |
|---|---|---|---|
| T10 | **COMPLETADA PARA EL CLIENTE ONLINE:** formulario y lista/ficha responsivos en español; `/acceso`, sexo obligatorio, opcionales aprobados, permisos visibles y replay en memoria del mismo sobre. No incluye IndexedDB/offline ni acredita aún navegador/teclado/resoluciones reales. | T01, T03 | Componentes/cliente: 14 pruebas dirigidas; regresión frontend 51/51, TypeScript y build correctos. EE01 y CA01/CA02/CA03/CA08/CA15 parciales. [Evidencia](../../docs/05-preparacion-implementacion/18-animales-react.md). |
| T11 | **PARCIAL:** adaptador IndexedDB nativo para operaciones pendientes implementado con sobre/IDs inmutables, aislamiento por cuenta/ganadería, estados, errores seguros, reapertura y fallos de escritura. No se conecta aún al formulario; faltan provisional+cola atómicos y navegador real. | T03, T06 | PU03/PI04 parciales; CA04/CA07/CA11/CA14/CA16. [Evidencia](../../docs/05-preparacion-implementacion/20-indexeddb-operaciones-pendientes.md). |
| T12 | **PARCIAL EN INFRAESTRUCTURA:** contexto recordado por cuenta/ganadería con rol, validadoEn, máximo 168 h, detección conservadora de retroceso de reloj y estados VIGENTE/REQUIERE_REVALIDACION/BLOQUEADO. Faltan opt-in, UI, páginas/fichas y PWA. | T09, T10, T11 | PU02/PI04/EE03 parciales; sin Service Worker ni caché privada. CA04/CA06/CA12/CA14/CA16. [Evidencia](../../docs/05-preparacion-implementacion/21-autorizacion-offline-local.md). |
| T13 | Implementar cliente HTTP y orquestador de cola con cuerpo inmutable, lease entre pestañas, backoff/Retry-After y recuperación tras recarga. | T08, T11 | PU03/PU04/PI04; CA04/CA05/CA07/CA09/CA10/CA11/CA12/CA16. |
| T14 | Mostrar estados, errores y conflictos; confirmar localmente por IDs, no degradar versiones remotas y no reenviar como otra alta. | T10, T13 | EE02/EE04; CA05/CA07/CA09/CA10/CA11/CA15. |
| T15 | **PARCIAL EN SERVICIO:** evaluación bloquea vencimiento, reloj inconsistente, sesión ausente y rechazo de `/acceso`; diferencia red/servidor, bloquea contextos por cuenta y conserva pendientes. Falta conectarlo a logout/UI, retirar copias, descarte y sincronización. | T02, T12, T13, T14 | EE03 parcial con dobles; cola/IDs intactos tras vencer. CA06/CA12/CA14. [Evidencia](../../docs/05-preparacion-implementacion/21-autorizacion-offline-local.md). |

## 2.1. Cambio controlado de identificación externa

| ID | Tarea | Depende de | Verificación / CA |
|---|---|---|---|
| T19-ID | **IMPLEMENTADA Y VERIFICADA:** extender dominio, API, PostgreSQL y React con colores+ubicación de lana/cinta como par opcional; conservar aretes, UUID e idempotencia v1 mediante schema v2 y V6 aditiva. Sufijo `-ID` evita colisionar con la tarea superior T19 de carga. | T08–T10 | Solo arete, solo textil, ambos, ninguno; ubicación/colores inválidos, permisos, replay v1/v2, consulta y compatibilidad de filas previas. CA01/CA02/CA03/CA05/CA09/CA13/CA15. Evidencia: `docs/05-preparacion-implementacion/19-identificacion-externa-animales.md`. |

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
