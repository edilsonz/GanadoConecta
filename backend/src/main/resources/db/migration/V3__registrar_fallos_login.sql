CREATE TABLE identidad.intento_login (
    dimension VARCHAR(16) NOT NULL,
    clave_sha256 BYTEA NOT NULL,
    ventana_inicia_en TIMESTAMPTZ NOT NULL,
    fallos SMALLINT NOT NULL,
    bloqueado_hasta TIMESTAMPTZ,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_intento_login PRIMARY KEY (dimension, clave_sha256),
    CONSTRAINT ck_intento_login_dimension CHECK (dimension IN ('CORREO', 'ORIGEN')),
    CONSTRAINT ck_intento_login_clave CHECK (OCTET_LENGTH(clave_sha256) = 32),
    CONSTRAINT ck_intento_login_fallos CHECK (fallos BETWEEN 1 AND 10),
    CONSTRAINT ck_intento_login_fechas CHECK (
        actualizado_en >= ventana_inicia_en
        AND (bloqueado_hasta IS NULL OR bloqueado_hasta >= ventana_inicia_en)
    )
);

CREATE INDEX ix_intento_login_bloqueado_hasta
    ON identidad.intento_login (bloqueado_hasta)
    WHERE bloqueado_hasta IS NOT NULL;
