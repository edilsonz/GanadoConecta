# SPEC-10 — Administración de usuarios

ACT03 · HU11 · RF11. El incremento permite consultar la administración de plataforma con permiso explícito vigente. Propietario, editor, lector y moderador requieren autorización de plataforma independiente para consultar cuentas.

- `GET /api/v1/administracion/acceso` verifica acceso. `GET /api/v1/administracion/usuarios` lista UUID, correo normalizado, estado y fecha de alta. Filtros opcionales `buscar` por correo exacto o UUID, `estado` ACTIVO/INACTIVO y `cursor` UUID. Páginas de 20 ordenadas por UUID.
- `GET /api/v1/administracion/estadisticas` devuelve cuentas y ganaderías totales y activas. Cada consulta de usuarios o estadísticas registra actor, tipo e instante. La auditoría de revisión de anuncios continúa en SPEC-09.
- Sesión ausente devuelve 401, permiso insuficiente 403 y filtros inválidos 400. Las respuestas usan no-store. No se devuelven credenciales, sesiones ni historiales privados.

| Criterio | Verificación |
|---|---|
| CA10-01 | Autorización por solicitud. Un propietario o un moderador sin permiso de plataforma recibe 403. |
| CA10-02 | Filtros, paginación, proyección mínima y agregaciones fieles a PostgreSQL. |
| CA10-03 | Auditoría de consultas autorizadas, sin almacenar los términos de búsqueda. Moderación conserva su permiso independiente. |

Pendientes de aprobación: alta y revocación administrativa por interfaz, bloqueo, suspensión y recuperación de cuentas. La migración solo crea los objetos necesarios para autorizar y auditar. El entorno de pruebas usa un permiso sintético explícito.
