# Rúbrica de evaluación

Seguimiento de los 16 criterios transcritos en la [ruta SDD original](06-evaluacion-sdd/ruta-sdd-rubrica.md). La [matriz inicial de evaluación](06-evaluacion-sdd/matriz-trazabilidad.md) conserva la revisión histórica. Esta tabla registra evidencia disponible y trabajo pendiente. No asigna calificaciones ni representa aprobación docente.

| N.º | Criterio | Evidencia disponible | Pendiente |
|---:|---|---|---|
| 1 | Identificación del problema y requerimientos | [README](../README.md), [actores](01-analisis-de-sistema/01-actores.md) y [matriz funcional](MATRIZ-DE-TRAZABILIDAD.md). | Validación con usuarios y revisión docente. |
| 2 | Análisis y diseño del sistema | [Diseño interno](03-diseno-de-software/diseño-interno-de-modulos.md), [relaciones principales](02-arquitectura-software/diagramas/modelo-de-datos.svg), contratos SDD y [migraciones](../backend/src/main/resources/db/migration/). | Documentar detalles físicos y flujos de capacidades pendientes. |
| 3 | Arquitectura de bajo y alto nivel | [C4 1–3, despliegue, Clean Architecture y sincronización con fuentes Draw.io](02-arquitectura-software/diagramas/README.md), [arquitectura](02-arquitectura-software/arquitectura-inicial.md) y [ADR](01-analisis-de-sistema/07-decisiones-arquitectonicas.md). | Actualizar el despliegue cuando se defina la topología de producción. |
| 4 | Desarrollo basado en SDD | [Índice SDD](../specs/README.md) y commits por incremento. | Completar las SPEC de alcance pendiente. |
| 5 | Pruebas unitarias e integración | Evidencias en tareas de [SPEC-07](../specs/SPEC-07-calendario-campanas/tasks.md), [SPEC-08](../specs/SPEC-08-catalogo-anuncios/tasks.md) y [SPEC-09](../specs/SPEC-09-ventas-moderacion/tasks.md). | Ampliar cobertura según cada incremento. |
| 6 | Pruebas End to End | [Recorridos reproducibles en Chrome por HTTPS](../tests/e2e/README.md) con Spring Boot y PostgreSQL aislado para SPEC-07, SPEC-08, SPEC-09 y navegación. | E2E offline, cambios de permiso y moderación con cuenta autorizada. |
| 7 | Interfaz y experiencia de usuario | Interfaz React responsiva en español, [consolidación de navegación](05-preparacion-implementacion/36-consolidacion-ux.md) y [capturas E2E en Chrome](../img/README.md#interfaz-actual). | Evaluación con usuarios y lector de pantalla. |
| 8 | Escalabilidad del frontend y backend | Monolito modular, PWA, paginación del catálogo en [SPEC-08](../specs/SPEC-08-catalogo-anuncios/plan.md) y [carga por sección](05-preparacion-implementacion/36-consolidacion-ux.md). | Medir recursos, concurrencia, tamaño de respuesta y límites. |
| 9 | Escalabilidad de base de datos | [Migraciones e índices](../backend/src/main/resources/db/migration/) de historiales, consultas y catálogo. | Analizar planes con volumen y medir crecimiento. |
| 10 | Configuración de dominio y/o subdominio, DNS y CDN | [Guía de HTTPS local](../README.md). | Despliegue y configuración real de dominio, DNS y CDN. |
| 11 | Implementación de caché | [ADR-11](01-analisis-de-sistema/07-decisiones-arquitectonicas.md). Catálogo consulta PostgreSQL. | Redis con claves, TTL, invalidación y reintentos verificados. |
| 12 | CI/CD | Comandos locales reproducibles en [README](../README.md). | Workflow, pipeline de despliegue y evidencia de ejecución. |
| 13 | Pruebas de carga k6 | Objetivo AC01 y escenarios en la [ruta SDD](06-evaluacion-sdd/ruta-sdd-rubrica.md). | Scripts, entorno, dataset, ejecuciones y resultados con caché fría, poblada y caída. |
| 14 | Código fuente y base de datos en GitHub | Código, [Compose](../compose.yaml), migraciones y pruebas sintéticas reproducibles en el repositorio. | Verificar publicación del incremento y documentar esquema y datos sintéticos autorizados. |
| 15 | Informe final | Documentación, [hoja de ruta](HOJA-DE-RUTA.md) e [índice de recursos gráficos](../img/README.md). | Redactar informe final con resultados y limitaciones. |
| 16 | Presentación y dominio del tema | Guion en [ruta SDD](06-evaluacion-sdd/ruta-sdd-rubrica.md), ADR y trazabilidad. | Preparar demostración y defensa. |

Para respaldar la base en GitHub se usan migraciones, esquema y datos sintéticos autorizados. Los respaldos con datos privados, secretos y archivos de entorno deben permanecer fuera del repositorio.
