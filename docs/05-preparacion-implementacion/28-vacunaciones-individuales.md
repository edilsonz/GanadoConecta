# Vacunaciones individuales — SPEC-02 S01–S04

Primer incremento: GET/POST
`/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}/vacunaciones`.
La ficha React permite registrar y consultar aplicaciones realizadas, con estados
de carga, vacío confirmado y error. Conserva el mismo payload e identificadores
para reintentar una respuesta incierta. No ofrece registro sanitario offline.

V7 es aditiva; V1–V6 no cambiaron. La validación se ejecutó en la base aislada
`gc_sanidad_test_20261120` del contenedor PostgreSQL saludable, sin escribir
registros sanitarios en la base de desarrollo del usuario. Los fixtures de cada
prueba se revierten. No se borraron bases, volúmenes ni IndexedDB.

Evidencia ejecutada el 20/11/2026:

- Frontend completo: 170 pruebas aprobadas; TypeScript correcto.
- Tras aislar el nuevo panel en las pruebas antiguas: 46 pruebas dirigidas aprobadas.
- Build Vite/PWA correcto.
- Dominio: 4 pruebas aprobadas; HTTP/PostgreSQL: 4 aprobadas, incluyendo roles,
  aislamiento, CSRF, origen, reintento y conflicto (CS01–CS04, CS08).
- V7 se aplicó y validó mediante el arranque de los tests en la base aislada.
- Regresión backend relacionada: 22/22 (API base, registro y consulta de animales,
  y autorización previa a persistir vacunaciones), sin fallos ni errores.

No se ejecutó prueba manual en Chrome ni carga sanitaria. Historial ampliado,
agenda y sanidad offline siguen pendientes. Al iniciar el backend local, Flyway
aplicará V7 a la base configurada; solo introduce tablas nuevas.
