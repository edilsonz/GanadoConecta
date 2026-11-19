# T11 — Primer almacenamiento de operaciones pendientes en IndexedDB

Fecha: 19 de noviembre de 2026, America/Lima. Estado: **primer incremento
implementado y verificado con doble de IndexedDB; integración y navegador real
pendientes**.

## Alcance implementado

- `AlmacenOperacionesPendientesIndexedDb` usa la API nativa IndexedDB y un
  object store versionado `operacionesPendientes`.
- Cada registro conserva `operationId`, `animalId`, payload inmutable, usuario,
  ganadería, estado local, código de error seguro y fechas locales.
- Los índices garantizan operationId único y un animal pendiente por contexto.
  Repetir el mismo sobre es idempotente; cambiar contenido, contexto o identidad
  produce un conflicto local sin sobrescribir.
- Guardar, listar, obtener y actualizar estado exigen el ámbito explícito de
  usuario y ganadería. Una consulta de otro ámbito no devuelve la operación.
- Solo se admiten propiedades del contrato de alta. Contraseñas, JWT, tokens de
  renovación y campos desconocidos son rechazados y no se registran ni imprimen.
- No existe TTL ni limpieza automática. Una operación pendiente continuó
  disponible 169 horas después de crearla; la ventana de 168 horas deberá ser
  aplicada por el contexto de autorización de T12/T15, no por borrado de datos.
- Un fallo de escritura se propaga como error seguro y la operación no se
  presenta como guardada.

No se modificaron React visual, backend, PostgreSQL ni Flyway. Los animales ya
confirmados no son leídos ni alterados por este módulo.

## Verificación real

| Verificación | Resultado |
|---|---|
| Pruebas dirigidas del módulo | 7 aprobadas, 0 fallos. |
| Regresión frontend completa | 60 aprobadas en 9 archivos, 0 fallos. |
| TypeScript `tsc -b` | Correcto. |
| Build Vite | Correcto; 25 módulos transformados. |

Las pruebas cubren reapertura conservando IDs/payload, aislamiento entre dos
usuarios y dos ganaderías, guardado idempotente, conflicto por contenido o
animal, cambio de estado, conservación después de 168 horas, rechazo de campos
sensibles, fallo simulado de cuota y ausencia de IndexedDB.

La reapertura y los fallos se verificaron con un doble inyectado que implementa
el contrato IndexedDB usado por el adaptador. No se ejecutó IndexedDB real en
Chrome, Playwright ni una prueba manual offline; por ello PI04 y CA04/CA11/CA16
solo quedan parciales. No se repitió backend porque ningún archivo backend cambió.

## Pendientes para registrar realmente sin internet

- Integrar en una sola transacción local el provisional visible y la operación,
  y conectar el formulario sin afirmar éxito antes del `complete` de IndexedDB.
- Implementar contexto preparado, opt-in y autorización máxima de 168 horas;
  bloquear visibilidad/envío al vencer sin borrar la cola.
- Añadir PWA/recursos estáticos, orquestador de sincronización, leases entre
  pestañas, reintentos, confirmaciones y conflictos T12–T15.
- Revalidar sesión, membresía y rol online antes de cualquier envío.
- Ejecutar PI04/EE en navegador real, incluidas recarga, cuota, cierre de pestaña,
  cambio de cuenta y revocación.
