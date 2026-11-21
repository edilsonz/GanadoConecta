CREATE TABLE identidad.permiso_administracion (
    usuario_id UUID PRIMARY KEY REFERENCES identidad.usuario(id),
    habilitado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE identidad.auditoria_administracion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES identidad.usuario(id),
    operacion VARCHAR(32) NOT NULL CHECK (operacion IN ('CONSULTAR_USUARIOS', 'CONSULTAR_ESTADISTICAS')),
    consultado_en TIMESTAMPTZ NOT NULL
);
