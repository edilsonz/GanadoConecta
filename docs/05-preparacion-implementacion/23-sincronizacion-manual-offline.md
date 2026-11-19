# T13/T14 — Sincronización manual segura de animales pendientes

**Estado: implementada y verificada automática y manualmente para un envío controlado.**

## Alcance implementado

- «Mis animales» muestra el contador y el botón explícito «Sincronizar
  pendientes». Montar React, recargar, recuperar red o recibir el evento `online`
  no inicia envíos.
- El orquestador trabaja solo con el usuario autenticado y la ganadería
  seleccionada. Antes de cada lote consulta `GET /ganaderias/{id}/acceso`; el
  contexto offline no autoriza el envío.
- Solo PROPIETARIO y EDITOR vigentes sincronizan. LECTOR, 401 y 403 bloquean o
  pausan la cola sin borrar su contenido.
- Cada intento reenvía el sobre original completo con los mismos `operationId` y
  `animalId`. El backend existente devuelve APLICADO o REPROCESADO; 409 sigue
  siendo conflicto y nunca se interpreta como confirmación.
- Una transacción IndexedDB reclama la operación con lease de 60 segundos. Web
  Locks coordina pestañas cuando está disponible; el lease y la unicidad remota
  siguen protegiendo cuando no lo está. Un envío abandonado puede recuperarse al
  vencer el lease.
- La llamada HTTP tiene límite de 20 segundos. Red, timeout, 429 y 5xx conservan
  la operación para reintento manual; 400/413/422 y 409 quedan visibles para
  revisión. Un error local posterior a la confirmación remota no se presenta como
  éxito local ni genera identificadores nuevos.
- El recibo válido se conserva en IndexedDB con la operación en estado
  SINCRONIZADO; `listarPendientes` deja de mostrarla, pero no se borra la evidencia.
- JWT continúa exclusivamente en memoria y CSRF/cookie segura usan el cliente ya
  aprobado. IndexedDB no recibe tokens, contraseñas, cookies ni valores CSRF.

No fue necesario modificar Spring Boot ni Flyway: el POST existente ya revalida
la membresía dentro de la transacción y aplica idempotencia persistente por
operación y animal.

## Manejo de concurrencia e incertidumbre

El botón se deshabilita durante el lote y el servicio rechaza un segundo lote del
mismo ámbito en la pestaña. Para varias pestañas se combinan Web Locks y una
reclamación IndexedDB atómica. PostgreSQL continúa como defensa final: si guardó
el animal pero se perdió la respuesta, el registro queda reintentable y el mismo
sobre obtiene REPROCESADO sin crear otro animal.

Un cambio de ganadería, cuenta o desmontaje de la vista cancela la petición en
curso. No se marca SINCRONIZADO ante aborto o respuesta incierta. Si 401/403 se
produce durante el lote, no se siguen enviando operaciones posteriores.

## Verificación ejecutada

Durante el desarrollo se ejecutaron pruebas dirigidas sobre el almacén, el
orquestador y React. Cubren aislamiento, payload íntegro (incluida identificación
textil), sexo mínimo, lease recuperable, respuesta perdida, REPROCESADO, doble
pestaña simulada, 401/403/409/422/5xx, fallo IndexedDB posterior al éxito remoto,
ausencia de ejecución automática, acción explícita, refresco y mensajes.

Resultado final: 12 archivos de prueba y 111 pruebas frontend aprobadas, sin
fallos; `npm run typecheck` y `npm run build` finalizaron correctamente (31
módulos transformados). No se ejecutaron pruebas backend porque no cambió
backend. Las pruebas usan IndexedDB y dependencias aisladas: no tocaron
`PRUEBA-OFFLINE-02`, PostgreSQL ni los animales reales existentes.

La validación manual posterior confirmó que `PRUEBA-OFFLINE-02` se sincronizó
una sola vez, permaneció en PostgreSQL tras F5 y no mostró duplicados visibles.
Esta observación acredita ese recorrido concreto; no sustituye E2E automatizado,
inspección multipestaña ni los escenarios de conflicto pendientes.

## Pendiente

- Validar mediante E2E de navegador los escenarios de respuesta perdida,
  multipestaña y conflicto.
- Apertura PWA desde cero sin red y caché de fichas privadas; sincronización
  automática y reintentos programados con backoff/Retry-After.
- E2E real de dos pestañas, suspensión/cierre durante HTTP y resolución asistida
  o descarte explícito de conflictos.

Este incremento no completa todavía el alcance Offline-First de SPEC-01.
