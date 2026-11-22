# Matriz funcional de GanadoConecta

Esta matriz referencia los catálogos originales de [22 requisitos](01-analisis-de-sistema/03-requisitos-funcionales.md), [23 historias](01-analisis-de-sistema/02-historias-de-usuario.md) y [tres actores humanos](01-analisis-de-sistema/01-actores.md). ACT01 es productor e integrante autorizado según rol; ACT02, visitante o interesado; ACT03, administrador. EXT01 sigue siendo un sistema externo. «Evidencia» enlaza a las tareas de la SPEC, con enlaces al contrato y al plan en el [índice SDD](../specs/README.md).

La [consolidación de la interfaz](05-preparacion-implementacion/36-consolidacion-ux.md) aporta evidencia transversal de navegación para los flujos privados y el catálogo. Los contratos de la matriz conservan su alcance.

La [auditoría funcional](AUDITORIA-FUNCIONAL.md) complementa esta matriz con el
estado de cada RF y HU, evidencias ejecutadas y brechas observadas.

| RF | HU asociadas | Actores | Evidencia o alcance pendiente |
|---|---|---|---|
| RF01 | HU01, HU22 | ACT01 | [SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md) y [SPEC-04](../specs/SPEC-04-acceso-familiar/tasks.md). Perfil pendiente. |
| RF02 | HU02, HU21 | ACT01 | [SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md), [SPEC-06](../specs/SPEC-06-alta-masiva/tasks.md). Edición pendiente. |
| RF03 | HU03 | ACT01 | [Sanidad](../specs/SPEC-02-seguimiento-sanitario/tasks.md), [reproducción](../specs/SPEC-03-reproduccion-partos-genealogia/tasks.md), [peso](../specs/SPEC-05-historial-peso/tasks.md). Cobertura productiva parcial. |
| RF04 | HU04, HU23 | ACT01 | [SPEC-02](../specs/SPEC-02-seguimiento-sanitario/tasks.md) y [SPEC-07](../specs/SPEC-07-calendario-campanas/tasks.md). Baño individual en interfaz pendiente. |
| RF05 | HU05, HU19 | ACT01 | [SPEC-03](../specs/SPEC-03-reproduccion-partos-genealogia/tasks.md). Cálculo automático de parto pendiente. |
| RF06 | HU06 | ACT01 | [SPEC-05](../specs/SPEC-05-historial-peso/tasks.md). Otros indicadores pendientes. |
| RF07 | HU07 | ACT01 | [SPEC-07](../specs/SPEC-07-calendario-campanas/tasks.md). Calendario y avisos en pantalla; entrega externa pendiente. |
| RF08 | HU08, HU20 | ACT01 | [SPEC-12](../specs/SPEC-12-analisis-alertas-asistente/tasks.md). Análisis descriptivo de registros confirmados, con procedencia y fechas. Inferencia mediante modelo pendiente. |
| RF09 | HU09 | ACT01 | [SPEC-12](../specs/SPEC-12-analisis-alertas-asistente/tasks.md). Avisos internos de hoy y vencidos a partir de la agenda. Criterios por tendencias pendientes. |
| RF10 | HU10 | ACT01 | [SPEC-11](../specs/SPEC-11-reportes-ganaderos/tasks.md). Reportes sanitarios, reproductivos y de peso por ganadería con autorización y fechas inclusivas. |
| RF11 | HU11 | ACT03 | [SPEC-10](../specs/SPEC-10-administracion-usuarios/tasks.md). Consultas administrativas, estadísticas y auditoría. Acciones sobre cuentas pendientes de aprobación. |
| RF12 | HU12 | ACT01 | [SPEC-08](../specs/SPEC-08-catalogo-anuncios/tasks.md). Almacenamiento local de desarrollo. |
| RF13 | HU13 | ACT02 | [SPEC-08](../specs/SPEC-08-catalogo-anuncios/tasks.md). Filtros y paginación públicos. |
| RF14 | HU14 | ACT01 | [SPEC-08](../specs/SPEC-08-catalogo-anuncios/tasks.md). Publicar, pausar y retirar. |
| RF15 | HU15 | ACT01 | [SPEC-09](../specs/SPEC-09-ventas-moderacion/tasks.md). Venta y cierre transaccionales online. |
| RF16 | HU16, HU17 | ACT01, ACT02, ACT03 | [SPEC-09](../specs/SPEC-09-ventas-moderacion/tasks.md). Recepción deshabilitada mientras se aprueba la política; revisión autorizada y retiro auditado. |
| RF17 | HU18 | ACT01 | [SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md), [sincronización sanitaria](05-preparacion-implementacion/32-sincronizacion-sanitaria-offline.md). Alcance offline parcial. |
| RF18 | HU19, HU12 | ACT01, ACT02 | [SPEC-03](../specs/SPEC-03-reproduccion-partos-genealogia/tasks.md) y [SPEC-08](../specs/SPEC-08-catalogo-anuncios/tasks.md). Proyección pública limitada y optativa. |
| RF19 | HU20 | ACT01 | [SPEC-12](../specs/SPEC-12-analisis-alertas-asistente/tasks.md#fase-2--validación-local-de-rf19). Asistente local con Qwen3, registros propios y citas verificados en Chrome. Corpus de orientación general pendiente. |
| RF20 | HU21 | ACT01 | [SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md) y [SPEC-06](../specs/SPEC-06-alta-masiva/tasks.md). |
| RF21 | HU22 | ACT01 | [SPEC-04](../specs/SPEC-04-acceso-familiar/tasks.md) y [SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md). Acceso vigente en servidor y al sincronizar. |
| RF22 | HU23 | ACT01 | [SPEC-02](../specs/SPEC-02-seguimiento-sanitario/tasks.md) y [SPEC-07](../specs/SPEC-07-calendario-campanas/tasks.md). |
