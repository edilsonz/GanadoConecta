# Renovación visual de GanadoConecta

Validación del 21 de noviembre de 2026 en Chrome headless a 360, 768 y 1440 px.
Se sirvió el build PWA por HTTPS con Spring Boot y PostgreSQL 17.12 en una base
dedicada. Las cuentas, ganaderías, animales, anuncios y pesajes son sintéticos.

| Comprobación | Resultado observado |
|---|---|
| Frontend | 248 pruebas aprobadas en 38 archivos. TypeScript y build PWA correctos. |
| Distribución | 63 comprobaciones de vista y ancho, sin desbordamiento horizontal de página. Incluyeron portada, acceso, registro, catálogo, selector, ficha y sus seis pestañas, sanidad, reproducción, producción, calendario, familia, administración, reportes y asistente. El lote mantiene su desplazamiento interno de tabla. |
| Ajuste final de portada | Las 12 comprobaciones públicas se repitieron tras ajustar el titular. «Todo conectado.» quedó en una sola línea a los tres anchos. |
| Tablas y filtro de peso | Seis comprobaciones adicionales en los tres anchos. Al filtrar por animal y período se mostraron la gráfica y las dos mediciones sintéticas. En móvil la tabla de alta masiva recibió foco y se desplazó horizontalmente con el teclado dentro de su contenedor. |
| Teclado | El enlace de salto recibió foco visible y llevó al contenido. Las flechas movieron el foco y activaron la pestaña correspondiente en la ficha. |
| Marca y PWA | Chrome cargó el logo y la fotografía. Se verificaron el favicon de 32 px y los iconos de 192 y 512 px, incluida la variante enmascarable. El manifest respondió y el service worker conservó el logo y la foto de portada en la caché del app shell. |
| Catálogo con datos | El [recorrido reproducible](../../README.md) de SPEC-08 pasó con anuncio, fotografía sintética, filtros, paginación y pausa. |

Las capturas se tomaron en la aplicación en ejecución. La comprobación de
teclado corresponde a Chrome headless. Queda pendiente una revisión con lector
de pantalla y dispositivos físicos.

| Vista | Captura |
|---|---|
| Portada | [Escritorio](portada-1440.png) · [Tablet](portada-768.png) · [Móvil](portada-360.png) |
| Acceso | [Tablet](acceso-768.png) |
| Panel privado | [Escritorio](panel-1440.png) · [Móvil](panel-360.png) |
| Ficha y gráfica de peso | [Móvil](ficha-360.png) |
| Reportes | [Tablet](reportes-768.png) |
| Administración | [Móvil](administracion-360.png) |
| Catálogo con publicaciones sintéticas | [Segunda página](catalogo-pagina-2.png) |
