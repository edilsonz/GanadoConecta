# SPEC-06 — Alta masiva de ganado

ACT01 → HU02/HU21 → RF02/RF20 (alta) → AC04/AC05, ADR-04/ADR-13.

Propietario y editor preparan de 1 a 20 filas en una tabla editable. Cada fila admite sexo obligatorio, categoría, nombre, aretes y datos complementarios opcionales del contrato individual. La vista previa muestra errores de las comprobaciones locales y pide confirmar antes de escribir.

Decisión técnica. El lote online es una secuencia de altas individuales independientes; no hay transacción global. El servidor vuelve a validar sesión, ámbito, rol y campos en cada fila. Se informa qué filas confirmó y cuáles requieren revisión. Una fila inválida no impide otras válidas. Si no hay confirmación o se pierde autorización, se detiene el envío y se conservan las filas no confirmadas en memoria con su payload e IDs, para reintentar en el mismo contexto de sesión. Un conflicto no se reintenta automáticamente. Los nombres o aretes no sustituyen al UUID.

| Criterio | Resultado |
|---|---|
| LT01 | La vista previa es obligatoria y presenta validación por fila y límite visible. |
| LT02 | Se confirman individualmente las filas válidas y se informan las inválidas. |
| LT03 | Tras un error incierto, el reenvío mantiene los UUID y no repite filas confirmadas. |
| LT04 | El servidor sigue aplicando aislamiento, permisos y validaciones de SPEC-01. |

Sin archivos Excel, endpoints adicionales ni cola offline del lote. Los borradores se conservan mientras la sección permanece abierta y pertenecen a esa cuenta y ganadería.
