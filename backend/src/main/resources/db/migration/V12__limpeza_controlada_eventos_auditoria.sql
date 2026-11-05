CREATE OR REPLACE FUNCTION impedir_alteracao_evento_auditoria() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND current_setting('ivis.limpeza_dados', true) = 'autorizada' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'Eventos de auditoria não podem ser alterados ou removidos.'
        USING ERRCODE = '42501';
END;
$$;
