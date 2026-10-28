ALTER TABLE perfis_instituicao
    ADD COLUMN nome_administrador VARCHAR(160);

UPDATE perfis_instituicao
SET nome_administrador = razao_social
WHERE nome_administrador IS NULL;

ALTER TABLE perfis_instituicao
    ALTER COLUMN nome_administrador SET NOT NULL;
