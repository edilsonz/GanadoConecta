# Plan — SPEC-06

React y TypeScript reutilizan el cliente registrarAnimal y el formulario de SPEC-01 como fuente de validación y payload. El cliente confirma de manera secuencial para que cada respuesta corresponda a una fila. PostgreSQL y los comprobantes existentes mantienen idempotencia y el autorizador revalida cada escritura. Vitest verifica vista previa, errores parciales y reenvíos; las pruebas backend del alta individual verifican LT04.
