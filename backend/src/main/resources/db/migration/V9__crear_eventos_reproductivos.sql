CREATE SCHEMA reproduccion;

CREATE TABLE reproduccion.evento_reproductivo (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    tipo VARCHAR(32) NOT NULL,
    fecha_evento DATE NOT NULL,
    estado_gestacion VARCHAR(24),
    fecha_probable_parto DATE,
    reproductor_animal_id UUID,
    responsable VARCHAR(150),
    observaciones VARCHAR(2000),
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_evento_reproductivo_id_ganaderia UNIQUE (id, ganaderia_id),
    CONSTRAINT fk_evento_reproductivo_animal FOREIGN KEY (animal_id, ganaderia_id)
      REFERENCES ganado.animal(id, ganaderia_id) ON DELETE RESTRICT,
    CONSTRAINT fk_evento_reproductivo_reproductor FOREIGN KEY (reproductor_animal_id, ganaderia_id)
      REFERENCES ganado.animal(id, ganaderia_id) ON DELETE RESTRICT,
    CONSTRAINT fk_evento_reproductivo_actor FOREIGN KEY (creado_por) REFERENCES identidad.usuario(id),
    CONSTRAINT ck_evento_reproductivo_tipo CHECK (tipo IN ('MONTA','INSEMINACION','EVALUACION_GESTACION')),
    CONSTRAINT ck_evento_reproductivo_estado CHECK (estado_gestacion IS NULL OR estado_gestacion IN ('CONFIRMADA','NO_CONFIRMADA','INDETERMINADA')),
    CONSTRAINT ck_evento_reproductivo_evaluacion CHECK ((tipo='EVALUACION_GESTACION')=(estado_gestacion IS NOT NULL)),
    CONSTRAINT ck_evento_reproductivo_fecha_probable CHECK (fecha_probable_parto IS NULL OR (tipo='EVALUACION_GESTACION' AND estado_gestacion='CONFIRMADA' AND fecha_probable_parto>=fecha_evento)),
    CONSTRAINT ck_evento_reproductivo_responsable CHECK (responsable IS NULL OR CHAR_LENGTH(responsable) BETWEEN 1 AND 150),
    CONSTRAINT ck_evento_reproductivo_observaciones CHECK (observaciones IS NULL OR CHAR_LENGTH(observaciones) BETWEEN 1 AND 2000)
);
CREATE INDEX ix_evento_reproductivo_historial ON reproduccion.evento_reproductivo(ganaderia_id,animal_id,fecha_evento DESC,creado_en DESC,id);

CREATE TABLE reproduccion.operacion_evento_reproductivo (
    operacion_id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    evento_id UUID NOT NULL UNIQUE,
    actor_usuario_id UUID NOT NULL,
    schema_version INTEGER NOT NULL CHECK (schema_version=1),
    huella_sha256 BYTEA NOT NULL CHECK (OCTET_LENGTH(huella_sha256)=32),
    confirmado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_operacion_reproductiva_evento FOREIGN KEY (evento_id,ganaderia_id)
      REFERENCES reproduccion.evento_reproductivo(id,ganaderia_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_operacion_reproductiva_actor FOREIGN KEY (actor_usuario_id) REFERENCES identidad.usuario(id)
);
