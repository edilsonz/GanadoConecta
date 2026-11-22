# Tareas · SPEC-12

- [x] CA12-01/03. Proyección de análisis, fechas inclusivas, resultados de peso y aislamiento en PostgreSQL.
- [x] CA12-02/03. Avisos internos, estados, ausencia de duplicados en consultas repetidas y segregación.
- [x] CA12-03/04. Navegación, estados de error y disponibilidad del asistente.
- [x] TypeScript, build PWA y Chrome con HTTPS y datos sintéticos.
- [x] CA12-05. Validar inferencia local, siempre de solo lectura, para registros propios autorizados.
- [ ] Aprobar y validar el corpus para la orientación general de RF19.
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
- La prueba de inferencia y la degradación del asistente se registran en la fase 2.

## Fase 2 · Validación local de RF19

- Python 3.14.5 y dependencias fijadas en un entorno virtual. `pip check`
  correcto. Ollama portable 0.40.17 y pesos oficiales `0edcdef34593` en la
  caché ignorada. Nube desactivada y servidor en loopback.
- Regresión dirigida de Spring con PostgreSQL 17.12 aislado: 21 pruebas
  aprobadas. Tras agrupar los dos pesajes se repitieron únicamente las 4 de
  RF19. Incluyeron referencias, CSRF, origen, aislamiento y revocación
  durante la inferencia con un motor controlado.
- FastAPI: 4 pruebas dirigidas aprobadas tras el cambio de selector, incluida
  salida inválida y demora controlada. La petición HTTP sin credencial produjo 401.
- React: 247 pruebas aprobadas y build PWA con TypeScript correcto.
- Chrome headless por HTTPS: el asistente comunicó indisponibilidad con
  FastAPI detenido mientras los 15 kg de diferencia siguieron accesibles
  por el análisis determinista en PostgreSQL. Se repitió con FastAPI activo
  y Ollama inaccesible. La interfaz no desbordó a 360, 768 ni 1440 px.
- El 21 de noviembre de 2026 se reanudó la prueba. Verificación SHA-256 de los
  seis componentes del modelo ya descargado. Ollama registró 2,61 GiB de VRAM
  para el modelo y ejecutó 37 de 37 capas en la RTX 2060. La
  primera consulta directa tardó 8,90 s, incluidos 7,35 s de carga. Las
  otras seis de ese ensayo tuvieron una mediana de 0,84 s. El ensayo inicial
  acertó 6/7 preguntas y devolvió un solo pesaje al pedir los dos.
- Tras separar el último pesaje, la pareja y la variación como hechos
  verificables, la selección de Ollama a través de FastAPI aprobó 8/8
  preguntas sintéticas en español. En caliente, 0,43 a 1,10 s y mediana de
  0,52 s según tiempos HTTP. La evaluación es funcional sobre esos casos.
- Chrome HTTPS recorrió React → Spring Boot → FastAPI → Ollama con PostgreSQL
  aislado. Verificó cinco preguntas con inferencia real, el rechazo de una
  instrucción ajena y la respuesta sin datos al cambiar de ganadería. El chat
  mostró los pesajes de 120 y 135 kg, la variación de +15 kg y sus dos fechas.
  Las seis consultas que invocaron al modelo tardaron 0,52 a 1,04 s desde el
  envío hasta el mensaje visible. Se guardaron capturas locales a 360, 768 y
  1440 px. Chrome fijó solo la huella del certificado original de Vite para
  conservar HTTPS, cookies seguras y CSRF.
- En el entorno anterior hubo cargas en frío que agotaron el plazo e incluso
  una carga exitosa de 77 s. La prueba final reutilizó los pesos en una carpeta
  temporal de C:. Conviene precargar en el equipo de la sustentación. Quedan
  pendientes la orientación general y pruebas de calidad con un conjunto mayor.
