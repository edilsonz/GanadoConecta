ALTER TABLE catalogo.revision_publica ADD COLUMN ultima_transaccion xid8;

CREATE OR REPLACE FUNCTION catalogo.renovar_revision_publica() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE catalogo.revision_publica
       SET revision = gen_random_uuid(), ultima_transaccion = pg_current_xact_id()
     WHERE unica AND ultima_transaccion IS DISTINCT FROM pg_current_xact_id();
    RETURN NULL;
END;
$$;

-- El bloqueo común se adquiere al confirmar, después de las escrituras
-- de negocio. Una venta puede bloquear el animal antes del anuncio.
DROP TRIGGER revision_anuncio ON catalogo.anuncio;
DROP TRIGGER revision_foto ON catalogo.foto;
DROP TRIGGER revision_parentesco ON reproduccion.parentesco;
DROP TRIGGER revision_animal ON ganado.animal;
DROP TRIGGER revision_ganaderia ON identidad.ganaderia;

CREATE CONSTRAINT TRIGGER revision_anuncio
AFTER INSERT OR UPDATE OR DELETE ON catalogo.anuncio
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE CONSTRAINT TRIGGER revision_foto
AFTER INSERT OR UPDATE OR DELETE ON catalogo.foto
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE CONSTRAINT TRIGGER revision_parentesco
AFTER INSERT OR UPDATE OR DELETE ON reproduccion.parentesco
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE CONSTRAINT TRIGGER revision_animal
AFTER UPDATE OF estado, categoria, raza, sexo ON ganado.animal
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE CONSTRAINT TRIGGER revision_ganaderia
AFTER UPDATE OF estado ON identidad.ganaderia
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION catalogo.renovar_revision_publica();

CREATE TRIGGER revision_truncar_anuncio AFTER TRUNCATE ON catalogo.anuncio
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_truncar_foto AFTER TRUNCATE ON catalogo.foto
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
CREATE TRIGGER revision_truncar_parentesco AFTER TRUNCATE ON reproduccion.parentesco
FOR EACH STATEMENT EXECUTE FUNCTION catalogo.renovar_revision_publica();
