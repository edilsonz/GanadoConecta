# Tareas — SPEC-11

- [x] CA11-01 Agregaciones y filtros inclusivos sin dobles conteos.
- [x] CA11-02 Autorización y aislamiento.
- [x] CA11-03 Vista responsiva y gráfica/tabla de pesajes.
- [x] Pruebas dirigidas y evidencia.

## Evidencia conjunta

- **PostgreSQL 17.12 aislado, Flyway V1–V17.** 38 pruebas aprobadas en la regresión dirigida final, sin fallos ni omisiones. Incluye 4 pruebas de reportes con límites de fecha, campaña de atención parcial, partos/crías, variaciones de peso, roles vigentes, revocación y aislamiento.
- **Frontend.** 240 pruebas aprobadas en 36 archivos. TypeScript y build PWA correctos.
- **Chrome headless por HTTPS.** Bases separadas para integración y navegador, datos sintéticos, Spring Boot y PostgreSQL reales. A 1440, 768 y 360 px se verificaron administración, entrada a moderación con su permiso propio, reportes, filtro de animal, gráfica y variación de +15 kg. La cuenta común obtuvo HTTP 403 y el enlace administrativo quedó oculto. PostgreSQL registró seis auditorías de las consultas en navegador. Las capturas locales están en `.cache/spec1011-browser/`.

Para revisar en Chrome, prepara el [entorno HTTPS aislado](../../tests/e2e/README.md),
crea una ganadería y un animal sintéticos y registra eventos y dos pesajes.
En la ganadería abre «Reportes», consulta un período inclusivo, selecciona
el animal y compara los resultados con su ficha. El rango inicial abarca
los últimos 30 días de Lima. Quedan pendientes la prueba con lectores de pantalla
y las mediciones de escala sobre un volumen representativo.
