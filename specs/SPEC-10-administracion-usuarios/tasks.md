# Tareas — SPEC-10

- [x] CA10-01 Permiso de plataforma y controles de acceso.
- [x] CA10-02 Consultas y pantalla independiente.
- [x] CA10-03 Auditoría y enlace a moderación.
- [x] Pruebas dirigidas. 14 pruebas Java contra PostgreSQL 17.12 aislado con Flyway V17 (3 de administración, 7 de esquema y 4 de catálogo/moderación). 12 pruebas React y TypeScript sin fallos. [Validación conjunta en Chrome](../SPEC-11-reportes-ganaderos/tasks.md#evidencia-conjunta).

Para un ensayo aislado, registra una cuenta sintética, localiza su UUID en
la base de pruebas y habilita su fila en `identidad.permiso_administracion`.
Abre «Administración» después de iniciar sesión. La bandeja de moderación
requiere además `identidad.permiso_moderacion` y conserva su auditoría.

En la base aislada, sustituye el marcador por el UUID sintético registrado.

```sql
INSERT INTO identidad.permiso_administracion (usuario_id, habilitado)
VALUES ('UUID_DE_CUENTA_SINTETICA', true);
```
