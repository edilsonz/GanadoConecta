# Plan — SPEC-08

V15 aditiva con anuncios, referencias y metadatos de fotos. Lectura SQL con proyección explícita e índices para catálogo, ámbito de propietario, paginación y filtros. Fotos reencodificadas en un directorio privado con ruta configurable; extremo público valida estado y pertenencia en cada lectura. REST usa las protecciones existentes en escrituras. Pruebas unitarias de decodificación, integración MockMvc/PostgreSQL y Vitest para visibilidad, filtros y respuesta pública.

## Caché del catálogo público · ADR-11

Spring Boot consulta la revisión vigente en PostgreSQL antes de leer la caché.
Redis guarda exclusivamente páginas de `GET /api/v1/catalogo`, bajo una
clave que identifica la versión de la proyección, la revisión de PostgreSQL,
el orden, los filtros y la página. TTL predeterminado de 30 segundos,
configurable entre 1 y 300 segundos. El TTL limita la retención, mientras
que la revisión evita servir publicaciones que hayan perdido autorización.

Los cambios de anuncios, fotografías, parentescos, atributos públicos de
animales y vigencia de ganaderías escriben una revisión UUID dentro de la
transacción PostgreSQL. V19 difiere la actualización a la confirmación
para adquirir el bloqueo compartido después de las escrituras de negocio.
La revisión se vuelve visible al confirmar. La
invalidación lógica es inmediata y no requiere borrar claves de Redis. Un
fallo revertido conserva la revisión anterior. Las claves antiguas expiran.

Los comandos Redis tienen plazos acotados. Ante error se registra la apertura
de una pausa breve, se consulta PostgreSQL y se reintenta Redis en una
lectura posterior. Se miden aciertos, ausencias, fallos y omisiones.
Los archivos de fotos, las fichas privadas, las sesiones y las credenciales
quedan fuera. Las fotos conservan su verificación individual en PostgreSQL.
