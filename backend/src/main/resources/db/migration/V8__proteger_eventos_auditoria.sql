CREATE FUNCTION impedir_alteracao_evento_auditoria() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Eventos de auditoria não podem ser alterados ou removidos.'
        USING ERRCODE = '42501';
END;
$$;

CREATE TRIGGER proteger_eventos_auditoria
BEFORE UPDATE OR DELETE ON eventos_auditoria
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_evento_auditoria();
