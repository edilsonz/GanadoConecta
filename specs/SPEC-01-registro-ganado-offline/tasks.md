# SPEC-01 — Tareas incrementales propuestas

**Estado:** plan de trabajo para aprobación; ninguna tarea de implementación o
prueba ejecutada. Depende de [spec.md](spec.md) y [plan.md](plan.md). Todos los
checks permanecen abiertos. No ejecutar estas tareas en la presente revisión.

## 0. Aprobación y dependencias

| ID | Tarea | Depende de | Entregable / verificación futura |
|---|---|---|---|
| T01 | Aprobar DP01–DP08, CA01–CA18 y alcance individual sin edición/masivo/fotos. | Revisión del responsable | Acta/estado de aprobación; no marcar implementado. |
| T02 | Disponer de contrato aprobado y autenticación, ganadería activa, membresía y roles PROPIETARIO/EDITOR/LECTOR implementados y verificados; incluir revocación transaccional. | T01 | Dependencia previa al registro real y preparación offline: integración de login, pertenencia y matriz de roles; negativos anónimo/lector/ajeno/revocado. Contrato o mocks solos no habilitan producción. |
| T03 | Fijar versiones de Java/Spring, Node/React, PostgreSQL y herramientas propuestas, navegadores objetivo y comandos reproducibles. | T01 | Decisiones registradas, archivos de dependencias en tarea de implementación posterior; sin secretos. |


**Bloqueo de habilitación:** T02 debe estar cumplida antes de cualquier registro,
consulta o preparación offline con datos reales. Las tareas de dominio y UI
pueden trabajarse con dobles/datos sintéticos sin habilitar ese flujo. La falta
de contrato o implementación del acceso familiar no se resuelve desactivando
seguridad. Invitaciones y registro masivo se gestionarán mediante otras SPEC.
La caducidad offline nunca es una tarea de limpieza de registros pendientes.
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
| T17 | Ejecutar PI01–PI05 contra API/PostgreSQL y navegador controlados. | T07, T08, T09, T12, T13, T15 | Autenticación real y pertenencia/roles como prerrequisitos, IDOR, sexo omitido/null/inválido sin escritura, concurrencia, rollback, respuesta perdida, cuota y ausencia de dependencia Redis/IA/notificaciones; CA02–CA17 aplicables. |
| T18 | Ejecutar EE01–EE04 con propietario/editor/lector y dos ganaderías sintéticas. | T10–T17 | Sexo obligatorio online/offline; ventana vencida/sesión perdida sin borrar cola; reautenticación de la misma cuenta y otra cuenta sin acceso; reapertura, doble clic/pestaña, 401/403/409/422/429/5xx, errores de protocolo y almacenamiento; CA01–CA16 aplicables. |
| T19 | Documentar entorno/dataset PC01 y ejecutar k6 cuando exista flujo funcional; analizar planes de consulta. | T03, T09, T17, T18 | CA18 y AC01: 100 concurrentes, p95 ≤ 2 s y errores nominales 0; registrar fallos sin ocultarlos. |
| T20 | Integrar suites disponibles a CI y registrar comandos/evidencias reales sin secretos. | T16, T17, T18 | Criterios 5/6/12/14; pipeline ejecutado solo cuando exista, sin prometer despliegue. |

## 4. Cierre del incremento

- [ ] T21: relacionar cada CA con sus resultados PU/PI/EE/PC; documentar entorno,
  commit evaluado, fecha, comandos, datos sintéticos, errores y limitaciones.
- [ ] T22: actualizar documentación global y estado de SPEC solo tras revisar
  resultados; mantener explícito lo excluido y las fuentes C4 aún pendientes.
- [ ] T23: revisión final y demostración: propietario/editor crea, lector consulta,
  guardar sin red, recuperar respuesta perdida, reconectar sin duplicados y
  rechazar una membresía revocada. No afirmar que RF02/RF20/RF21 completos están cubiertos.

T21 depende de T16–T20; T22 y T23 dependen de T21. Registrar cualquier ajuste de
SPEC antes de modificar comportamiento. No hay autorización para git add,
commit, push, cambios en main ni implementación durante esta tarea documental.
