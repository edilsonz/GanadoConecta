# Alcance y acuerdos de GanadoConecta — propuesta v2

> **Importante:** las siguientes decisiones recogen la conversación de definición funcional y deben contrastarse formalmente con la documentación aprobada por el docente. Se conservan los requisitos existentes RF01–RF16 como punto de partida.

## Visión del producto

GanadoConecta es una **aplicación web responsiva** para computadora y celular, orientada al manejo del **ganado vacuno**. Su propósito principal es facilitar registros, seguimiento sanitario/productivo/reproductivo, campañas colectivas, calendario y consultas de la información. Incorpora análisis inteligente y un asistente con dos modos: consultas sobre datos del productor y explicaciones generales con fuentes confiables. De forma complementaria, ofrece **catálogo digital no transaccional de ganado en venta**.

## Acuerdos funcionales

1. Interfaz en español de fácil lectura; usar «Mi ganado», «Todos mis animales» y «Grupo de ganado» en vez de «hato» en menús y botones.
2. Acceso por cuentas individuales. Un propietario puede autorizar a familiares como editores o lectores de una misma **ganadería**. El administrador del sistema no tiene acceso indiscriminado a historiales privados.
3. Registro flexible de vacunos: `sexo` es obligatorio; apodo/nombre, inscripción del arete (texto o números), raza, nacimiento estimado y fotografía son opcionales al crear. Identificador técnico UUID inmutable.
4. Categorías visibles: **ternera → vaquilla → vaca**; **ternero → torete → toro**. No inferir una categoría solo por edad; la primera parición y el papel reproductivo importan. El productor puede indicar una categoría y el sistema advertir incoherencias. Nuevas categorías pueden añadirse después.
5. Crear un nacimiento vinculando madre y padre opcional, incluso si son referencias externas; registrar cada cría separadamente y admitir partos múltiples.
6. Genealogía visible en ficha privada; hasta abuelos si la información está disponible. Sin datos, mostrar «No registrado». Distinguir declaración del productor de certificación documental.
7. Inscripción por lote o importación CSV y registro individual. La fotografía no es obligatoria. Soportar registrar 50+ animales sin repetir información compartida.
8. Actividades sanitarias **colectivas** (vacunas, desparasitaciones, baños de inmersión u otras formas de control) e **individuales** cuando corresponda. Formulario rápido con detalles opcionales: producto, dosis realmente aplicada, vía, responsable, siguiente fecha, observaciones y período de retiro cuando corresponda. Confirmar animales realmente atendidos; no registrar tratamientos no realizados.
9. Recordatorios para todos los animales, grupos seleccionados o uno específico. Se basan en fechas programadas por el productor o en protocolos previamente validados, nunca en dosis o calendarios inventados por IA.
10. Seguimiento de peso, tendencias y análisis general del ganado; se pueden destacar animales cuyo registro amerite revisión sin diagnosticar enfermedades.
11. IA = a) análisis de datos autorizados del productor y b) asistente conversacional con información ganadera general respaldada por fuentes trazables y revisadas. No diagnostica ni receta.
12. Catálogo **no transaccional** de animales, con fotos, características, ubicación general, precio solicitado y contacto aprobado por el productor. Genealogía pública **solo cuando sea autorizada** explícitamente; por defecto oculta. Nunca publicar historiales completos ni análisis privados de IA.
13. Venta: el propietario marca vendido, se cierra el anuncio dentro de la misma transacción en PostgreSQL y conserva su historial. **Sin transferir fichas entre cuentas** en la primera versión.
14. Web PWA con capacidades Offline-First: visualizar datos descargados y guardar registros en IndexedDB para sincronizar después. Sin acceso inicial a internet no se garantiza disponer de la app/datos. Notificaciones puntuales con la web cerrada y sin señal **no están garantizadas**.

## Aclaraciones técnicas cerradas por recomendación (revisables)

- El lenguaje de la interfaz es español; colores base verde, blanco y grises neutros.
- PostgreSQL es la fuente central de verdad; Redis solo guarda caché del catálogo público.
- El servicio de IA usa FastAPI independiente del backend principal, con datos mínimos, permisos y ejecución supervisada.
- Fotografías mediante puerto/adaptador del backend y almacenamiento de objetos o archivos; sin microservicio de imágenes.
- Roles iniciales en una ganadería: propietario, editor familiar, lector familiar. Solo propietario publica/cierra ventas e invita/elimina integrantes en la primera versión.
- Datos básicos y controles tienen prioridad sobre IA y catálogo; implementación incremental.

## Propuestas de actualización de requisitos

| Identificador propuesto | Cambio | Relación existente |
|---|---|---|
| RF17 | Consulta y registro offline de datos autorizados y sincronización posterior | RC01, RF02, RF04, AC02 |
| RF18 | Genealogía declarada, padres, abuelos y origen de vínculos | RF02, RF05, RF12 |
| RF19 | Asistente ganadero con información propia autorizada y conocimiento general respaldado | RF08, RC06 |
| RF20 | Registro rápido e importación masiva de vacunos | RF02, AC08 |
| RF21 | Acceso de familiares autorizados a la misma ganadería | RF01, ADR-07 |
| RF22 | Campañas sanitarias con confirmación por animal | RF03, RF04, RF07 |
| AC09 | Continuidad offline y sincronización segura bajo escenarios medibles | AC02, AC04, AC05 |
| ADR-12 | PWA Offline-First con IndexedDB, cola de cambios y reconciliación | ADR-02, ADR-04 |
| ADR-13 | Colaboración por ganadería y autorización por membresía | ADR-07 |
| ADR-14 | Genealogía privada y proyección pública autorizada | ADR-08 |
| ADR-15 | Asistente de IA con herramientas de lectura autorizadas y fuentes verificables | ADR-05, ADR-06 |

**Estos códigos no están incorporados aún a los documentos originales**. Codex deberá proponer una modificación explícita de RF/AC/ADR con trazabilidad, sin cambios silenciosos.

## Fuera del alcance inicial

Pagos en línea, carrito, comisiones, transporte, chat comprador-vendedor, transferencia automática de propiedad, diagnósticos IA, prescripción veterinaria, lectura OCR de arete como función obligatoria y notificaciones offline garantizadas con el navegador cerrado.
