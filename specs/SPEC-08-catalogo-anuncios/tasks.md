# Tareas — SPEC-08

- [x] CA08-01: persistencia y gestión privada.
- [x] CA08-02/03: catálogo público y proyección autorizada.
- [x] CA08-04: almacenamiento y acceso de fotos.
- [x] Interfaz y pruebas dirigidas.

El 21/11/2026 pasaron 9 pruebas Java (incluyen esquema V15 y dos
escenarios HTTP/PostgreSQL con imágenes sintéticas) y una prueba React de
filtros/paginación. TypeScript correcto.

Para habilitar fotos en desarrollo, crea un directorio privado fuera del
repositorio y define `GANADOCONECTA_FOTOS_DIRECTORIO` en el entorno de Spring.
Sin esa variable el resto del catálogo funciona y la carga devuelve 503.
Se incorporó la caché pública Redis y la revisión transaccional V18–V19 del
catálogo. El [informe del Sprint 2](../../docs/07-sprint-2/README.md) enlaza
las pruebas y el arnés de rendimiento. El proveedor de despliegue queda pendiente.
