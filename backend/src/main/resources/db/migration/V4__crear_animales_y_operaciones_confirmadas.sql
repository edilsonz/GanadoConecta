CREATE SCHEMA ganado;

CREATE TABLE ganado.animal (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    sexo VARCHAR(8) NOT NULL,
    nombre VARCHAR(100),
    arete_frente VARCHAR(100),
    arete_reverso VARCHAR(100),
    raza VARCHAR(100),
    fecha_nacimiento DATE,
    nacimiento_estimado BOOLEAN,
    categoria VARCHAR(16),
    observaciones VARCHAR(2000),
    estado VARCHAR(16) NOT NULL DEFAULT 'ACTIVO',
    version BIGINT NOT NULL DEFAULT 1,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_animal_ganaderia FOREIGN KEY (ganaderia_id)
        REFERENCES identidad.ganaderia (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_animal_creado_por FOREIGN KEY (creado_por)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_animal_id_ganaderia UNIQUE (id, ganaderia_id),
    CONSTRAINT ck_animal_sexo CHECK (sexo IN ('MACHO', 'HEMBRA')),
    CONSTRAINT ck_animal_nombre CHECK (nombre IS NULL OR CHAR_LENGTH(nombre) BETWEEN 1 AND 100),
    CONSTRAINT ck_animal_arete_frente CHECK (arete_frente IS NULL OR CHAR_LENGTH(arete_frente) BETWEEN 1 AND 100),
    CONSTRAINT ck_animal_arete_reverso CHECK (arete_reverso IS NULL OR CHAR_LENGTH(arete_reverso) BETWEEN 1 AND 100),
    CONSTRAINT ck_animal_raza CHECK (raza IS NULL OR CHAR_LENGTH(raza) BETWEEN 1 AND 100),
    CONSTRAINT ck_animal_nacimiento CHECK (
        (fecha_nacimiento IS NULL AND nacimiento_estimado IS NULL)
        OR (fecha_nacimiento IS NOT NULL AND nacimiento_estimado IS NOT NULL)
    ),
    CONSTRAINT ck_animal_categoria CHECK (
        categoria IS NULL OR categoria IN (
            'TERNERA', 'VAQUILLA', 'VACA', 'TERNERO', 'TORETE', 'TORO'
        )
    ),
    CONSTRAINT ck_animal_categoria_sexo CHECK (
        categoria IS NULL
        OR (sexo = 'HEMBRA' AND categoria IN ('TERNERA', 'VAQUILLA', 'VACA'))
        OR (sexo = 'MACHO' AND categoria IN ('TERNERO', 'TORETE', 'TORO'))
    ),
    CONSTRAINT ck_animal_observaciones CHECK (
        observaciones IS NULL OR CHAR_LENGTH(observaciones) BETWEEN 1 AND 2000
    ),
    CONSTRAINT ck_animal_estado CHECK (estado = 'ACTIVO'),
    CONSTRAINT ck_animal_version CHECK (version = 1)
);

CREATE INDEX ix_animal_ganaderia_listado
    ON ganado.animal (ganaderia_id, creado_en DESC, id);

CREATE INDEX ix_animal_ganaderia_arete_frente
    ON ganado.animal (ganaderia_id, LOWER(arete_frente))
    WHERE arete_frente IS NOT NULL;

CREATE INDEX ix_animal_ganaderia_arete_reverso
    ON ganado.animal (ganaderia_id, LOWER(arete_reverso))
    WHERE arete_reverso IS NOT NULL;

CREATE TABLE ganado.operacion_confirmada (
    operacion_id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    actor_usuario_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(32) NOT NULL,
    schema_version INTEGER NOT NULL,
    base_version BIGINT NOT NULL,
    huella_sha256 BYTEA NOT NULL,
    version_creada BIGINT NOT NULL,
    confirmado_en TIMESTAMPTZ NOT NULL,
    arete_coincidente BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_operacion_ganaderia FOREIGN KEY (ganaderia_id)
        REFERENCES identidad.ganaderia (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_operacion_actor FOREIGN KEY (actor_usuario_id)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_operacion_animal FOREIGN KEY (animal_id, ganaderia_id)
        REFERENCES ganado.animal (id, ganaderia_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uq_operacion_animal UNIQUE (animal_id),
    CONSTRAINT ck_operacion_tipo CHECK (tipo = 'CREAR_ANIMAL'),
    CONSTRAINT ck_operacion_schema_version CHECK (schema_version = 1),
    CONSTRAINT ck_operacion_base_version CHECK (base_version = 0),
    CONSTRAINT ck_operacion_huella CHECK (OCTET_LENGTH(huella_sha256) = 32),
    CONSTRAINT ck_operacion_version_creada CHECK (version_creada = 1)
);

CREATE INDEX ix_operacion_ganaderia_confirmacion
    ON ganado.operacion_confirmada (ganaderia_id, confirmado_en DESC);
