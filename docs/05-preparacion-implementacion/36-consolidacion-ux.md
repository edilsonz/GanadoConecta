# Consolidación de la navegación y la interfaz

El espacio del productor conserva la ganadería activa en memoria. Inicio, Mi ganado, Sanidad, Reproducción, Producción, Calendario, Anuncios y Acceso familiar se consultan por separado. La ficha reúne General, Sanidad, Reproducción, Peso, Genealogía y Anuncios. El catálogo tiene su propia vista pública y la bandeja de moderación aparece tras una consulta autorizada al servidor.

El cambio reutiliza la verificación por ganadería, los formularios y el sincronizador existente. Las pestañas consultan su contenido en la primera visita y lo mantienen montado durante la navegación de la ficha. El formulario individual y el listado masivo comparten las reglas del formulario sin depender del componente de pantalla. La agenda consulta directamente el filtro inicial.

| Alcance | Evidencia ejecutada |
|---|---|
| SPEC-07 a SPEC-09 antes de los cambios | 14 pruebas dirigidas, sin fallos. Se ejecutaron 13 casos HTTP con PostgreSQL Docker en una base aislada y un caso unitario de moderación. Cubrieron calendario, campañas, catálogo, privacidad, venta y autorización de moderación. |
| Limpieza y navegación | 50 pruebas dirigidas de ganado, alta masiva y agenda. 60 de inicio, selección, animales, pestañas, catálogo y moderación. 7 de cuenta y sesión. Cada ejecución finalizó sin fallos. |
| Chrome headless | Recorrido sintético a 360, 768 y 1440 px. Selección de ganadería, seis pestañas de ficha, secciones productivas, calendario, entrada de acceso familiar y catálogo separado. El formulario de alta conservó su borrador al regresar. El ancho de desplazamiento coincidió con el viewport en las tres ejecuciones. |
| Compilación | TypeScript correcto. Vite generó el manifiesto y el service worker PWA. |

El navegador usó respuestas controladas de la API y un perfil aislado. Quedan por ejecutar la prueba manual HTTPS integrada, una revisión con usuarios y las mediciones de carga. Ver [rúbrica](../RUBRICA-DE-EVALUACION.md) y [trazabilidad funcional](../MATRIZ-DE-TRAZABILIDAD.md).
