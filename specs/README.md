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
| SPEC-01 | [Registro de ganado y sincronización](SPEC-01-registro-ganado-offline/spec.md) | [Plan](SPEC-01-registro-ganado-offline/plan.md) | [Tareas](SPEC-01-registro-ganado-offline/tasks.md) | Aprobada — Implementación en curso; AU01 y AU02–AU04 con cliente React automatizado, sin membresías operativas ni animales |

RF17–RF22 y AC09 ya están integrados en la documentación base. La SPEC-01
está aprobada documentalmente por el responsable el 16 de noviembre de 2026,
incluida la rotación de renovación. Implementación y pruebas siguen pendientes;
el alta real de familiares y las invitaciones necesitan otra SPEC.

Las rutas entre comillas invertidas en documentos y prompts se interpretan
desde la raíz del repositorio. Los enlaces Markdown se resuelven desde el
documento que los contiene.

Las evidencias se vincularán a resultados reales de pruebas. No se crean
resultados ni carpetas de evidencias vacías.
