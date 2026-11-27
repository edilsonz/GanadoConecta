# Organización de las especificaciones SDD

Cada funcionalidad utiliza una carpeta `SPEC-XX-nombre-descriptivo/` con:

- `spec.md`: objetivo, requisitos, permisos, reglas, flujos, criterios de aceptación y pruebas.
- `plan.md`: diseño técnico y verificaciones, después de revisar la especificación.
- `tasks.md`: tareas ordenadas y su estado, derivadas del plan revisado.

La [plantilla original](plantilla-spec.md) se conserva sin modificaciones para
nuevas especificaciones. Reorganizar un borrador no implica aprobarlo.

## Funcionalidades existentes

| Identificador | Especificación | Plan | Tareas | Estado |
|---|---|---|---|---|
| BASE-01 | [Estructura ejecutable](BASE-01-estructura-ejecutable/spec.md) | [Plan](BASE-01-estructura-ejecutable/plan.md) | [Tareas](BASE-01-estructura-ejecutable/tasks.md) | Base y conexión PostgreSQL/JDBC verificadas. No completa SPEC-01 |
| SPEC-01 | [Registro de ganado y sincronización](SPEC-01-registro-ganado-offline/spec.md) | [Plan](SPEC-01-registro-ganado-offline/plan.md) | [Tareas](SPEC-01-registro-ganado-offline/tasks.md) | Alta, listado, ficha, PWA y sincronización parcial validados. Consulta privada offline y edición versionada siguen pendientes. |

| SPEC-02 | [Seguimiento sanitario](SPEC-02-seguimiento-sanitario/spec.md) | [Plan](SPEC-02-seguimiento-sanitario/plan.md) | [Tareas](SPEC-02-seguimiento-sanitario/tasks.md) | Historial, vacunación, agenda y sincronización de eventos sanitarios habilitados. Baño individual en interfaz pendiente. |
| SPEC-03 | [Reproducción, partos y genealogía](SPEC-03-reproduccion-partos-genealogia/spec.md) | [Plan](SPEC-03-reproduccion-partos-genealogia/plan.md) | [Tareas](SPEC-03-reproduccion-partos-genealogia/tasks.md) | Registro online y genealogía auditada. Estimación automática de parto pendiente de criterios. |
| SPEC-04 | [Acceso familiar](SPEC-04-acceso-familiar/spec.md) | [Plan](SPEC-04-acceso-familiar/plan.md) | [Tareas](SPEC-04-acceso-familiar/tasks.md) | Invitaciones manuales y administración de colaboradores. Transferencia de titularidad pendiente. |
| SPEC-05 | [Control de peso](SPEC-05-historial-peso/spec.md) | [Plan](SPEC-05-historial-peso/plan.md) | [Tareas](SPEC-05-historial-peso/tasks.md) | Registro e historial online con gráfico. Validación manual pendiente. |
| SPEC-06 | [Alta masiva](SPEC-06-alta-masiva/spec.md) | [Plan](SPEC-06-alta-masiva/plan.md) | [Tareas](SPEC-06-alta-masiva/tasks.md) | Alta online por tabla. Validación manual pendiente. |
| SPEC-07 | [Calendario y campañas](SPEC-07-calendario-campanas/spec.md) | [Plan](SPEC-07-calendario-campanas/plan.md) | [Tareas](SPEC-07-calendario-campanas/tasks.md) | Calendario online, campañas y avisos internos. Entrega externa pendiente. |
| SPEC-08 | [Anuncios y catálogo](SPEC-08-catalogo-anuncios/spec.md) | [Plan](SPEC-08-catalogo-anuncios/plan.md) | [Tareas](SPEC-08-catalogo-anuncios/tasks.md) | Proyección pública, gestión y fotos con adaptador local. Redis y proveedor definitivo pendientes. |
| SPEC-09 | [Ventas y moderación](SPEC-09-ventas-moderacion/spec.md) | [Plan](SPEC-09-ventas-moderacion/plan.md) | [Tareas](SPEC-09-ventas-moderacion/tasks.md) | Venta y revisión auditada. Recepción de reportes desactivada por defecto. |
| SPEC-10 | [Administración](SPEC-10-administracion-usuarios/spec.md) | [Plan](SPEC-10-administracion-usuarios/plan.md) | [Tareas](SPEC-10-administracion-usuarios/tasks.md) | Consulta y auditoría con permiso de plataforma. Acciones sobre cuentas pendientes de aprobación. |
| SPEC-11 | [Reportes ganaderos](SPEC-11-reportes-ganaderos/spec.md) | [Plan](SPEC-11-reportes-ganaderos/plan.md) | [Tareas](SPEC-11-reportes-ganaderos/tasks.md) | Agregados sanitarios, reproductivos y de peso con filtros, autorización y evidencia en navegador. |
| SPEC-12 | [Análisis, alertas y asistente](SPEC-12-analisis-alertas-asistente/spec.md) | [Plan](SPEC-12-analisis-alertas-asistente/plan.md) | [Tareas](SPEC-12-analisis-alertas-asistente/tasks.md) | Análisis descriptivo, avisos de agenda y conversación local sobre registros propios verificados. Corpus y avisos por tendencias pendientes. |

RF17–RF22 y AC09 forman parte de la documentación base. La SPEC-01 fue
aprobada el 16 de noviembre de 2026, incluida la rotación de renovación.
Su alcance offline sigue en implementación incremental.

La [matriz del Sprint 2](../docs/07-sprint-2/matriz-calidad.md) vincula los
escenarios de calidad con decisiones, código y resultados de pruebas.

Las rutas entre comillas invertidas en documentos y prompts se interpretan
desde la raíz del repositorio. Los enlaces Markdown se resuelven desde el
documento que los contiene.

Las evidencias se vincularán a resultados reales de pruebas. No se crean
resultados ni carpetas de evidencias vacías.
