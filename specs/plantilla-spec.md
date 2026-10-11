# SPEC-XX: Nombre de la funcionalidad

## 1. Identificación

| Campo | Valor |
|---|---|
| Estado | Borrador |
| Requisitos relacionados | RFXX |
| Historias de usuario relacionadas | HUXX |
| Atributos de calidad relacionados | ACXX |

## 2. Objetivo

Describir el resultado que la funcionalidad permite obtener
y la necesidad que resuelve.

## 3. Actores y permisos

| Actor | Acciones permitidas | Restricciones |
|---|---|---|
| Actor correspondiente | Acciones autorizadas | Límites de acceso |

Los permisos deben verificarse en el backend.
Cuando corresponda, se comprobará la propiedad de los registros.

## 4. Alcance

### Incluye

- Comportamientos que cubre esta especificación.

### Fuera del alcance

- Comportamientos que corresponden a otras especificaciones.

## 5. Precondiciones

- Condiciones necesarias para iniciar la operación.
- Estado requerido de los datos.
- Autenticación y autorización, cuando correspondan.

## 6. Flujo principal

1. El actor inicia la operación.
2. El sistema recibe y valida los datos.
3. El sistema verifica los permisos y las reglas de negocio.
4. El sistema ejecuta la operación.
5. El sistema devuelve el resultado.

Adaptar estos pasos al comportamiento concreto de la funcionalidad.

## 7. Flujos alternativos y errores

| ID | Situación | Respuesta esperada |
|---|---|---|
| FA01 | Datos inválidos | Informar los campos que deben corregirse sin guardar cambios. |
| FA02 | Acceso no autorizado | Rechazar la operación sin revelar información privada. |
| FA03 | Registro no disponible | Informar el resultado según las reglas de acceso. |
| FA04 | Fallo de una dependencia | Aplicar la recuperación definida para la funcionalidad. |

Conservar solo los casos aplicables y agregar los necesarios.

## 8. Reglas de negocio

| ID | Regla |
|---|---|
| RN01 | Describir una condición obligatoria y verificable. |
| RN02 | Describir otra condición de la funcionalidad. |

Incluir, cuando corresponda:

- Propiedad y visibilidad de los datos.
- Estados y transiciones permitidas.
- Prevención de duplicados.
- Comportamiento ante operaciones concurrentes.
- Cambios que deben confirmarse en una misma transacción.

## 9. Datos de entrada y salida

### Entrada

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| Nombre del campo | Tipo de dato | Sí/No | Condición que debe cumplir |

### Salida

Describir los datos devueltos, el estado resultante
y la información que debe mostrarse al usuario.

Excluir de las respuestas los datos privados que el actor
no tenga permiso para consultar.

## 10. Persistencia e integraciones

Describir únicamente las dependencias que utiliza la funcionalidad.

- PostgreSQL: datos persistidos y límites de la transacción.
- Redis: consultas almacenadas, claves, TTL e invalidación.
- Servicio de IA: datos enviados, resultado esperado y manejo de errores.
- Notificaciones: evento que las genera y tratamiento de fallos.
- Fotografías: almacenamiento y permisos de acceso.

Si se utiliza Redis para el catálogo:

- PostgreSQL conserva los datos permanentes.
- Una ausencia o un fallo de caché permite consultar PostgreSQL.
- La invalidación se realiza después de confirmar la transacción.
- Las invalidaciones fallidas se registran y reintentan.
- Se define cómo evitar mostrar anuncios cuya visibilidad fue revocada.
- No se almacenan historiales privados, credenciales ni archivos de fotos.

## 11. Criterios de aceptación

### CA01: Operación exitosa

**Dado** un actor autorizado y las precondiciones cumplidas,
**cuando** realiza la operación con datos válidos,
**entonces** obtiene el resultado definido y los cambios correspondientes.

### CA02: Datos inválidos

**Dado** un conjunto de datos que incumple una validación,
**cuando** el actor intenta realizar la operación,
**entonces** el sistema informa el error y no guarda cambios.

### CA03: Acceso no autorizado

**Dado** un actor sin permiso para la operación,
**cuando** intenta ejecutarla,
**entonces** el sistema rechaza el acceso y protege los datos.

Adaptar los criterios y agregar casos de errores, concurrencia
o recuperación cuando sean relevantes.

## 12. Pruebas

| ID | Tipo | Escenario | Resultado esperado | Criterio relacionado |
|---|---|---|---|---|
| PU01 | Unitaria | Regla de negocio específica | Resultado verificable | CA correspondiente |
| PI01 | Integración | Colaboración entre componentes | Resultado verificable | CA correspondiente |
| PC01 | Carga | Operación con solicitudes concurrentes | Objetivo medible | AC o EC correspondiente |

El proyecto debe incluir los tres tipos de pruebas.
Cada especificación identifica cuáles le corresponden;
si un tipo no aplica, se justifica.

Las pruebas de carga deben indicar:

- Operación evaluada.
- Cantidad de usuarios concurrentes.
- Conjunto de datos.
- Duración y distribución de solicitudes.
- Estado de la caché, cuando corresponda.
- Objetivo de latencia y tasa de errores.

## 13. Decisiones pendientes

| ID | Decisión pendiente | Impacto |
|---|---|---|
| DP01 | Aspecto que requiere definición | Qué impide diseñar, implementar o verificar |

Las decisiones que bloqueen la implementación deben resolverse
antes de marcar la especificación como revisada.

## 14. Evidencia de validación

| Prueba | Fecha | Entorno | Resultado | Evidencia |
|---|---|---|---|---|
| Pendiente | — | — | No ejecutada | — |

## 15. Condición de finalización

La funcionalidad se considera completada cuando:

- La especificación está revisada.
- El comportamiento está implementado.
- Los criterios de aceptación se cumplen.
- Las pruebas correspondientes fueron ejecutadas y aprobadas.
- La evidencia y la documentación están actualizadas.