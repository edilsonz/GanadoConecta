ALTER TABLE ganado.animal
    ADD COLUMN colores_identificacion_textil VARCHAR(100),
    ADD COLUMN ubicacion_identificacion_textil VARCHAR(12),
    ADD CONSTRAINT ck_animal_colores_identificacion_textil CHECK (
        colores_identificacion_textil IS NULL
        OR CHAR_LENGTH(colores_identificacion_textil) BETWEEN 1 AND 100
    ),
    ADD CONSTRAINT ck_animal_ubicacion_identificacion_textil CHECK (
        ubicacion_identificacion_textil IS NULL
        OR ubicacion_identificacion_textil IN ('IZQUIERDA', 'DERECHA', 'AMBAS')
    ),
    ADD CONSTRAINT ck_animal_identificacion_textil_completa CHECK (
        (colores_identificacion_textil IS NULL)
        = (ubicacion_identificacion_textil IS NULL)
    );

ALTER TABLE ganado.operacion_confirmada
    DROP CONSTRAINT ck_operacion_schema_version,
    ADD CONSTRAINT ck_operacion_schema_version CHECK (schema_version IN (1, 2));
