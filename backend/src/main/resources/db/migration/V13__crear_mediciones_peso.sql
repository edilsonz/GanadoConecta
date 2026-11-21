CREATE TABLE ganado.medicion_peso (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    kilos NUMERIC(12,3) NOT NULL CHECK (kilos > 0),
    fecha DATE NOT NULL,
    observaciones VARCHAR(2000),
    actor_id UUID NOT NULL REFERENCES identidad.usuario(id),
    registrado_en TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id)
);
CREATE INDEX ix_medicion_peso_historial
    ON ganado.medicion_peso(ganaderia_id,animal_id,fecha,registrado_en,id);
