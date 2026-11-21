# Tareas · SPEC-12

- [x] CA12-01/03. Proyección de análisis, fechas inclusivas, resultados de peso y aislamiento en PostgreSQL.
- [x] CA12-02/03. Avisos internos, estados, ausencia de duplicados en consultas repetidas y segregación.
- [x] CA12-03/04. Navegación, estados de error y disponibilidad del asistente.
- [x] TypeScript, build PWA y Chrome con HTTPS y datos sintéticos.
- [ ] CA12-05. Aprobar modelo, corpus y política de tratamiento de datos. Implementar FastAPI y evaluar fundamentación, aislamiento, robustez y caída real del servicio.
- [ ] RF09. Acordar qué tendencias requieren seguimiento y sus fuentes antes de generar avisos por tendencias.

## Evidencia del incremento

- PostgreSQL Docker 17.12, base nueva aislada, Flyway V1–V17. La regresión
  dirigida pasó 18 pruebas de integración (4 de análisis, 4 de reportes y
  10 de sanidad/agenda) y 2 unitarias de agenda. Se comprobaron 401 y 403,
  revocación, intervalos inclusivos, fechas en Lima, campañas colectivas sin
  alertas duplicadas y retiro del aviso al completar la actividad.
- React: 244 pruebas aprobadas en 37 archivos, incluidas las 4 nuevas.
  TypeScript y build PWA correctos. Tras ajustar el contraste, se repitieron
  compilación y Chrome.
- Chrome headless por HTTPS, con Spring Boot y la misma base aislada. Se
  comprobaron el filtro por animal, la diferencia de 15 kg entre los dos
  pesajes sintéticos, los datos confirmados, la actualización de alertas y
  el cambio a una ganadería vacía. Se guardaron capturas locales de 360,
  768 y 1440 px, sin desbordamiento horizontal y con contraste de las tarjetas
  comprobado. El asistente informó que no estaba disponible.
- La caída real de FastAPI, la validación de referencias de un modelo y la
  robustez frente a instrucciones maliciosas quedan pendientes de CA12-05.
