CREATE SCHEMA identidad;

CREATE TABLE identidad.usuario (
    id UUID PRIMARY KEY,
    email_normalizado VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_email_normalizado UNIQUE (email_normalizado),
    CONSTRAINT ck_usuario_email_normalizado CHECK (
        email_normalizado = LOWER(BTRIM(email_normalizado))
        AND CHAR_LENGTH(email_normalizado) BETWEEN 3 AND 254
        AND email_normalizado !~ '[[:space:]]'
    ),
    CONSTRAINT ck_usuario_password_argon2id CHECK (
        password_hash LIKE '$argon2id$%'
        OR password_hash LIKE '{argon2}$argon2id$%'
    ),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_usuario_fechas CHECK (actualizado_en >= creado_en)
);

CREATE TABLE identidad.ganaderia (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    propietario_usuario_id UUID NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ganaderia_propietario
        FOREIGN KEY (propietario_usuario_id) REFERENCES identidad.usuario (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_ganaderia_nombre CHECK (
        nombre = BTRIM(nombre)
        AND CHAR_LENGTH(nombre) BETWEEN 1 AND 100
    ),
    CONSTRAINT ck_ganaderia_estado CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT ck_ganaderia_fechas CHECK (actualizado_en >= creado_en)
);

CREATE INDEX ix_ganaderia_propietario
    ON identidad.ganaderia (propietario_usuario_id);

CREATE TABLE identidad.membresia (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    rol VARCHAR(16) NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revocado_en TIMESTAMPTZ,
    CONSTRAINT fk_membresia_ganaderia
        FOREIGN KEY (ganaderia_id) REFERENCES identidad.ganaderia (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_membresia_usuario
        FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_membresia_ganaderia_usuario UNIQUE (ganaderia_id, usuario_id),
    CONSTRAINT ck_membresia_rol CHECK (rol IN ('PROPIETARIO', 'EDITOR', 'LECTOR')),
    CONSTRAINT ck_membresia_estado CHECK (estado IN ('ACTIVA', 'REVOCADA')),
    CONSTRAINT ck_membresia_fechas CHECK (
        actualizado_en >= creado_en
        AND (revocado_en IS NULL OR revocado_en >= creado_en)
    ),
    CONSTRAINT ck_membresia_revocacion CHECK (
        (estado = 'ACTIVA' AND revocado_en IS NULL)
        OR (estado = 'REVOCADA' AND revocado_en IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_membresia_propietario_activo_por_ganaderia
    ON identidad.membresia (ganaderia_id)
    WHERE rol = 'PROPIETARIO' AND estado = 'ACTIVA';

CREATE INDEX ix_membresia_usuario_activa
    ON identidad.membresia (usuario_id, ganaderia_id)
    WHERE estado = 'ACTIVA';

CREATE TABLE identidad.sesion_dispositivo (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    vence_en TIMESTAMPTZ NOT NULL,
    revocado_en TIMESTAMPTZ,
    CONSTRAINT fk_sesion_usuario
        FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_sesion_vigencia CHECK (vence_en > creado_en),
    CONSTRAINT ck_sesion_revocacion CHECK (
        revocado_en IS NULL OR revocado_en >= creado_en
    )
);

CREATE INDEX ix_sesion_usuario_vigente
    ON identidad.sesion_dispositivo (usuario_id, vence_en)
    WHERE revocado_en IS NULL;

CREATE INDEX ix_sesion_vencimiento
    ON identidad.sesion_dispositivo (vence_en);

CREATE TABLE identidad.verificador_renovacion (
    id UUID PRIMARY KEY,
    sesion_id UUID NOT NULL,
    huella_sha256 BYTEA NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumido_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    sucesor_id UUID,
    CONSTRAINT fk_verificador_sesion
        FOREIGN KEY (sesion_id) REFERENCES identidad.sesion_dispositivo (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_verificador_sucesor
        FOREIGN KEY (sucesor_id) REFERENCES identidad.verificador_renovacion (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uq_verificador_huella UNIQUE (huella_sha256),
    CONSTRAINT uq_verificador_sucesor UNIQUE (sucesor_id),
    CONSTRAINT ck_verificador_huella_sha256 CHECK (OCTET_LENGTH(huella_sha256) = 32),
    CONSTRAINT ck_verificador_estado CHECK (
        estado IN ('ACTIVO', 'CONSUMIDO', 'REVOCADO')
    ),
    CONSTRAINT ck_verificador_transicion CHECK (
        (estado = 'ACTIVO'
            AND consumido_en IS NULL
            AND revocado_en IS NULL
            AND sucesor_id IS NULL)
        OR (estado = 'CONSUMIDO'
            AND consumido_en IS NOT NULL
            AND revocado_en IS NULL
            AND sucesor_id IS NOT NULL)
        OR (estado = 'REVOCADO' AND revocado_en IS NOT NULL)
    ),
    CONSTRAINT ck_verificador_fechas CHECK (
        (consumido_en IS NULL OR consumido_en >= creado_en)
        AND (revocado_en IS NULL OR revocado_en >= creado_en)
    )
);

CREATE UNIQUE INDEX uq_verificador_activo_por_sesion
    ON identidad.verificador_renovacion (sesion_id)
    WHERE estado = 'ACTIVO';

CREATE INDEX ix_verificador_sesion_creacion
    ON identidad.verificador_renovacion (sesion_id, creado_en);

CREATE FUNCTION identidad.exigir_propietario_activo(p_ganaderia_id UUID)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM identidad.ganaderia g WHERE g.id = p_ganaderia_id
    ) AND NOT EXISTS (
        SELECT 1
        FROM identidad.ganaderia g
        JOIN identidad.membresia m
          ON m.ganaderia_id = g.id
         AND m.usuario_id = g.propietario_usuario_id
         AND m.rol = 'PROPIETARIO'
         AND m.estado = 'ACTIVA'
        WHERE g.id = p_ganaderia_id
    ) THEN
        RAISE EXCEPTION 'La ganaderia requiere su membresia PROPIETARIO activa'
            USING ERRCODE = '23514',
                  CONSTRAINT = 'ck_ganaderia_propietario_activo';
    END IF;
END;
$$;

CREATE FUNCTION identidad.validar_propietario_desde_ganaderia()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM identidad.exigir_propietario_activo(NEW.id);
    RETURN NULL;
END;
$$;

CREATE FUNCTION identidad.validar_propietario_desde_membresia()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM identidad.exigir_propietario_activo(OLD.ganaderia_id);
    ELSE
        PERFORM identidad.exigir_propietario_activo(NEW.ganaderia_id);
        IF TG_OP = 'UPDATE' AND OLD.ganaderia_id <> NEW.ganaderia_id THEN
            PERFORM identidad.exigir_propietario_activo(OLD.ganaderia_id);
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ct_ganaderia_propietario_activo
AFTER INSERT OR UPDATE ON identidad.ganaderia
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION identidad.validar_propietario_desde_ganaderia();

CREATE CONSTRAINT TRIGGER ct_membresia_propietario_activo
AFTER INSERT OR UPDATE OR DELETE ON identidad.membresia
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION identidad.validar_propietario_desde_membresia();
