ALTER TABLE ganado.animal DROP CONSTRAINT ck_animal_estado,
    ADD CONSTRAINT ck_animal_estado CHECK (estado IN ('ACTIVO','VENDIDO'));
ALTER TABLE catalogo.anuncio DROP CONSTRAINT anuncio_estado_check,
    ADD CONSTRAINT ck_anuncio_estado CHECK (estado IN ('BORRADOR','PUBLICADO','PAUSADO','RETIRADO','CERRADO_VENTA'));

CREATE TABLE ganado.venta (
    operacion_id UUID PRIMARY KEY,
    animal_id UUID NOT NULL UNIQUE,
    ganaderia_id UUID NOT NULL,
    actor_id UUID NOT NULL REFERENCES identidad.usuario(id),
    registrada_en TIMESTAMPTZ NOT NULL,
    anuncios_cerrados INT NOT NULL DEFAULT 0 CHECK(anuncios_cerrados>=0),
    FOREIGN KEY (animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id)
);

CREATE TABLE identidad.permiso_moderacion (
    usuario_id UUID PRIMARY KEY REFERENCES identidad.usuario(id),
    habilitado BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE catalogo.reporte (
    id UUID PRIMARY KEY,
    anuncio_id UUID NOT NULL REFERENCES catalogo.anuncio(id),
    motivo VARCHAR(1000) NOT NULL CHECK(char_length(motivo) BETWEEN 5 AND 1000),
    reportado_por UUID REFERENCES identidad.usuario(id),
    recibido_en TIMESTAMPTZ NOT NULL,
    estado VARCHAR(18) NOT NULL DEFAULT 'PENDIENTE',
    revisado_por UUID REFERENCES identidad.usuario(id),
    revisado_en TIMESTAMPTZ,
    CHECK ((estado='PENDIENTE' AND revisado_por IS NULL AND revisado_en IS NULL)
        OR (estado IN ('DESESTIMADO','RETIRADO') AND revisado_por IS NOT NULL AND revisado_en IS NOT NULL))
);
CREATE INDEX ix_reporte_bandeja ON catalogo.reporte(estado,recibido_en DESC,id DESC);
