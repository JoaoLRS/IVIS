ALTER TABLE avaliacoes_treinamento
    ADD COLUMN avaliacao_qualitativa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN nivel_confianca VARCHAR(5) NOT NULL DEFAULT 'BAIXA',
    ADD COLUMN limitacoes VARCHAR(1000) NOT NULL
        DEFAULT 'Avaliação legada sem detalhamento por pergunta.',
    ADD COLUMN avaliacoes_por_pergunta JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD CONSTRAINT ck_avaliacoes_nivel_confianca
        CHECK (nivel_confianca IN ('BAIXA', 'MEDIA', 'ALTA'));
