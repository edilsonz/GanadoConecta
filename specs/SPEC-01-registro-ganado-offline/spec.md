# SPEC-01: Registro flexible de ganado y sincronización (borrador propuesto)

> **Borrador pendiente de revisión:** el alcance general está aprobado, pero
> las reglas detalladas siguientes siguen siendo propuestas. Consultar la
> sección 12 y la [consolidación arquitectónica](../../docs/05-preparacion-implementacion/05-consolidacion-arquitectonica.md)
> antes de diseñar o implementar. Ningún criterio se declara verificado.

## 1. Identificación

| Campo | Valor |
|---|---|
| Estado | **Borrador — no implementado** |
| Requisitos | RF02, RF17, RF21; RF20 como contexto (cobertura parcial: alta individual, no masiva) |
| Calidad | AC04, AC05, AC08, AC09 |

## 2. Objetivo

Permitir a un productor o familiar editor registrar un vacuno con pocos datos, incluso sin conexión, y sincronizar el registro una sola vez, manteniendo los límites de acceso a su ganadería.

## 3. Actores y permisos

- Propietario: crear y editar vacunos de su ganadería.
- Familiar editor: crear y editar vacunos autorizados de su ganadería.
- Familiar lector: consultar, sin crear ni editar.
- Administrador plataforma: no accede indiscriminadamente a fichas privadas.

## 4. Entrada

- Obligatorios: sexo (`HEMBRA`/`MACHO`), ganadería seleccionada, UUID técnico de animal, `operationId` de la escritura.
- Opcionales: nombre, texto de arete frontal/reverso, raza, fecha estimada nacimiento, categoría, foto, observaciones.
- Validaciones: no inventar categorías sin edad/criterios; normalizar y controlar caracteres; advertir posible duplicado de arete; nunca confundir marca familiar con identificador individual.

## 5. Flujo online

1. Usuario autorizado abre «Mi ganado» → «Registrar animal».
2. Selecciona sexo y agrega datos opcionales.
3. Cliente crea UUID; valida campos y envía operación con identificador único.
4. Servidor valida JWT, membresía y permisos. Persiste animal con ganadería y versión inicial.
5. Devuelve datos y estado `sincronizado`.

## 6. Flujo offline

1. La PWA está previamente preparada y el usuario dispone de datos locales autorizados.
2. Cliente crea UUID de animal y `operationId`, guarda operación en IndexedDB y enseña «Guardado en este dispositivo · pendiente de sincronización».
3. La cola reintenta al recuperar red o pulsar «Sincronizar ahora».
4. Backend vuelve a validar membresía, permisos, esquema y deduplica `operationId`.
5. Se sincroniza un solo animal y se actualiza el estado local. Si hay error/conflicto, muestra detalle y mantiene operación hasta revisión.

## 7. Reglas

- No usar nombre ni arete como PK global.
- No exponer información de ganadería ajena en ningún endpoint.
- No marcar como confirmado algo solo guardado localmente.
- Si permiso fue revocado antes de sincronizar, rechazar y mostrar aviso; definir exportación/limpieza segura de datos locales.
- En altas duplicadas por reenvío idéntico de `operationId`, devolver mismo resultado o confirmación coherente.
- Sin foto también se crea registro completo para etapa inicial.
- La página funciona sin red si se preparó al menos una vez; no garantizar persistencia permanente del navegador.

## 8. Errores

- Datos inválidos: no se guarda operación inválida.
- Membresía revocada: servidor rechaza; no exponer datos privados.
- Conflicto de versión: no sobreescribir; conservar datos pendientes.
- Caída del backend/red: conservar en cola, reintentar con límites y retroceso.
- Espacio local insuficiente: indicar error y ofrecer acciones, no simular guardado.

## 9. Criterios de aceptación

- CA01: Crear un animal indicando solo sexo y UUID técnico -> persiste con estado activo.
- CA02: Acepta nombres/aretes alfanuméricos y ausencia de ambos.
- CA03: Editor autorizado puede crear; lector y usuario de otra ganadería no.
- CA04: Operación offline se ve pendiente, posteriormente sincronizada sin duplicados.
- CA05: Reenvío de operación misma `operationId` es idempotente.
- CA06: Revocación de permisos antes de sincronizar provoca rechazo seguro.
- CA07: No se muestran mensajes de éxito remoto antes de confirmación del backend.
- CA08: El código de categoría y sus etiquetas en UI no infieren automáticamente parición o reproductor solo por edad.

## 10. Pruebas

- Unitarias: validaciones de animal, reglas de campos opcionales, permisos de uso.
- Integración backend: membresía/tenencia, idempotencia, UUID, aislamiento, error por permiso revocado.
- E2E navegador: instalar/preparar PWA, desconectar, crear, reconectar, comprobar estados y registro único; simular conflicto.
- Carga: consultas de animales de AC01 cuando el módulo sea funcional, registrar entorno y percentiles.

## 11. Aspectos técnicos abiertos

Versión inicial de dependencias, esquema exacto de eventos de sincronización, política de borrado local, elección de biblioteca IndexedDB y tratamiento de fotos offline. Deben definirse antes de implementar esos subflujos, evitando inventar proveedores.

## 12. Estado tras consolidar la arquitectura

RF17–RF22 y AC09 están integrados en la documentación base. Esta SPEC sigue
siendo borrador: sexo obligatorio, UUID, permisos de editor/lector, generación
de identificadores y reglas de categorías son propuestas por revisar. El alcance
aprobado requiere identidad interna única y permite nombre, arete y datos
adicionales opcionales. Los criterios CA01–CA08 no se consideran aprobados
ni verificados por esta consolidación. El registro masivo RF20 requiere su
propia especificación; aquí solo se aborda el alta individual.

Referencias: HU02/HU18/HU22; DA01/DA02/DA09; ADR-04/ADR-07/ADR-12/ADR-13;
EC11–EC12. El plan y las tareas permanecen pendientes, sin autorizar implementación.
