# Evidencia T13/T14 — Errores y conflictos de sincronización

**Fecha:** 20 de noviembre de 2026
**Estado:** implementado; prueba manual en Chrome pendiente.

## Comportamiento implementado

- «Mis animales» conserva visibles todas las operaciones locales no resueltas,
  incluso cuando el animal ya aparece en la lectura remota pero falta guardar el
  recibo idempotente en IndexedDB.
- La interfaz diferencia red, servidor, sesión, autorización, validación,
  protocolo y conflicto HTTP 409 mediante mensajes seguros en español.
- El botón manual existente solo habilita el motor cuando hay operaciones
  recuperables. Reutiliza el sobre original, `operationId` y `animalId`.
- Validación, protocolo y conflicto permanecen bloqueados para reintentos
  automáticos. No se editan, fusionan, descartan ni sustituyen operaciones.
- Sesión o autorización exigen reautenticación y revalidación de `/acceso` antes
  de que el motor pueda reclamar nuevamente la operación.
- Un segundo rechazo CSRF/origen se clasifica como rechazo de protocolo y no
  entra al backoff. Red, 429 y 5xx conservan el comportamiento reintentable.

No se modificaron backend, endpoints, PostgreSQL, Flyway ni otros módulos. Web
Locks, lease IndexedDB, timeout y la idempotencia remota continúan siendo las
únicas rutas de envío; no se creó un segundo sincronizador.

## Verificación

Ejecutada el 20 de noviembre de 2026:

- pruebas dirigidas de panel y motores: **70/70** correctas;
- regresión frontend completa: **162/162** correctas en 17 archivos;
- `npm.cmd run typecheck`: correcto;
- `npm.cmd run build`: correcto, incluido el app shell PWA.

Las pruebas cubren clasificación, conservación ante fallo local, mensajes React,
permisos, sesión, reintento idempotente y bloqueo de reintentos permanentes. El
backend no cambió y no se repitió su suite. La prueba manual sigue pendiente.

## Prueba manual segura propuesta

1. Mantener desactivada la sincronización automática, preparar el dispositivo y
   crear offline un animal sintético `PRUEBA-ERROR-01`.
2. Volver online y, en Chrome DevTools → Network request blocking, bloquear solo
   la URL POST de animales de esa ganadería. No modificar IndexedDB.
3. Pulsar «Sincronizar pendientes». Debe conservarse el registro y mostrarse el
   error temporal de red/servidor con el mismo número de intentos incrementado.
4. Retirar el bloqueo y pulsar nuevamente el mismo botón. No crear otro animal ni
   otro sobre; el motor reutiliza los IDs originales.
5. Confirmar un único animal remoto y el recibo `APLICADO` o `REPROCESADO` en
   `ganadoconecta-offline/operacionesPendientes`.

Esta prueba no se declara ejecutada todavía.
