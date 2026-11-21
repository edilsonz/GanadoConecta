# Validación funcional final de SPEC-10 y SPEC-11

El 21 de noviembre de 2026 se verificó `df54df3` con la PWA compilada,
Chrome 158 headless, Spring Boot 3.5.16 y PostgreSQL Docker 17.12. Flyway
aplicó V1–V17 en una base nueva aislada. El frontend usó
`https://127.0.0.1:15173` con el backend de pruebas en 18082. El arnés
reutilizó los [controles de Chrome del repositorio](../chrome.mjs) y envió
solicitudes a la API real. Las comparaciones usaron consultas SQL de
solo lectura sobre la base sintética.

| Caso | Resultado | Comprobación |
|---|---|---|
| CA10-02/03 · Administración | Aprobado | Avance y retroceso por cursor comparados con el orden por UUID de PostgreSQL. Búsqueda por correo completo y UUID, filtro inactivo, vacío y error por búsqueda inválida. Las ocho consultas válidas del listado y una de estadísticas quedaron auditadas. Los indicadores coincidieron con SQL: 52 cuentas, 30 activas, ocho ganaderías y seis activas en la repetición dirigida. |
| CA10-01 · Autorización de plataforma | Aprobado | Un editor y un moderador sin permiso de plataforma obtuvieron HTTP 403 en acceso, listado y estadísticas. Abrir directamente `#administracion` dejó la vista privada ausente. El administrador sin permiso de moderación obtuvo 403 en la bandeja. El moderador pudo abrir su bandeja. La recepción de reportes seguía desactivada. |
| CA11-01/03 · Reportes y filtros | Aprobado | El período del 1 al 30 de septiembre incluyó ambos extremos y excluyó los registros externos al rango. Tres eventos en la ganadería y dos en la madre. La atención confirmada de una campaña con dos participantes contó una sola vez. Se comprobaron una monta, una inseminación, tres evaluaciones con sus resultados, un parto y dos crías, sin sumar otra vez sus vínculos genealógicos. |
| CA11-02 · Aislamiento | Aprobado | Propietario, editor y lector accedieron a su ganadería. Se rechazaron recursos ajenos y se ocultó el reporte del lector después de revocar su membresía sintética. |
| UX · 1440 px | Aprobado | Navegación y focos por teclado, formularios y gráfica, con contenido dentro del ancho útil. |
| UX · 768 px | Aprobado | Navegación adaptable, filtros y focos por teclado, con tabla y gráfica legibles. |
| UX · 360 px | Aprobado | Administración y Reportes utilizables con teclado, sin desplazamiento horizontal. |

La tabla de peso mostró 120,25, 140 y 135,5 kg con tres puntos en la gráfica
y variaciones de +19,75 y −4,5 kg. La primera medición del rango quedó sin
anterior. También se verificaron una ganadería vacía, un animal sin pesajes
y el mensaje de intervalo inválido junto con HTTP 400.

Resultado final **7/7 aprobados, 0 fallos funcionales**. La corrida inicial
tuvo un error de sintaxis en la consulta auxiliar del arnés. Se corrigió en
la sesión de validación y se repitió únicamente el caso administrativo.
La aplicación conservó el código de `df54df3` y se reutilizaron los resultados
automatizados previos.

Capturas de la primera corrida con 26 cuentas y cuatro ganaderías sintéticas.
Las capturas de escritorio y tablet se conservaron localmente.
[Administración móvil](spec10-11-administracion-360.png) ·
[Reportes en móvil](spec10-11-reportes-360.png) ·
[Gráfica y tabla en móvil](spec10-11-peso-360.png).

La revisión de teclado comprobó foco y orden de controles en Chrome headless.
Quedan pendientes la evaluación con un lector de pantalla y con dispositivos
físicos. Se consultó la bandeja de moderación, cuyas acciones de retiro quedan
fuera de esta validación.
