# Consolidación arquitectónica y trazabilidad

**Fecha:** 15 de noviembre de 2026. **Estado:** consolidación documental para
revisión, sin código ni pruebas funcionales ejecutadas. La autoridad para integrar
las decisiones es la lista de 16 decisiones aprobadas por el responsable del
proyecto en esta tarea; no equivale a aprobación docente ni aprobación de todas
las propuestas del paquete v2.

## Fuentes y precedencia

- [Requisitos](../01-analisis-de-sistema/03-requisitos-funcionales.md),
  [historias](../01-analisis-de-sistema/02-historias-de-usuario.md),
  [atributos](../01-analisis-de-sistema/04-atributos-de-calidad.md),
  [drivers](../01-analisis-de-sistema/06-drivers-arquitectonicos.md) y
  [ADR](../01-analisis-de-sistema/07-decisiones-arquitectonicas.md) constituyen
  la base consolidada junto con las vistas C4 y el diseño interno.
- RF01–RF16, HU01–HU17, AC01–AC08, RC01–RC09, DA01–DA08 y ADR-01–ADR-11
  conservan sus identificadores. Se precisan RF02, RF07, AC04, RC01 y RC06;
  se añaden RF17–RF22, HU18–HU23, AC09, DA09, ADR-12–ADR-15 y EC11–EC13.
- Los documentos v2 preservan el detalle original como propuesta. Solo queda
  aprobado lo recogido en la tabla siguiente; las SPEC siguen siendo borradores.
- El prompt de validación es una guía de trabajo, no una autorización para
  integrar todos sus detalles o empezar a programar.

## Correspondencia de las 16 decisiones aprobadas

| N.º | Decisión | RF / HU | AC / driver | ADR | Arquitectura y verificación prevista |
|---|---|---|---|---|---|
| 1 | React responsivo, PWA y Offline-First | RF17, HU18 | AC08, AC09; DA07, DA09 | ADR-02, ADR-12 | C4-2 cliente y sincronización; E2E móvil/escritorio y desconexión. |
| 2 | Spring Boot monolito modular y Clean Architecture | RF01–RF22 | AC06; DA06 | ADR-01, ADR-03 | C4-3 y diseño interno; revisión de dependencias y unitarias del núcleo. |
| 3 | PostgreSQL, Redis público y FastAPI independiente | RF08, RF13, RF19 | AC01, AC02, AC07; DA03, DA05 | ADR-04, ADR-05, ADR-11 | C4-2; integración de adaptadores y fallos; carga del catálogo. |
| 4 | Cuentas individuales y acceso familiar | RF01, RF21, HU01, HU22 | AC04; DA01 | ADR-07, ADR-13 | Identidad/membresía; integración de acceso válido, ajeno y revocado. |
| 5 | Ganado vacuno, registro individual y masivo | RF02, RF20, HU02, HU21 | AC05, AC08; DA02, DA05 | ADR-01, ADR-04 | Animales; validaciones y pruebas de lotes según SPEC. |
| 6 | Identificador único y datos opcionales | RF02, HU02, HU21 | AC05; DA02 | ADR-04 | Dominio Animal; unitarias de identidad y entradas opcionales. |
| 7 | Seis categorías principales | RF02, HU02, HU21 | AC05, AC08; DA02 | ADR-04 | Animales/UI; categorías ternera, vaquilla, vaca, ternero, torete y toro. Reglas de clasificación pendientes. |
| 8 | Nacimientos, parentescos, genealogía y antecedentes | RF03, RF05, RF18, HU03, HU05, HU19 | AC04, AC05; DA01, DA02 | ADR-04, ADR-14 | Animales y Seguimiento; asociación y privacidad. |
| 9 | Sanidad individual y campañas colectivas | RF04, RF22, HU04, HU23 | AC05; DA02 | ADR-01, ADR-04 | Seguimiento; integración de campañas e historial por animal. |
| 10 | Recordatorios individuales/colectivos sin IA | RF07, HU07 | AC02, AC07; DA04 | ADR-09 | Calendario y tareas; pruebas con IA no disponible. |
| 11 | Análisis y conversación autorizada con fuentes | RF08, RF19, HU08, HU20 | AC04, AC07; DA01, DA03 | ADR-05, ADR-06, ADR-15 | Análisis/FastAPI; EC13 y fallos controlados, sin diagnosticar ni prescribir. |
| 12 | Catálogo no transaccional y genealogía autorizada | RF12–RF14, RF18, HU12–HU14, HU19 | AC04; DA01, DA08 | ADR-08, ADR-11, ADR-14 | Anuncios; proyección pública y revocación de visibilidad. |
| 13 | Venta y cierre sin transferencia | RF15, HU15 | AC05; DA02, DA08 | ADR-04, ADR-08, ADR-14 | Transacción Animal/Anuncios; EC05 y conservación de historial. |
| 14 | Fotografías mediante adaptador | RF02, RF12, HU02, HU12 | AC06, AC07; DA05, DA06 | ADR-06, ADR-10 | Infraestructura; integración de permisos y errores, sin nuevo microservicio. |
| 15 | IndexedDB, permisos, versiones y duplicados | RF17, RF21, HU18, HU22 | AC04, AC05, AC09; DA09 | ADR-12, ADR-13 | API/casos de uso y almacenamiento local; EC11–EC12. |
| 16 | Español, lenguaje sencillo y «Mi ganado» | RF02, RF20, HU02, HU21 | AC08; DA07 | ADR-02 | Cliente React; evaluación de tareas y revisión responsiva. |

