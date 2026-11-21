# Plan — SPEC-04

- AF01/AF03: reutilizar autorización transaccional, sesiones y AU07.
- AF02: migración V12 con huella SHA-256 y estado; bloqueo de ganadería compartido con AU07 y bloqueo de la invitación antes de consumirla. Código por POST, jamás en URL ni en almacenamiento persistente del cliente.
- AF04: auditoría transaccional de altas/cambios mediante trigger y contexto de actor limitado a la transacción.
- REST privado bajo ganaderías y un punto autenticado de aceptación. React usa JWT en memoria y CSRF vigente.
- MockMvc y PostgreSQL aislado para AF01–AF04. Vitest para interacción y contrato del cliente.
