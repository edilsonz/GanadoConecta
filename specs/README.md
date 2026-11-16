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
| SPEC-01 | [Registro de ganado y sincronización](SPEC-01-registro-ganado-offline/spec.md) | [Plan](SPEC-01-registro-ganado-offline/plan.md) | [Tareas](SPEC-01-registro-ganado-offline/tasks.md) | Pendiente de aprobación — No implementada; plan y tareas propuestos |

RF17–RF22 y AC09 ya están integrados en la documentación base. La SPEC-01
sigue en borrador: roles concretos, validaciones y contratos requieren revisión.
La aprobación de capacidades no aprueba todos sus detalles técnicos.

Las rutas entre comillas invertidas en documentos y prompts se interpretan
desde la raíz del repositorio. Los enlaces Markdown se resuelven desde el
documento que los contiene.

Las evidencias se vincularán a resultados reales de pruebas. No se crean
resultados ni carpetas de evidencias vacías.
