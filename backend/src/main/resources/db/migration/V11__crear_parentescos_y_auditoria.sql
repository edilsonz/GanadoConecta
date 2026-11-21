CREATE TABLE reproduccion.parentesco(
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(8) NOT NULL,
    progenitor_animal_id UUID NOT NULL,
    origen VARCHAR(16) NOT NULL,
    parto_id UUID,
    actualizado_por UUID NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY(ganaderia_id,animal_id,tipo),
    CONSTRAINT fk_parentesco_animal FOREIGN KEY(animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    CONSTRAINT fk_parentesco_progenitor FOREIGN KEY(progenitor_animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    CONSTRAINT fk_parentesco_parto FOREIGN KEY(parto_id,ganaderia_id) REFERENCES reproduccion.parto(id,ganaderia_id),
    CONSTRAINT fk_parentesco_actor FOREIGN KEY(actualizado_por) REFERENCES identidad.usuario(id),
    CONSTRAINT ck_parentesco_tipo CHECK(tipo IN('MADRE','PADRE')),
    CONSTRAINT ck_parentesco_origen CHECK(origen IN('PARTO','CORRECCION')),
    CONSTRAINT ck_parentesco_distinto CHECK(animal_id<>progenitor_animal_id)
);
CREATE INDEX ix_parentesco_progenitor ON reproduccion.parentesco(ganaderia_id,progenitor_animal_id,tipo,animal_id);
CREATE TABLE reproduccion.parentesco_historial(
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(8) NOT NULL CHECK(tipo IN('MADRE','PADRE')),
    progenitor_anterior_id UUID,
    progenitor_nuevo_id UUID,
    motivo VARCHAR(24) NOT NULL CHECK(motivo IN('PARTO','CORRECCION','RETIRO')),
    actor_usuario_id UUID NOT NULL REFERENCES identidad.usuario(id),
    operacion_id UUID,
    registrado_en TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_parentesco_historial_animal ON reproduccion.parentesco_historial(ganaderia_id,animal_id,registrado_en DESC);
CREATE TABLE reproduccion.operacion_parentesco(
    operacion_id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(8) NOT NULL,
    progenitor_animal_id UUID,
    actor_usuario_id UUID NOT NULL REFERENCES identidad.usuario(id),
    schema_version INTEGER NOT NULL CHECK(schema_version=1),
    huella_sha256 BYTEA NOT NULL CHECK(OCTET_LENGTH(huella_sha256)=32),
    confirmado_en TIMESTAMPTZ NOT NULL
);
INSERT INTO reproduccion.parentesco(ganaderia_id,animal_id,tipo,progenitor_animal_id,origen,parto_id,actualizado_por,actualizado_en)
SELECT pc.ganaderia_id,pc.cria_id,'MADRE',p.madre_id,'PARTO',p.id,p.creado_por,p.creado_en FROM reproduccion.parto_cria pc JOIN reproduccion.parto p ON p.id=pc.parto_id
ON CONFLICT DO NOTHING;
INSERT INTO reproduccion.parentesco(ganaderia_id,animal_id,tipo,progenitor_animal_id,origen,parto_id,actualizado_por,actualizado_en)
SELECT pc.ganaderia_id,pc.cria_id,'PADRE',p.padre_id,'PARTO',p.id,p.creado_por,p.creado_en FROM reproduccion.parto_cria pc JOIN reproduccion.parto p ON p.id=pc.parto_id WHERE p.padre_id IS NOT NULL
ON CONFLICT DO NOTHING;
