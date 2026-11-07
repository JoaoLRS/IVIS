ALTER TABLE avaliacoes_treinamento
    ALTER COLUMN nota DROP NOT NULL;

ALTER TABLE avaliacoes_treinamento
    ADD CONSTRAINT ck_avaliacao_nota_qualitativa CHECK (
        avaliacao_qualitativa = FALSE OR nota IS NOT NULL
    );
