# Historial sanitario individual — SPEC-02 S05

El endpoint GET/POST
`/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}/eventos-sanitarios`
registra desparasitaciones, tratamientos sanitarios y revisiones veterinarias, y
consulta el historial unificado (incluidas vacunaciones). El GET acepta el filtro
opcional `tipo`. Los eventos son hechos inmutables y no generan consejo clínico.

React integra formulario, historial cronológico, filtro y estados de carga, vacío
y error en la ficha. Propietario/editor escriben; lector consulta. Se reutilizan
V7, CSRF, origen permitido, idempotencia y autorización transaccional.

Evidencia ejecutada en PostgreSQL aislado: 8/8 pruebas backend dirigidas y 50/50
pruebas frontend relacionadas, sin fallos; TypeScript correcto. Cubren tipos, campos,
orden, filtro, reintento, conflicto, roles y aislamiento. No se insertaron eventos
en la ganadería real y no se ejecutó prueba manual en Chrome.
