# Plan — SPEC-07

Ampliar la tabla de agenda mediante V14 y añadir participantes con claves de ganadería compuestas. Reutilizar rutas, casos de uso, autorización y cronología. La confirmación escribe historial y resultado en la misma transacción de PostgreSQL; revalida rol y coteja los datos al reintentar. La ampliación no modifica el protocolo offline. MockMvc sobre PostgreSQL aislado y Vitest verifican CA07-01–04.
