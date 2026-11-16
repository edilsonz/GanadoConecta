# Plan incremental de implementación con Codex

## Regla general

No pedir a Codex «construye toda GanadoConecta» en una sola instrucción. Primero revisar/agregar documentación; después estructura ejecutable; luego verticales funcionales (UI + API + datos + pruebas). Mantener el SDD indicado en `AGENTS.md` y `specs/plantilla-spec.md`.

## Entregas de trabajo

| Etapa | Resultado verificable | Dependencias |
|---|---|---|
| 0 — Coherencia | RF/AC/ADR propuestos integrados explícitamente; arquitectura/C4 con fuentes editables o cambios descritos; specs validadas | Documentación base |
| 1 — Esqueleto | `frontend/`, `backend/`, `servicio-ia/` con comandos reproducibles, configuración local, `docker-compose`, migraciones, salud, CI inicial y variables de entorno de ejemplo | Etapa 0 |
| 2 — Usuarios/familias | Autenticación, autorización de pertenencia, propietario/editor/lector, creación de ganadería, pruebas de aislamiento | Etapa 1 |
| 3 — Animales | Registro flexible, lista «Mi ganado», categorías, ficha, búsquedas, nacimiento y genealogía básica | Etapa 2 |
| 4 — Offline del primer flujo | Instalabilidad PWA, IndexedDB, operaciones pendientes, API idempotente, reintentos y conflictos, test desconexión | Etapa 3 |
| 5 — Salud y peso | Actividades individuales/colectivas, confirmación por animal, historiales y calendario individual/colectivo | Etapa 4 |
| 6 — Reportes + IA | Métricas SQL autorizadas, gráficas, servicio FastAPI, asistente con fuentes revisadas y pruebas de límites | Etapa 5 |
| 7 — Catálogo | Fotos, anuncio público, autorización explícita de genealogía, moderación, venta/cierre transaccional y caché Redis | Etapa 6 |
| 8 — Endurecimiento | Seguridad, accesibilidad, datos de respaldo, pruebas de carga, calidad offline y despliegue | Todas |

**Importante:** no postergar toda la prueba offline hasta después de implementar campañas. El primer flujo animal offline debe existir ya en la etapa 4; los siguientes casos deben integrar la cola existente.

## Contrato de cada tarea de Codex

- Leer antes `AGENTS.md`, requisitos, ADR, specs vinculadas y propuesta v2.
- Explicar supuestos y diferencias con documentos aprobados.
- Modificar únicamente los archivos necesarios; preferir PR/commit pequeño.
- Agregar pruebas relevantes: unitarias, integración y, cuando corresponda, E2E de navegador con modo offline.
- Ejecutar comandos realmente disponibles, informar cuáles se ejecutaron y cuáles no.
- Presentar estructura de archivos modificados, decisiones, evidencias y pendientes.
- Nunca incluir claves reales ni secretos; no introducir proveedores innecesarios.

## Pruebas mínimas previas al despliegue de la primera funcionalidad

1. Crear vacuno con solo sexo -> se genera UUID; cuenta lectora no puede crear.
2. Crear offline desde dispositivo preparado y sincronizar tras recuperar red -> un único registro.
3. Reenviar la misma operación -> no duplica vacuno.
4. Dos cuentas distintas de una ganadería comparten registros con su rol; otra ganadería no puede leerlos.
5. Dos operaciones concurrentes sobre una misma versión -> conflicto explícito, no pérdida silenciosa.
6. Arete no numérico y sin arete son aceptados; nombre opcional; fotografía no obligatoria.
7. Un recurso privado no se hace visible desde endpoints públicos ni por caché de service worker.
8. La pérdida de conexión durante una escritura deja una operación local pendiente, no un mensaje falso de «Sincronizado».

## Alcance de primera demostración al profesor

Una ganadería familiar con 2 cuentas, 3 vacunos (incluido uno sin nombre y uno con arete texto), registro de un peso o vacuna, desconexión, nueva operación pendiente, reconexión y sincronización demostrada con pruebas. No mostrar interfaces ficticias como funcionales.
