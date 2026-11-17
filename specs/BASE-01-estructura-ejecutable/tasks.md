# BASE-01 — Tareas de la iteración

- [x] B01: revisar documentación, rama/estado inicial y herramientas.
- [x] B02: fijar versiones compatibles antes de crear los proyectos.
- [x] B03: crear frontend, inicio responsivo y pruebas BASE-CA01/04.
- [x] B04: crear backend modular, salud y seguridad BASE-CA02/03.
- [x] B05: preparar Compose/PostgreSQL opcional y entorno BASE-CA05.
  Compose healthy, PostgreSQL 17.12 y conexión JDBC verificados el 17 de noviembre
  de 2026 en `127.0.0.1:5433`; el contenedor escucha internamente en 5432.
- [x] B06: instalar, compilar, probar y comprobar arranques que permita el entorno.
  Frontend/backend ejecutados; prueba JDBC y arranque con perfil `postgres` correctos.
- [x] B07: documentar comandos/resultados/limitaciones y comprobar BASE-CA06.

Resultados y límites: [informe de evidencia](../../docs/05-preparacion-implementacion/06-base-ejecutable.md).
BASE-CA05 cuenta con evidencia ejecutada; aún no existen esquema ni migraciones de negocio.

T02 de SPEC-01 y reglas de negocio permanecen pendientes. Esta base no habilita
usuarios reales ni satisface los 18 CA de animales/autenticación/offline.
