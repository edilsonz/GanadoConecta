# GanadoConecta — Preparación arquitectónica para Codex (propuesta v2)

> **Avance de implementación:** ya existe una base ejecutable sin funciones de
> negocio. Consultar el [informe de BASE-01](06-base-ejecutable.md) para pruebas
> ejecutadas y limitaciones, y la
> [evidencia del esquema de Identidad](07-esquema-identidad.md) para la parte de
> persistencia iniciada en T02.1. Los estados de propuesta siguientes describen la
> preparación arquitectónica original, no acreditan implementación de SPEC-01.

> **Estado de consolidación:** las 16 decisiones aprobadas por el responsable
> del proyecto se integran en la [base consolidada](05-consolidacion-arquitectonica.md).
> Los detalles de este documento que no figuren como aprobados allí siguen
> siendo propuestas pendientes de SPEC. No hay implementación ni pruebas funcionales.

**Estado:** documentación de trabajo preparada a partir del ZIP original y las decisiones conversadas; **NO es software implementado, probado ni aprobado por el docente**.

## Cómo usar

1. Conserva una copia del repositorio original antes de incorporar cambios.
2. Abre la raíz del repositorio GanadoConecta en VS Code/Codex. El archivo [AGENTS.md](../../AGENTS.md) original se conserva sin modificaciones.
3. Lee `docs/05-preparacion-implementacion/` en el orden numérico.
4. Usa `prompts-codex/01-validacion-arquitectura.md` como **primera tarea de Codex**. Es una revisión y actualización de documentación, no la implementación completa.
5. Revisa el resultado y aprueba o ajusta las diferencias con los documentos originales (`docs/01-` a `docs/04-`).
6. Solo después utiliza `02-proyecto-base.md` y `03-primer-flujo.md`, en tareas independientes.

## Material nuevo agregado

- `docs/05-preparacion-implementacion/00-alcance-y-decisiones-v2.md`
- `docs/05-preparacion-implementacion/01-arquitectura-v2.md`
- `docs/05-preparacion-implementacion/02-modelo-datos-y-reglas.md`
- `docs/05-preparacion-implementacion/03-interfaces-y-navegacion.md`
- `docs/05-preparacion-implementacion/04-plan-codex.md`
- `specs/SPEC-01-registro-ganado-offline/spec.md`
- `prompts-codex/01-validacion-arquitectura.md`
- `prompts-codex/02-proyecto-base.md`
- `prompts-codex/03-primer-flujo.md`

## Documentación original

La documentación original define RF01–RF16, AC01–AC08, ADR-01–ADR-11 y tres niveles C4. Esta propuesta no sustituye silenciosamente esos documentos. La consolidación integra RF17–RF22, AC09 y ADR-12–ADR-15 para el alcance aprobado; pantallas, entidades y detalles adicionales siguen como propuestas para revisión. No se modificaron los diagramas PNG originales ni se afirmó que existiera código funcional.

## Meta del primer ciclo de código

Un productor autorizado registra un vacuno con sexo y código técnico, puede asignarle nombre/arete opcional, verlo en «Mi ganado», guardar un registro sin conexión en IndexedDB y sincronizarlo sin duplicados al recuperar internet. Probar también el aislamiento entre ganaderías. Las campañas, IA y catálogo se agregan después por incrementos verificables.

## Rutas de esta guía

Las rutas entre comillas invertidas se expresan desde la raíz del repositorio.
Consultar el [README principal](../../README.md), las [convenciones SDD](../../specs/README.md) y la [ruta de evaluación](../06-evaluacion-sdd/ruta-sdd-rubrica.md).
