ALTER TABLE perguntas_entrevista
    ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'LEGADO';

ALTER TABLE perguntas_entrevista
    ADD CONSTRAINT ck_perguntas_origem CHECK (origem IN ('LEGADO', 'IA', 'CATALOGO'));

CREATE UNIQUE INDEX uq_treinamentos_aluno_em_andamento
    ON treinamentos (codigo_objeto_perfis_aluno)
    WHERE situacao = 'EM_ANDAMENTO';
