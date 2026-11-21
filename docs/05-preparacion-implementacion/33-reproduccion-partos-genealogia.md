# Reproducción, partos y genealogía — SPEC-03

## Resultado

Se implementaron tres incrementos independientes:

- eventos `MONTA`, `INSEMINACION` y `EVALUACION_GESTACION`, con estado explícito y fecha probable solo cuando la informa el usuario;
- partos idempotentes con cero o varias crías ya registradas y padre conocido opcional, sin modificar automáticamente las fichas;
- parentescos privados madre/padre, ascendencia disponible hasta abuelos, descendencia directa y correcciones con auditoría.

Propietario y editor escriben; lector consulta. Todas las operaciones revalidan sesión y membresía en PostgreSQL. Se rechazan animales de otra ganadería, sexo incompatible, autorrelaciones y ciclos. La observación libre de FIONA no se interpretó ni transformó.

Flyway incorpora V9, V10 y V11 de forma aditiva. Las relaciones originadas por un parto alimentan la genealogía. Los reintentos conservan `operationId` y la huella del payload.

## Evidencia ejecutada el 20/11/2026

- Pruebas PostgreSQL dirigidas: 9/9, en `ganadoconecta_reproduccion_test` sobre PostgreSQL 17.12 y puerto Docker 5433.
- Pruebas React dirigidas: 6/6.
- Regresión backend final: 155/155, 0 fallos, 0 errores; Flyway validó V1–V11.
- Regresión frontend final: 218/218, 0 fallos. El entorno emitió avisos de conexión rechazada al puerto 3000 que no pertenecen a una aserción ni causaron fallos.
- TypeScript y build PWA: correctos; se generó el service worker de producción.
- `git diff --check`: correcto antes de preparar el commit.

No se ejecutó una prueba manual en Chrome. Reproducción, partos y genealogía offline quedan fuera de este incremento; no se amplió IndexedDB ni el protocolo de sincronización.
