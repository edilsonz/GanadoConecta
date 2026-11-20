# SPEC-03 — Reproducción, partos y genealogía

**Estado:** Aprobada para implementación por solicitud del 20-11-2026.

## Objetivo y alcance

Registrar hechos reproductivos informados por el productor, partos y parentescos privados de animales de una misma ganadería. No se infieren diagnósticos, períodos de gestación ni fechas clínicas. La observación libre preexistente de un animal no crea hechos reproductivos.

## Actores y permisos

- PROPIETARIO y EDITOR activos pueden registrar y corregir.
- LECTOR activo puede consultar.
- Cada operación revalida sesión y membresía en PostgreSQL; no se aceptan identificadores de usuario del cliente.

## Contratos mínimos

### Eventos reproductivos

- Tipos: `MONTA`, `INSEMINACION`, `EVALUACION_GESTACION`.
- Solo se registran para una hembra de la misma ganadería.
- Fecha obligatoria no futura; responsable, observaciones y reproductor registrado son opcionales.
- `estadoGestacion` solo aplica a evaluación: `CONFIRMADA`, `NO_CONFIRMADA` o `INDETERMINADA`.
- `fechaProbableParto` es opcional y siempre indicada por el usuario; nunca se calcula.
- UUIDv4 de evento y operación; reintentos conservan ambos y son idempotentes.

### Partos

- Madre hembra, fecha no futura, resultado informativo `CON_CRIAS_REGISTRADAS` o `SIN_CRIAS_REGISTRADAS`, observaciones y padre conocido opcionales.
- Un parto admite cero, una o varias crías ya registradas en la misma ganadería. Una cría nueva se registra primero con el flujo vigente de animales y luego se vincula sin crear otra identidad.
- No se modifican automáticamente la categoría, el estado ni las observaciones de animales.

### Genealogía

- Madre y padre conocidos son animales registrados de la misma ganadería; la ausencia de relación significa «no registrado».
- Madre debe ser hembra y padre macho. Se impiden autorrelaciones, ciclos y dos progenitores del mismo tipo.
- Los partos originan la relación madre-cría y, si se informó, padre-cría.
- Una corrección sustituye o retira la relación vigente y conserva auditoría de valor anterior, nuevo valor, actor, operación y fecha.

## Criterios de aceptación

| ID | Criterio |
|---|---|
| RP01 | Propietario/editor registra y reintenta un evento reproductivo sin duplicarlo; lector no escribe. |
| RP02 | Solo una hembra admite eventos y la evaluación exige un estado válido. |
| RP03 | La fecha probable de parto solo refleja el valor suministrado; observaciones existentes no se interpretan. |
| RP04 | Los tres roles activos consultan el historial aislado por ganadería. |
| PA01 | Un parto idempotente vincula cero o varias crías existentes sin cambiar sus fichas. |
| PA02 | Madre, padre y crías pertenecen a la misma ganadería y cumplen sexo/identidad. |
| PA03 | La ficha de la madre muestra partos y crías vinculadas. |
| GE01 | La consulta muestra madre, padre, ascendencia disponible hasta abuelos y descendencia directa. |
| GE02 | Se rechazan ciclos, autorrelaciones, sexo incompatible y animales de otra ganadería. |
| GE03 | Las correcciones autorizadas conservan trazabilidad y son idempotentes. |
| SE01 | Lecturas y escrituras revalidan membresía; datos ajenos o revocados no se revelan. |

## Fuera de alcance

Predicciones, reproducción offline, referencias externas, certificación genealógica pública y reglas veterinarias automáticas.
