# SPEC-12 · Análisis, alertas y asistente

Alcance incremental de RF08, RF09 y RF19 (HU08, HU09 y HU20; ADR-05 y ADR-15).
Propietario, editor y lector con membresía activa pueden consultar su ganadería.
La sesión y el acceso se comprueban en cada consulta. Los integrantes sin
sesión reciben 401 y los accesos ajenos, inexistentes o revocados reciben 403.

- **RF08:** describir la distribución de eventos sanitarios confirmados y los
  resultados reproductivos registrados en un intervalo inclusivo. Con el filtro
  de animal habilitar la comparación de sus dos últimas mediciones de peso del
  intervalo. Indicar las fechas, los datos de origen y las limitaciones. Los
  partos filtrados corresponden a la madre. No calcular tendencias clínicas,
  predicciones, diagnósticos ni recomendaciones de tratamiento.
- **RF09:** mostrar avisos interiores de seguimiento con origen en el calendario
  confirmado en PostgreSQL. Reutilizar la regla de SPEC-07 en America/Lima,
  actividades de hoy (`PENDIENTE`) y anteriores sin completar (`VENCIDA`).
  Una actividad colectiva genera una fila, con su UUID, tipo, descripción y
  fecha. Las futuras y completadas quedan en el calendario. No crear registros
  nuevos al leer. Queda pendiente la detección por tendencias con criterios
  aprobados. El aviso reutiliza la agenda de RF07, que es independiente de IA.
- **RF19:** el asistente consulta datos propios con referencias y explicita
  insuficiencia de registros. El alcance de registros propios se verificó
  con inferencia local de `qwen3:4b-instruct-2507-q4_K_M` en Ollama. La
  orientación general requiere un corpus revisado y queda fuera de esta fase.

## Criterios y excepciones

| Criterio | Condición verificable |
|---|---|
| CA12-01 | El análisis identifica orígenes y período, muestra los conteos confirmados sin duplicar campañas o partos, compara pesajes en orden y explica la ausencia de datos o de un pesaje anterior. |
| CA12-02 | La consulta de alertas devuelve solo las actividades de hoy y las vencidas de la ganadería autorizada. Las recargas conservan su UUID y el cierre retira el aviso. |
| CA12-03 | La API rechaza accesos ajenos y revocados, incluido el filtro por animal ajeno, y evita cachés privadas. La UI elimina resultados anteriores al cambiar de ámbito o al recibir un rechazo. |
| CA12-04 | Sin motor disponible, la UI informa la indisponibilidad y permite usar análisis y alertas. Las solicitudes fallidas se distinguen de consultas sin datos. |
| CA12-05 | El asistente local usa hechos autorizados, cita sus registros, informa insuficiencia y mantiene el ámbito y el acceso de solo lectura ante instrucciones del usuario. El corpus de orientación general queda pendiente. |

Los parámetros inválidos se rechazan con 400. Si falla la base de datos,
la API devuelve 503. El módulo requiere conexión y no guarda los resultados
en IndexedDB ni en la caché del service worker.
