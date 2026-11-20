# Plan técnico — SPEC-03

- Módulo Spring Boot `reproduccion` con dominio, casos de uso y adaptadores JDBC/HTTP.
- Migraciones aditivas: V9 eventos reproductivos, V10 partos y V11 parentescos/auditoría.
- Endpoints bajo `/api/v1/ganaderias/{ganaderiaId}/animales/{animalId}`; JWT, origen y CSRF existentes.
- UUIDv4 e identificadores de operación con comprobante y huella SHA-256 para idempotencia.
- React integra paneles en la ficha; el backend continúa siendo autoridad de permisos.
- No se amplía IndexedDB ni el protocolo offline en este incremento.
