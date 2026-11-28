# Rediseño de GanadoConecta

Comparación del 21 de noviembre de 2026 en Chrome headless por HTTPS. El
«antes» usa el build de `fe2d0e7`. El «después» usa el build del rediseño.
Ambas ejecuciones usaron Spring Boot, PostgreSQL 17.12 y cuentas, granjas y
animales sintéticos en una base aislada. Las cifras del panel privado se
conciliaron con registros sintéticos conocidos de esa base.

| Vista | Ancho | Antes | Después |
|---|---:|---|---|
| Portada | 360 px | [Captura](../identidad-2026-11-21/portada-360.png) | [Captura](despues-portada-360.png) |
| Portada | 768 px | [Captura](../identidad-2026-11-21/portada-768.png) | [Captura](despues-portada-768.png) |
| Portada | 1440 px | [Captura](../identidad-2026-11-21/portada-1440.png) | [Captura](despues-portada-1440.png) |
| Panel de ganadería | 360 px | [Captura](antes-panel-360.png) | [Captura](despues-panel-360.png) |
| Panel de ganadería | 768 px | [Captura](antes-panel-768.png) | [Captura](despues-panel-768.png) |
| Panel de ganadería | 1440 px | [Captura](../identidad-2026-11-21/panel-1440.png) | [Captura](despues-panel-1440.png) |

[Ficha de escritorio](ficha-1440.png) · [Ficha y peso en móvil](ficha-360.png) ·
[Agenda](agenda-1440.png) · [Catálogo con foto sintética](catalogo-1440.png) ·
[Familia](familia-1440.png) · [Administración móvil](administracion-360.png) ·
[Reporte de peso filtrado](reportes-768.png) · [Análisis y asistente](asistente-1440.png).

## Distribución y resultados

- La portada reúne el mensaje y la foto en una sola composición y sitúa el
  acceso y el registro en paralelo en escritorio y tablet. En móvil se apilan.
- El menú agrupa trabajo diario, seguimiento y gestión. El resumen usa
  `GET /reportes` para eventos sanitarios, evaluaciones confirmadas y partos de
  los últimos 30 días. `GET /agenda-sanitaria` y `GET /alertas` muestran hasta
  cuatro próximas actividades y cuatro avisos. Los pesajes permanecen
  disponibles en las vistas por animal y reportes. Los errores de consulta
  tienen su propio estado y el resumen requiere acceso vigente.
- En Mi ganado, la ficha se abre junto al listado en escritorio y después del
  listado en móvil. El filtro por sexo y la búsqueda actúan sobre los resultados
  cargados. El teclado puede desplazarse por las pestañas y el foco vuelve a la
  fila al cerrar la ficha. Los historiales, agenda, anuncios, familia,
  administración, reportes y asistente se distribuyen según el ancho real de
  sus tarjetas y formularios.

| Verificación | Resultado ejecutado |
|---|---|
| Frontend dirigido | 102/102 en 18 archivos durante el desarrollo. |
| Regresión final | 251/251 en 39 archivos. TypeScript y build PWA correctos. |
| Chrome en el rediseño | 63 verificaciones de vista y ancho, más seis comprobaciones de tabla de alta masiva y reporte filtrado. Sin desbordamiento horizontal de página a 360, 768 y 1440 px. |
| Accesibilidad básica | Chrome comprobó el enlace de salto, el foco al abrir la ficha, las flechas de pestañas y el desplazamiento interno de la tabla con teclado. Vitest verificó el retorno a la fila al cerrar la ficha. |
| Funcionalidad | Cuatro recorridos aprobados de campañas, catálogo, ventas y navegación. Registro, selección de ganadería y prueba de permisos familiares aprobados. El lector no vio el formulario de alta y el editor lo recuperó tras revalidar. El backend devolvió 403 tras revocar el acceso. |
| Datos y marca | Los indicadores mostraron un registro real de cada tipo en la base aislada. La próxima actividad y el aviso vencido concordaron con la agenda. Chrome cargó el logo, fotografía, manifest y recursos PWA. |

Quedan pendientes el lector de pantalla, la revisión en dispositivos físicos y
una nueva prueba de inferencia. En esta iteración se verificó la distribución
del asistente y sus pruebas de frontend.
