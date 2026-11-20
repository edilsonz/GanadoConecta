# Evidencia T10 — Estados del listado «Mis animales»

**Fecha:** 20 de noviembre de 2026
**Estado:** implementado y verificado automáticamente.

## Comportamiento

- La carga inicial permanece separada del resultado remoto.
- «Aún no hay animales registrados» solo aparece después de una respuesta válida
  con `items` vacío.
- Un fallo muestra «No se pudo cargar el listado de animales» y mantiene
  disponible «Actualizar».
- Si existía una lista confirmada, un fallo temporal conserva sus elementos y
  advierte que podrían estar desactualizados.
- Un error sin datos previos no se representa como vacío confirmado.
- Animales remotos y operaciones pendientes de IndexedDB continúan en secciones
  separadas; registro y sincronización no cambian.

No se modificaron backend, API, PostgreSQL, Flyway ni Service Worker.

## Verificación

Ejecutada el 20 de noviembre de 2026:

- pruebas dirigidas del panel: **41/41** correctas;
- regresión frontend completa: **165/165** correctas en 17 archivos;
- `npm.cmd run typecheck`: correcto;
- `npm.cmd run build`: correcto, incluido el app shell PWA.

Se cubrieron respuesta vacía válida, error inicial sin falso vacío, error después
de una carga correcta con conservación de datos y recuperación mediante
«Actualizar». El backend no cambió y no se repitió su suite.
