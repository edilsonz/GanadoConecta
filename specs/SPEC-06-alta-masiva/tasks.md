# Tareas — SPEC-06

- [x] LT01: edición y vista previa.
- [x] LT02–LT04: ejecución secuencial, estado por fila y recuperación.
- [x] Pruebas Vitest y evidencia.

El 21/11/2026 pasaron dos casos dirigidos de confirmación parcial y reintento
sin regenerar identificadores. La regresión frontend final aprobó 224 pruebas
en 30 archivos, junto con TypeScript y build PWA. Se reutiliza la regresión
backend con PostgreSQL aislado del bloque anterior (158 pruebas con V13).
La validación manual en Chrome está pendiente.
