DROP INDEX ganado.ix_animal_ganaderia_listado;

CREATE INDEX ix_animal_ganaderia_listado
    ON ganado.animal (ganaderia_id, creado_en DESC, id DESC);
