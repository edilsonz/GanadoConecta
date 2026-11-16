# Matriz de trazabilidad de los 16 criterios de evaluación

**Estado:** revisión documental del 15 de noviembre de 2026. Se usa la rúbrica
transcrita en [ruta SDD](ruta-sdd-rubrica.md); no se dispuso de una fuente docente
adicional ni se verificó aprobación docente. «Disponible» significa documentación,
no software, despliegue ni prueba aprobada.

Rutas entre comillas invertidas relativas a la raíz del repositorio.
La [matriz de decisiones](../05-preparacion-implementacion/05-consolidacion-arquitectonica.md)
relaciona las 16 decisiones del producto, que son distintas de estos 16 criterios.

| N.º | Criterio | Referencia y trazabilidad | Evidencia disponible | Evidencia pendiente / riesgo |
|---|---|---|---|---|
| 1 | Problema y requerimientos | README; docs/01, RF01–RF22, HU01–HU23, AC01–AC09 | Descripción, actores y requisitos consolidados. | Validación del problema con usuarios y revisión de SPEC; no acreditar evaluación por mera documentación. |
| 2 | Análisis y diseño | docs/03; docs/05-preparacion-implementacion/02-modelo-datos-y-reglas.md; RF18/RF21/RF22 | Responsabilidades, entidades conceptuales y flujos. | Modelo físico, contratos y validaciones; modelo conceptual sigue propuesto. |
| 3 | Alto y bajo nivel | docs/02, docs/03 y docs/04; ADR-01–ADR-15 | Arquitectura y C4 textual consolidados; PNG originales. | Fuentes editables C4 ausentes, regeneración visual pendiente; clases/API/despliegue aún por diseñar. |
| 4 | SDD | specs/plantilla-spec.md; specs/SPEC-01-registro-ganado-offline/; RF02/RF17/RF21 | SPEC borrador, plan y tareas pendientes. | Revisar SPEC, completar plan, derivar tareas y aportar implementación/evidencias; no declarar SDD completado. |
| 5 | Unitarias e integración | AC04–AC07, EC04–EC07/EC12/EC13; SPEC-01 | Escenarios previstos de permisos, transacciones, adaptadores y sincronización. | Seleccionar herramientas y ejecutar pruebas sobre código real; sin resultados. |
| 6 | E2E | AC08/AC09, EC11–EC12; RF17/RF21 | Flujos de preparación, desconexión, reconexión y errores previstos. | Navegador y app ejecutables; Playwright figura como candidato de la ruta SDD, sin ejecución. |
| 7 | Interfaz y UX | AC08, DA07, RC01; docs/05-preparacion-implementacion/03-interfaces-y-navegacion.md | Español, «Mi ganado», responsividad y flujos propuestos. | Prototipo, accesibilidad y pruebas de tareas; paleta exacta y componentes siguen propuestos. |
| 8 | Escalabilidad frontend/backend | AC03, EC03, DA05, ADR-01/10 | Diseño modular y paginación previstos; PWA. | Mediciones de recursos, volumen, solicitudes y límites; diseño no demuestra escalabilidad. |
| 9 | Escalabilidad de BD | AC01/AC03, ADR-04/10, EC01/EC03 | Índices, relaciones y consultas descritos conceptualmente. | Migraciones, planes de consulta y cargas con crecimiento de datos. |
| 10 | Dominio, DNS y CDN | RC01/RC03; fase 5 de ruta SDD | Objetivo de despliegue y separación de recursos públicos/privados. | Hosting, dominio, HTTPS, DNS y CDN por seleccionar/configurar; sin despliegue. |
| 11 | Caché | RF13, AC01/02/04/05, ADR-11, EC09–EC10 | Redis público, caducidad, invalidación posterior y fallback documentados. | TTL, claves y protección de revocación por concretar; pruebas de acierto/ausencia/caducidad/fallo/reintento. |
| 12 | CI/CD | RC02, AC06; fase 1 de ruta SDD | GitHub Actions propuesto para automatización. | Workflows, entornos y logs de ejecución; no hay pipeline implementado. |
| 13 | Carga k6 | AC01, DA05, EC01–EC03 | Objetivo p95 ≤ 2 s con 100 concurrentes; caché vacía/poblada y fallo de Redis. | Confirmar herramienta k6 en plan, datos, duración y errores; scripts y resultados pendientes, IA/fotos excluidos. |
| 14 | Fuente y BD en GitHub | RC02, ADR-04; README | Repositorio local con documentación y organización SDD. | Código, migraciones, esquema y reproducción de instalación; no se verificó el remoto en esta revisión. |
| 15 | Informe final | docs/01–06 y esta matriz | Material base y registro de pendientes/limitaciones. | Informe final con resultados reales, entorno, métricas y conclusiones. |
| 16 | Presentación y dominio | fase 6 de ruta SDD; ADR y matriz de decisiones | Guion de demostración y decisiones explicadas. | Aplicación ejecutable, demostración, defensa y revisión docente. |

## Comprobaciones de esta revisión

Se revisan correspondencias de códigos, enlaces locales y estado de Git.
Las pruebas unitarias, integración, E2E, carga, seguridad ejecutable, CI/CD,
instalación y despliegue no se ejecutan porque esta tarea es documental.
La existencia de diagramas PNG no demuestra que reflejen las decisiones nuevas.

## Resultados de la comprobación documental

- 42 enlaces locales comprobados, sin destinos ausentes.
- Tablas Markdown comprobadas por número de columnas, sin inconsistencias.
- Códigos RF/HU/AC/RC/DA/ADR citados en ambas matrices encontrados en sus documentos de definición.
- Ambas matrices contienen exactamente las filas 1–16, sin omisiones ni duplicados.
- Inspección visual de los PNG C4 niveles 1–3: reflejan el alcance original; no incluyen todavía las ampliaciones de colaboración, PWA/sincronización e IA conversacional.
- Búsqueda de fuentes editables en el árbol del repositorio: no se encontraron archivos asociados a esos PNG.
- git diff --check sin errores. Índice sin cambios preparados; rama develop, HEAD y main conservados.
- No se ejecutaron pruebas unitarias, de integración, E2E ni de carga. No hay cambios en imágenes ni código.