Los nombres y reglas precisas de las pruebas se concretarán en las SPEC.
La tabla describe pruebas previstas, no resultados.

## Contradicciones resueltas y vacíos identificados

| Hallazgo | Resolución documental | Límite pendiente |
|---|---|---|
| IA restringida solo a registros históricos frente a conversación aprobada. | RC06, RF19 y ADR-15 amplían el alcance; se mantienen privacidad y prohibición de diagnóstico/prescripción. | Modelo, corpus, criterios de fuentes y contratos. |
| Registros asociados exclusivamente a un productor frente a acceso familiar. | RF21/ADR-13 y diseño interno establecen pertenencia a ganadería y autorización individual. | Roles, invitación, revocación, cambio de titular y política local. |
| PWA/offline sin requisito formal ni medida de calidad. | RF17, AC09, DA09, ADR-12, EC11–EC12 y C4 textual. | Flujos offline habilitados, retención y resolución de conflictos. |
| Genealogía propuesta sin trazabilidad ni política pública. | RF18/ADR-14; proyección pública solo autorizada, historial completo privado. | Profundidad, vínculos externos y validaciones genealógicas. |
| Registro masivo y campañas ausentes en requisitos originales. | RF20/RF22, HU21/HU23 y responsabilidades de módulos. | Formatos, límites, atomicidad de lotes y confirmación por animal. |
| Catálogo «no transaccional» confundible con ausencia de transacciones de BD. | Sin pagos/negociación interna; la venta conserva su transacción en PostgreSQL. | Estados finales y reglas de concurrencia de venta. |
| «Exactamente una vez» confundible con garantía del transporte. | Se exige único efecto ante reintentos mediante idempotencia, no una garantía de red. | Claves, caducidad y respuesta a mismo identificador con otro contenido. |
| Recordatorios confundibles con alertas generadas por IA. | RF07 opera con programación independiente; RF09 sigue admitiendo alertas de registros/tendencias. | Anticipación, zona horaria y entrega externa. |
| RF20 en SPEC-01 podría implicar que el primer flujo implementa lotes. | SPEC-01 conserva RF20 como contexto; cobertura parcial de alta individual, no masiva. | SPEC independiente de registro masivo. |
| Autenticación obligatoria para reportar aparece firme en Actores/HU pero propuesta en RF16. | Se explicita como propuesta en las tres fuentes; no se aprueba silenciosamente. | Confirmar en SPEC de moderación. |
| PNG sin fuentes y nombres distintos de módulos en diseño/C4. | C4 describe ampliaciones y estado gráfico; diseño explica agrupaciones lógicas. | Recuperar o crear fuentes editables revisadas antes de regenerar PNG. |

## Detalles que continúan como propuestas

La lista aprobada no fija UUID, sexo obligatorio, editor/lector con permisos
concretos, CSV/Excel, lotes de 50+, ascendencia hasta abuelos, categorías
inferidas por edad/parto, colores exactos, endpoints, biblioteca IndexedDB,
JWT offline, algoritmo de conflictos ni habilitación de ventas offline.
Estos detalles de v2 se conservan para evaluar en las SPEC; no se promueven
por estar escritos en un borrador o prompt.

Las fuentes editables C4 no se encontraron en los archivos del repositorio:
solo hay los PNG de niveles 1–3 en img/. Draw.io figura como herramienta,
pero no hay archivos .drawio asociados. No se inspeccionaron repositorios
externos ni se afirma que las fuentes no existan fuera de este repositorio.

## Pendientes antes de implementar el flujo afectado

| Pendiente | Documento responsable | Bloqueo |
|---|---|---|
| Roles, membresías, cuenta y revocación | SPEC de acceso por preparar | Autorización y aislamiento familiar. |
| Identidad técnica, validaciones, versiones, deduplicación y conflictos | SPEC-01/plan.md | Registro y sincronización. |
| Datos locales permitidos, cierre de sesión, dispositivos compartidos, limpieza, fotos y pérdida de almacenamiento | SPEC-01 y seguridad | Persistencia local privada; no se promete revocación inmediata sin red. |
| Estados, unicidad de arete, categorías y reglas genealógicas | SPEC de animales/genealogía | Validación de datos y publicación autorizada. |
| Formato/límites/errores de lote y confirmación de campañas | SPEC de registro masivo y sanidad | Operaciones colectivas. |
| TTL, claves, invalidación reintentable y protección ante revocación | SPEC de catálogo | No exponer datos retirados desde caché. |
| Modelo, fuentes/corpus, privacidad, retención, errores y respuesta conversacional | SPEC de IA | Integración FastAPI; no se elige proveedor. |
| Fotografías, notificaciones, infraestructura, versiones y herramientas de prueba | Plan técnico de cada integración | Implementación/despliegue correspondiente. |
| Fuentes editables de diagramas y revisión visual | docs/04-modelo-c4 | Regeneración de imágenes y cierre gráfico del criterio 3. |

La ausencia de un pendiente no autoriza a implementar fuera de la SPEC.
Los criterios de evaluación se relacionan en la
[matriz de los 16 criterios](../06-evaluacion-sdd/matriz-trazabilidad.md).
