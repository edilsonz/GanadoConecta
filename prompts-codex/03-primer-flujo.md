# Prompt 03 — Primera funcionalidad vertical: registro flexible de animales

Lee `AGENTS.md`, los requisitos aprobados, `specs/SPEC-01-registro-ganado-offline/spec.md` (y la versión aprobada si fue actualizada) y los documentos de arquitectura y UI.

Implementa verticalmente, **sin abarcar más módulos**:
1. Esquema PostgreSQL mínimo: ganadería, usuario, membresía, animal; migraciones con restricciones e índices.
2. Autenticación y autorización funcional por ganadería/rol; propietario y editor crean; lector consulta; otra ganadería nunca accede.
3. Alta flexible de vacuno: sexo obligatorio, UUID generado, nombre/arete/raza opcionales y categoría no inferida falsamente; lista de «Mi ganado» y ficha básica.
4. Frontend React responsivo fiel al estilo verde/blanco, etiquetas en español, estado vacío, validaciones accesibles y pruebas.
5. PWA/IndexedDB con cola persistente para la operación de ALTA; estado «Guardado en este dispositivo / pendiente / sincronizado / error»; sincronización automática al regresar señal, más botón manual.
6. Endpoint idempotente de sincronización con `operationId`, verificación de permiso y control de duplicado, sin mezclar datos de otras ganaderías.
7. Pruebas unitarias, integración y E2E del flujo offline/red, reintento y operación duplicada. Mostrar evidencias de las pruebas ejecutadas.

**No implementar en esta tarea:** IA, catálogo, venta, genealogía completa, campañas o compra. No incluir datos ficticios sin etiquetar como demo. Mantén historial y extensiones preparadas para fases siguientes sin crear arquitectura excesiva.

Reporta: archivos modificados, contratos API y migraciones, decisiones, pasos de ejecución, pruebas reales, riesgos y próximos incrementos.
