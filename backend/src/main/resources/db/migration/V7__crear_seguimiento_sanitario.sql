CREATE SCHEMA sanidad;

CREATE TABLE sanidad.evento_sanitario (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(32) NOT NULL,
    fecha_evento DATE NOT NULL,
    nombre VARCHAR(150),
    descripcion VARCHAR(1000),
    responsable VARCHAR(150),
    lote VARCHAR(100),
    producto VARCHAR(150),
    indicaciones VARCHAR(1000),
    observaciones VARCHAR(2000),
    proxima_fecha DATE,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_evento_sanitario_id_ganaderia UNIQUE (id, ganaderia_id),
    CONSTRAINT fk_evento_sanitario_animal FOREIGN KEY (animal_id, ganaderia_id)
        REFERENCES ganado.animal (id, ganaderia_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_evento_sanitario_actor FOREIGN KEY (creado_por)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_evento_sanitario_tipo CHECK (tipo IN (
        'VACUNACION', 'DESPARASITACION', 'TRATAMIENTO_SANITARIO', 'REVISION_VETERINARIA'
    )),
    CONSTRAINT ck_evento_sanitario_nombre CHECK (
        nombre IS NULL OR CHAR_LENGTH(nombre) BETWEEN 1 AND 150
    ),
    CONSTRAINT ck_evento_sanitario_descripcion CHECK (
        descripcion IS NULL OR CHAR_LENGTH(descripcion) BETWEEN 1 AND 1000
    ),
    CONSTRAINT ck_evento_sanitario_responsable CHECK (
        responsable IS NULL OR CHAR_LENGTH(responsable) BETWEEN 1 AND 150
    ),
    CONSTRAINT ck_evento_sanitario_lote CHECK (
        lote IS NULL OR CHAR_LENGTH(lote) BETWEEN 1 AND 100
    ),
    CONSTRAINT ck_evento_sanitario_producto CHECK (
        producto IS NULL OR CHAR_LENGTH(producto) BETWEEN 1 AND 150
    ),
    CONSTRAINT ck_evento_sanitario_indicaciones CHECK (
        indicaciones IS NULL OR CHAR_LENGTH(indicaciones) BETWEEN 1 AND 1000
    ),
    CONSTRAINT ck_evento_sanitario_observaciones CHECK (
        observaciones IS NULL OR CHAR_LENGTH(observaciones) BETWEEN 1 AND 2000
    ),
    CONSTRAINT ck_evento_sanitario_vacunacion CHECK (
        tipo <> 'VACUNACION' OR nombre IS NOT NULL
    ),
    CONSTRAINT ck_evento_sanitario_otro CHECK (
        tipo = 'VACUNACION' OR descripcion IS NOT NULL
    ),
    CONSTRAINT ck_evento_sanitario_proxima_fecha CHECK (
        proxima_fecha IS NULL OR proxima_fecha >= fecha_evento
    )
);

CREATE INDEX ix_evento_sanitario_animal_fecha
    ON sanidad.evento_sanitario (ganaderia_id, animal_id, fecha_evento DESC, creado_en DESC, id);

CREATE TABLE sanidad.operacion_confirmada (
    operacion_id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    evento_id UUID NOT NULL,
    actor_usuario_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    schema_version INTEGER NOT NULL,
    huella_sha256 BYTEA NOT NULL,
    confirmado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_operacion_sanitaria_evento UNIQUE (evento_id),
    CONSTRAINT fk_operacion_sanitaria_evento FOREIGN KEY (evento_id, ganaderia_id)
        REFERENCES sanidad.evento_sanitario (id, ganaderia_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_operacion_sanitaria_animal FOREIGN KEY (animal_id, ganaderia_id)
        REFERENCES ganado.animal (id, ganaderia_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_operacion_sanitaria_actor FOREIGN KEY (actor_usuario_id)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_operacion_sanitaria_tipo CHECK (tipo IN (
        'REGISTRAR_VACUNACION', 'REGISTRAR_EVENTO_SANITARIO'
    )),
    CONSTRAINT ck_operacion_sanitaria_schema CHECK (schema_version = 1),
    CONSTRAINT ck_operacion_sanitaria_huella CHECK (OCTET_LENGTH(huella_sha256) = 32)
);
