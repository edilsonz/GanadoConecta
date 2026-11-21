# Tareas — SPEC-07

- [x] CA07-01: ampliar calendario, filtro y estados con V14.
- [x] CA07-02/03: confirmar atenciones de forma atómica e idempotente.
- [x] CA07-04: avisos internos y puerto EXT01.
- [x] Validación dirigida el 21/11/2026: 18 pruebas Java con PostgreSQL aislado, 11 pruebas React y TypeScript aprobados.

- [x] Corrección CA07-02/03: el formulario distingue las actividades de agenda de las campañas que requieren confirmación por animal. Desde una vacunación colectiva se puede elegir el tipo campaña conservando los participantes. La campaña realizada muestra quién quedó sin atención confirmada.
- [x] Regresión dirigida: 7 pruebas React y 10 pruebas Java con PostgreSQL aislado, sin fallos. La prueba de vacunación confirmó solo PRUEBA-LOTE-01 y comprobó el vínculo con la campaña, un único evento tras repetir la confirmación y cero eventos de PRUEBA-LOTE-02. TypeScript y build PWA correctos.

La prueba manual previa observó el cierre de agenda sin atenciones. Queda pendiente repetir en Chrome el flujo corregido. En el selector se muestran los animales cargados en «Mi ganado». Cargar más resultados amplía la selección.
