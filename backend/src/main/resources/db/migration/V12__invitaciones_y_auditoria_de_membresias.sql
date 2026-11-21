CREATE TABLE identidad.invitacion (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL REFERENCES identidad.ganaderia(id),
    correo VARCHAR(254) NOT NULL,
    rol VARCHAR(16) NOT NULL CHECK (rol IN ('EDITOR', 'LECTOR')),
    huella BYTEA NOT NULL UNIQUE CHECK (octet_length(huella) = 32),
    creada_en TIMESTAMPTZ NOT NULL,
    vence_en TIMESTAMPTZ NOT NULL CHECK (vence_en > creada_en),
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('PENDIENTE','ACEPTADA','CANCELADA'))
);

CREATE UNIQUE INDEX uq_invitacion_pendiente
    ON identidad.invitacion(ganaderia_id, correo) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_invitacion_ganaderia ON identidad.invitacion(ganaderia_id, creada_en DESC);

CREATE TABLE identidad.auditoria_membresia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membresia_id UUID NOT NULL,
    ganaderia_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    rol_anterior VARCHAR(16),
    estado_anterior VARCHAR(16),
    rol_nuevo VARCHAR(16) NOT NULL,
    estado_nuevo VARCHAR(16) NOT NULL
);

CREATE FUNCTION identidad.auditar_membresia() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE actor UUID;
BEGIN
    actor := nullif(current_setting('ganadoconecta.actor_membresia', true), '')::UUID;
    IF TG_OP = 'INSERT' THEN
        INSERT INTO identidad.auditoria_membresia
            (membresia_id, ganaderia_id, usuario_id, actor_id, rol_nuevo, estado_nuevo)
        VALUES (NEW.id, NEW.ganaderia_id, NEW.usuario_id,
                coalesce(actor, NEW.usuario_id), NEW.rol, NEW.estado);
    ELSIF actor IS NOT NULL AND (OLD.rol, OLD.estado) IS DISTINCT FROM (NEW.rol, NEW.estado) THEN
        INSERT INTO identidad.auditoria_membresia
            (membresia_id, ganaderia_id, usuario_id, actor_id, rol_anterior, estado_anterior, rol_nuevo, estado_nuevo)
        VALUES (NEW.id, NEW.ganaderia_id, NEW.usuario_id, actor,
                OLD.rol, OLD.estado, NEW.rol, NEW.estado);
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER auditar_membresia AFTER INSERT OR UPDATE ON identidad.membresia
    FOR EACH ROW EXECUTE FUNCTION identidad.auditar_membresia();
