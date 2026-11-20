CREATE TABLE sanidad.actividad_agenda (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    evento_origen_id UUID,
    tipo VARCHAR(32) NOT NULL,
    fecha_prevista DATE NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    observaciones VARCHAR(2000),
    estado VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE',
    version BIGINT NOT NULL DEFAULT 1,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    completado_por UUID,
    completado_en TIMESTAMPTZ,
    CONSTRAINT uq_actividad_agenda_id_ganaderia UNIQUE (id, ganaderia_id),
    CONSTRAINT fk_agenda_animal FOREIGN KEY (animal_id, ganaderia_id)
        REFERENCES ganado.animal (id, ganaderia_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_agenda_evento FOREIGN KEY (evento_origen_id, ganaderia_id)
        REFERENCES sanidad.evento_sanitario (id, ganaderia_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_agenda_creador FOREIGN KEY (creado_por)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_agenda_completado_por FOREIGN KEY (completado_por)
        REFERENCES identidad.usuario (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_agenda_tipo CHECK (tipo IN ('VACUNACION','TRATAMIENTO_SANITARIO','REVISION_VETERINARIA')),
    CONSTRAINT ck_agenda_descripcion CHECK (CHAR_LENGTH(descripcion) BETWEEN 1 AND 1000),
    CONSTRAINT ck_agenda_observaciones CHECK (observaciones IS NULL OR CHAR_LENGTH(observaciones) BETWEEN 1 AND 2000),
    CONSTRAINT ck_agenda_estado CHECK (estado IN ('PENDIENTE','COMPLETADA')),
    CONSTRAINT ck_agenda_version CHECK (version IN (1,2)),
    CONSTRAINT ck_agenda_completada CHECK (
      (estado='PENDIENTE' AND version=1 AND completado_por IS NULL AND completado_en IS NULL)
      OR (estado='COMPLETADA' AND version=2 AND completado_por IS NOT NULL AND completado_en IS NOT NULL)
    )
);
CREATE INDEX ix_agenda_ganaderia_fecha ON sanidad.actividad_agenda
    (ganaderia_id, estado, fecha_prevista, id);
CREATE INDEX ix_agenda_animal_fecha ON sanidad.actividad_agenda
    (ganaderia_id, animal_id, fecha_prevista, id);
