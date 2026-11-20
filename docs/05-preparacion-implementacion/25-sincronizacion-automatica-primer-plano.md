# Evidencia T13/T14 — Sincronización automática controlada en primer plano

**Fecha:** 20 de noviembre de 2026
**Estado:** implementada y automatizada; prueba manual en Chrome pendiente.

## Alcance implementado

React ofrece «Sincronización automática al recuperar conexión» dentro de
«Mis animales». Está desactivada por defecto y solo PROPIETARIO o EDITOR pueden
activarla para la cuenta, sesión y ganadería actuales. La preferencia permanece
únicamente en memoria: cerrar sesión, desmontar la vista o cambiar de contexto
detiene el mecanismo y una sesión posterior no la hereda.

La ejecución ocurre solo con la aplicación abierta y en primer plano. El evento
`online` es una señal para intentar, no prueba conectividad: el mismo motor manual
consulta `/acceso` y revalida sesión, ganadería, membresía y rol antes de enviar.
El botón «Sincronizar pendientes» se conserva. Ambos caminos comparten Web Locks,
lease en IndexedDB, identificadores inmutables y la idempotencia de PostgreSQL.
También toman el bloqueo PWA compartido para impedir que una actualización se
active mientras comienza un envío.

No se implementó Background Sync, envío desde Service Worker, sincronización con
la aplicación cerrada, login offline ni persistencia de la preferencia. El Service
Worker sigue sin abrir IndexedDB ni cachear respuestas privadas.

## Política de reintentos

- Un intento inmediato y hasta cuatro reintentos con esperas base de 2, 5, 15 y
  30 segundos, más jitter de 0 a 1 segundo.
- `Retry-After` válido en 429/5xx se respeta cuando exige una espera mayor; el
  cliente limita valores remotos a cinco minutos para evitar temporizadores
  abusivos.
- Solo red, 429 y 5xx son reintentables. 400/413/422, 409, 401 y 403 detienen el
  ciclo o requieren acción, sesión o permisos según el contrato.
- Perder conexión o desactivar la opción cancela temporizadores nuevos. Una
  solicitud ya enviada no se aborta de forma insegura; su recibo se procesa y no
  inicia otro intento si el contexto dejó de estar activo.
- Tras cinco fallos se pausa y se conserva la operación completa en IndexedDB.
  Nunca se regeneran `operationId` o `animalId` ni se reenvía `SINCRONIZADO`.

## Estados visibles y seguridad

La interfaz diferencia desactivada, activada, espera de reconexión,
sincronización, reintento programado, confirmación, acción requerida, sesión
expirada y permiso insuficiente. Un registro pendiente continúa identificado
como local hasta recibir un recibo válido del servidor. Ningún JWT, token de
renovación, contraseña o secreto nuevo se guarda en IndexedDB, almacenamiento
web, Cache Storage o Service Worker.

## Verificación automatizada

Ejecutada el 20 de noviembre de 2026:

- pruebas dirigidas del controlador, motor, API, panel y coordinación PWA:
  **73/73** correctas;
- regresión frontend completa: **151/151** correctas en 17 archivos;
- `npm.cmd run typecheck`: correcto;
- `npm.cmd run build`: correcto; Vite generó el app shell PWA;
- `git diff --check`: se ejecuta en la revisión final previa al commit.

El backend no cambió y su suite no se repitió. No se realizó todavía la prueba
manual `PRUEBA-AUTO-01`, Background Sync ni una prueba real multipestaña.

## Prueba manual segura pendiente

Usar una cuenta sintética PROPIETARIO o EDITOR y una ganadería de prueba:

1. Abrir «Mis animales», preparar el dispositivo confiable y activar
   «Sincronización automática al recuperar conexión».
2. En Chrome DevTools → Network elegir `Offline` y registrar un animal llamado
   `PRUEBA-AUTO-01`. Confirmar que aparece en «Pendientes en este dispositivo».
3. Sin cerrar la aplicación, volver a `No throttling`. Debe verse la revalidación,
   el envío o un reintento temporal; no crear otra operación.
4. Confirmar que el pendiente pasa a sincronizado y aparece una sola vez en el
   listado. Verificar en IndexedDB que conserva los IDs y el recibo.
5. Verificar mediante una consulta de solo lectura que existe una sola fila con
   ese `animalId`. La prueba no se declara ejecutada en este documento.

Los animales existentes y las operaciones ya sincronizadas no se modifican.
