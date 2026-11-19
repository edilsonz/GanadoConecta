# T10–T12/T15 — Registro provisional offline en React

Fecha: 19 de noviembre de 2026, America/Lima. Estado: **incremento limitado
implementado y verificado automáticamente; navegador real y sincronización
pendientes**.

## Alcance implementado

- «Mis animales» conserva intacta el alta online. Un éxito REST no crea una
  operación local duplicada.
- La preparación offline es voluntaria y exige seleccionar «Preparar trabajo
  sin conexión» mientras `/acceso` confirma online usuario, ganadería y rol. El
  contexto dura como máximo 168 horas y una recarga no crea un permiso ausente.
- Solo una caída real de transporte permite intentar el guardado local. Las
  respuestas 401, 403, validación, conflicto, límite o 5xx no se convierten en
  registros offline.
- PROPIETARIO y EDITOR con contexto vigente pueden guardar el mismo sobre
  `schemaVersion=2`; LECTOR, contexto ausente, vencido, bloqueado o sin sesión
  no pueden hacerlo.
- IndexedDB conserva `animalId`, `operationId`, sexo y todos los opcionales
  aprobados. La transacción termina antes de mostrar «Pendiente de
  sincronización». Un fallo local conserva formulario e IDs para reintentar.
- Los pendientes se muestran en una sección separada de los animales
  confirmados en PostgreSQL. Se consultan por usuario y ganadería y quedan
  ocultos al cambiar contexto o cerrar/perder la sesión, sin borrarse.
- Logout, pérdida de sesión y rechazo de acceso bloquean el contexto local. No
  se guardan contraseñas, JWT, refresh, cookies ni CSRF en IndexedDB.

El contexto local sigue siendo manipulable por quien controla el perfil del
navegador. No sustituye la autorización del backend: antes de una futura
sincronización se deberá revalidar sesión, ganadería, membresía y rol en
PostgreSQL.

## Verificación ejecutada

| Verificación | Resultado real |
|---|---|
| Pruebas dirigidas de formulario, sesión, autorización y stores | 52 aprobadas, 0 fallos. |
| Regresión frontend completa | 87 aprobadas en 11 archivos, 0 fallos. |
| TypeScript | `tsc -b` correcto. |
| Build Vite | Correcto; 30 módulos transformados. |
| Backend | No ejecutado: no hubo cambios backend. |

Se cubrieron éxito online, caída de red, 4xx/5xx diferenciados, opt-in, roles,
autorización ausente/vencida, reapertura simulada, aislamiento por cuenta y
ganadería, cierre de sesión, error IndexedDB, doble envío, UUID/idempotencia,
solo arete, solo lana/cinta, solo sexo y representación visual del pendiente.
Las pruebas de IndexedDB usaron el doble inyectable existente; no se realizó una
prueba manual en Chrome en este incremento.

## Corrección posterior de visibilidad

Una prueba manual informó que, después de intentar preparar el dispositivo, el
control ya no se encontraba y un alta supuestamente offline apareció como
confirmada. La revisión comprobó dos comportamientos distintos:

- al preparar correctamente, el botón desaparecía y solo quedaba un aviso
  temporal que se borraba al editar el formulario; se corrigió mostrando de
  forma persistente «Trabajo sin conexión preparado» y el estado de red que
  informa el navegador;
- «Animal registrado correctamente» solo se emite tras recibir un recibo HTTP
  válido. La permanencia tras F5 confirma que aquella operación llegó a
  PostgreSQL, no a la cola local. El indicador del navegador no fuerza el modo
  offline: IndexedDB solo se usa cuando la petición falla realmente como red.

La corrección añadió regresiones para la confirmación persistente, los eventos
`online`/`offline` como señal informativa y la clasificación de un rechazo real
de `fetch`. Resultado posterior: 37/37 pruebas dirigidas y 89/89 en la regresión
frontend; TypeScript y build Vite correctos. No se ejecutó backend ni se alteró
PostgreSQL, Flyway o IndexedDB.

## Corrección posterior de visibilidad

Una prueba manual informó que, después de intentar preparar el dispositivo, el
control ya no se encontraba y un alta supuestamente offline apareció como
confirmada. La revisión comprobó dos comportamientos distintos:

- al preparar correctamente, el botón desaparecía y solo quedaba un aviso
  temporal que se borraba al editar el formulario; se corrigió mostrando de
  forma persistente «Trabajo sin conexión preparado» y el estado de red que
  informa el navegador;
- «Animal registrado correctamente» solo se emite tras recibir un recibo HTTP
  válido. La permanencia tras F5 confirma que aquella operación llegó a
  PostgreSQL, no a la cola local. El indicador del navegador no fuerza el modo
  offline: IndexedDB solo se usa cuando la petición falla realmente como red.

La corrección añadió regresiones para la confirmación persistente, los eventos
`online`/`offline` como señal informativa y la clasificación de un rechazo real
de `fetch`. Resultado posterior: 37/37 pruebas dirigidas y 89/89 en la regresión
frontend; TypeScript y build Vite correctos. No se ejecutó backend ni se alteró
PostgreSQL, Flyway o IndexedDB.

## Límites pendientes

- No hay Service Worker: la aplicación no puede prometer apertura o recarga
  desde cero sin internet. El flujo solo aplica a una aplicación ya abierta.
- No existe envío automático/manual de la cola, backoff, lease multipestaña,
  reconciliación de recibos ni resolución de conflictos.
- No se cachean todavía páginas o fichas confirmadas para consulta offline.
- Falta validar IndexedDB, desconexión física, recarga, cambio de cuenta y
  revocación en Chrome real sin poner en riesgo datos existentes.
