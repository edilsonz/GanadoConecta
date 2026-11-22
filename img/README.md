# Recursos gráficos de GanadoConecta

## Arquitectura vigente

Consulta el [índice de vistas y fuentes editables](../docs/02-arquitectura-software/diagramas/README.md)
para C4 niveles 1–3, Clean Architecture, modelo de datos, despliegue local
y sincronización. Las fuentes Draw.io y sus SVG se mantienen junto a la
documentación. La trazabilidad de evaluación está en la [rúbrica](../docs/RUBRICA-DE-EVALUACION.md).

## Interfaz actual

La [identidad aprobada](marca/README.md) reúne el logo original, las variantes
de favicon e instalación y la procedencia de la fotografía. Consulta las
[capturas HTTPS de la renovación](../tests/e2e/evidencia/identidad-2026-11-21/README.md).

## Capturas funcionales de referencia

Las capturas de Chrome con datos sintéticos están versionadas en la
[evidencia E2E](../tests/e2e/README.md). Se reutilizan aquí mediante enlaces
a la corrida del 21 de noviembre de 2026.

| Vista | Captura |
|---|---|
| Navegación lateral, calendario y campaña con atención parcial | [Campaña sanitaria](../tests/e2e/evidencia/campana-parcial.png) |
| Mi ganado, ficha por pestañas y sanidad en un viewport móvil | [Ficha móvil](../tests/e2e/evidencia/ficha-movil.png) |
| Anuncios públicos, fotografía, filtros y paginación | [Catálogo público](../tests/e2e/evidencia/catalogo-pagina-2.png) |
| Anuncios de la ficha cerrados tras una venta sintética | [Cierre de anuncios](../tests/e2e/evidencia/anuncios-cerrados.png) |

La navegación y las pestañas tienen cobertura en esa ejecución. Queda
pendiente una serie de capturas específicas del selector de ganadería,
acceso familiar, reproducción, genealogía, peso y registro masivo.
La captura de moderación requiere una cuenta sintética autorizada.

Los [iconos PWA](../frontend/public/icons/) siguen en el frontend con sus
rutas de manifiesto.

## Archivo histórico

Los cinco PNG originales se conservaron sin cambios. La revisión del árbol
y de los PNG encontró las imágenes, con fuente editable original ausente.

- [Arquitectura general](historico/Figura_1_Estilo_GanadoConecta.png).
- [Clean Architecture inicial](historico/Figura_2_Clean_Architecture.png).
- [C4 contexto](historico/GanadoConecta_C4_Nivel_1_Contexto.png).
- [C4 contenedores](historico/GanadoConecta_C4_Nivel_2_Contenedores.png).
- [C4 componentes](historico/GanadoConecta_C4_Nivel_3_Componentes.png).
