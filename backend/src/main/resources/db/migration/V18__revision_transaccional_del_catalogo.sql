CREATE TABLE catalogo.revision_publica (
    unica boolean PRIMARY KEY DEFAULT true CHECK (unica),
    revision uuid NOT NULL DEFAULT gen_random_uuid()
);
INSERT INTO catalogo.revision_publica DEFAULT VALUES;

CREATE FUNCTION catalogo.renovar_revision_publica() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE catalogo.revision_publica SET revision = gen_random_uuid() WHERE unica;
    RETURN NULL;
END;
$$;

CREATE TRIGGER revision_anuncio
AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON catalogo.anuncio
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_foto
AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON catalogo.foto
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_parentesco
AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON reproduccion.parentesco
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_animal
AFTER UPDATE OF estado, categoria, raza, sexo ON ganado.animal
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_ganaderia
AFTER UPDATE OF estado ON identidad.ganaderia
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
