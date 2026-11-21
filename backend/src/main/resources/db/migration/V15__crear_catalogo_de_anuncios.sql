CREATE SCHEMA catalogo;

CREATE TABLE catalogo.anuncio (
    id UUID PRIMARY KEY,
    ganaderia_id UUID NOT NULL,
    animal_id UUID NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    caracteristicas VARCHAR(1000) NOT NULL,
    ubicacion_general VARCHAR(100) NOT NULL,
    precio_pen NUMERIC(12,2) NOT NULL CHECK (precio_pen > 0),
    contacto VARCHAR(180),
    genealogia_autorizada BOOLEAN NOT NULL,
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('BORRADOR','PUBLICADO','PAUSADO','RETIRADO')),
    version BIGINT NOT NULL DEFAULT 1,
    actualizado_en TIMESTAMPTZ NOT NULL,
    publicado_en TIMESTAMPTZ,
    FOREIGN KEY (animal_id,ganaderia_id) REFERENCES ganado.animal(id,ganaderia_id),
    CHECK (estado<>'PUBLICADO' OR publicado_en IS NOT NULL)
);
CREATE INDEX ix_anuncio_publico_orden ON catalogo.anuncio(publicado_en DESC,id DESC)
    WHERE estado='PUBLICADO';
CREATE INDEX ix_anuncio_publico_precio ON catalogo.anuncio(precio_pen)
    WHERE estado='PUBLICADO';
CREATE INDEX ix_anuncio_publico_ubicacion ON catalogo.anuncio(lower(ubicacion_general))
    WHERE estado='PUBLICADO';
CREATE INDEX ix_anuncio_animal ON catalogo.anuncio(ganaderia_id,animal_id,id);
CREATE INDEX ix_animal_catalogo_filtros ON ganado.animal(categoria,sexo,lower(raza),id);

CREATE TABLE catalogo.foto (
    id UUID PRIMARY KEY,
    anuncio_id UUID NOT NULL REFERENCES catalogo.anuncio(id),
    bytes INTEGER NOT NULL CHECK(bytes BETWEEN 1 AND 5242880),
    creada_en TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_foto_anuncio ON catalogo.foto(anuncio_id,creada_en,id);
