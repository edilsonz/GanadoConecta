ALTER TABLE sanidad.actividad_agenda
    ALTER COLUMN animal_id DROP NOT NULL,
    DROP CONSTRAINT ck_agenda_tipo,
    ADD CONSTRAINT ck_agenda_tipo CHECK (tipo IN ('VACUNACION','TRATAMIENTO_SANITARIO',
        'REVISION_VETERINARIA','REPRODUCTIVA','PRODUCTIVA','CAMPANA_VACUNACION',
        'CAMPANA_DESPARASITACION','CAMPANA_BANO')),
    ADD COLUMN fecha_atencion DATE,
    ADD COLUMN nombre_vacuna VARCHAR(150),
    ADD CONSTRAINT ck_agenda_datos_campana CHECK (
        (fecha_atencion IS NOT NULL) = (tipo LIKE 'CAMPANA_%' AND estado='COMPLETADA')
        AND (nombre_vacuna IS NOT NULL) = (tipo='CAMPANA_VACUNACION' AND estado='COMPLETADA')
        AND (animal_id IS NOT NULL OR evento_origen_id IS NULL)
    );

CREATE TABLE sanidad.participante_agenda (
    actividad_id UUID NOT NULL,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    evento_confirmado_id UUID UNIQUE,
    PRIMARY KEY (actividad_id, animal_id),
    FOREIGN KEY (actividad_id,ganaderia_id) REFERENCES sanidad.actividad_agenda(id,ganaderia_id),
    FOREIGN KEY (animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    FOREIGN KEY (evento_confirmado_id,ganaderia_id) REFERENCES sanidad.evento_sanitario(id,ganaderia_id)
);
CREATE INDEX ix_participante_agenda_animal
    ON sanidad.participante_agenda(ganaderia_id, animal_id, actividad_id);

ALTER TABLE sanidad.evento_sanitario DROP CONSTRAINT ck_evento_sanitario_tipo,
    ADD CONSTRAINT ck_evento_sanitario_tipo CHECK (tipo IN (
        'VACUNACION','DESPARASITACION','TRATAMIENTO_SANITARIO','REVISION_VETERINARIA','BANO_ANTIPARASITARIO'));
