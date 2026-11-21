# Plan — SPEC-09

V16 amplía estados, persiste recibos, permisos administrativos específicos y reportes. La venta comparte el bloqueo transaccional de ganadería con la publicación y conserva versión de creación de SPEC-01. Las escrituras administrativas bloquean actor, sesión, permiso y luego filas de moderación. El catálogo consulta PostgreSQL y excluye inmediatamente retiros y ventas. Pruebas HTTP/PostgreSQL aislado y Vitest; no se usa caché para decisiones de visibilidad.
