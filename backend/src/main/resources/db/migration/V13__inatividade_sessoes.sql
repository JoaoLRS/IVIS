ALTER TABLE sessoes_autenticacao
    ADD COLUMN ultima_atividade_em TIMESTAMPTZ NOT NULL DEFAULT now();
