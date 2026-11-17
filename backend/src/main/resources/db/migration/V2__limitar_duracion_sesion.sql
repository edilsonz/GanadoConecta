ALTER TABLE identidad.sesion_dispositivo
    ADD CONSTRAINT ck_sesion_duracion_maxima
    CHECK (vence_en <= creado_en + INTERVAL '24 hours');
