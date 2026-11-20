CREATE TABLE reproduccion.parto (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    madre_id UUID NOT NULL,
    padre_id UUID,
    fecha_parto DATE NOT NULL,
    resultado VARCHAR(32) NOT NULL,
    observaciones VARCHAR(2000),
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_parto_id_ganaderia UNIQUE(id,ganaderia_id),
    CONSTRAINT fk_parto_madre FOREIGN KEY(madre_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    CONSTRAINT fk_parto_padre FOREIGN KEY(padre_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    CONSTRAINT fk_parto_actor FOREIGN KEY(creado_por) REFERENCES identidad.usuario(id),
    CONSTRAINT ck_parto_resultado CHECK(resultado IN('CON_CRIAS_REGISTRADAS','SIN_CRIAS_REGISTRADAS')),
    CONSTRAINT ck_parto_observaciones CHECK(observaciones IS NULL OR CHAR_LENGTH(observaciones) BETWEEN 1 AND 2000)
);
CREATE INDEX ix_parto_madre_fecha ON reproduccion.parto(ganaderia_id,madre_id,fecha_parto DESC,creado_en DESC,id);
CREATE TABLE reproduccion.parto_cria(
    parto_id UUID NOT NULL,
    ganaderia_id UUID NOT NULL,
    cria_id UUID NOT NULL,
    PRIMARY KEY(parto_id,cria_id),
    CONSTRAINT uq_cria_parto UNIQUE(ganaderia_id,cria_id),
    CONSTRAINT fk_parto_cria_parto FOREIGN KEY(parto_id,ganaderia_id) REFERENCES reproduccion.parto(id,ganaderia_id),
    CONSTRAINT fk_parto_cria_animal FOREIGN KEY(cria_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id)
);
CREATE TABLE reproduccion.operacion_parto(
    operacion_id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    parto_id UUID NOT NULL UNIQUE,
    actor_usuario_id UUID NOT NULL,
    schema_version INTEGER NOT NULL CHECK(schema_version=1),
    huella_sha256 BYTEA NOT NULL CHECK(OCTET_LENGTH(huella_sha256)=32),
    confirmado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_operacion_parto FOREIGN KEY(parto_id,ganaderia_id) REFERENCES reproduccion.parto(id,ganaderia_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_operacion_parto_actor FOREIGN KEY(actor_usuario_id) REFERENCES identidad.usuario(id)
);
