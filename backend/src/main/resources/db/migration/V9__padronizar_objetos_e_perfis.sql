-- Padroniza as chaves primarias e os nomes das relacoes sem descartar dados.
-- Os perfis compartilham o objeto da conta para manter a relacao 1:1 existente.

ALTER TABLE contas RENAME COLUMN id TO objeto;

ALTER TABLE perfis_aluno RENAME COLUMN conta_id TO objeto;
ALTER TABLE perfis_aluno DROP CONSTRAINT fk_perfis_aluno_conta;
ALTER TABLE perfis_aluno RENAME COLUMN nome_exibicao TO nome;
ALTER TABLE perfis_aluno
    ADD COLUMN codigo_objeto_contas UUID,
    ADD COLUMN cpf VARCHAR(11),
    ADD COLUMN telefone VARCHAR(30),
    ADD COLUMN endereco VARCHAR(500),
    ADD COLUMN codigo_objeto_perfis_instituicao UUID;
UPDATE perfis_aluno SET codigo_objeto_contas = objeto;
ALTER TABLE perfis_aluno
    ALTER COLUMN codigo_objeto_contas SET NOT NULL,
    ADD CONSTRAINT uq_perfis_aluno_conta UNIQUE (codigo_objeto_contas),
    ADD CONSTRAINT ck_perfis_aluno_conta_compartilhada CHECK (objeto = codigo_objeto_contas),
    ADD CONSTRAINT fk_perfis_aluno_conta FOREIGN KEY (codigo_objeto_contas)
        REFERENCES contas (objeto) ON DELETE RESTRICT;

ALTER TABLE perfis_instituicao RENAME COLUMN conta_id TO objeto;
ALTER TABLE perfis_instituicao DROP CONSTRAINT fk_perfis_instituicao_conta;
ALTER TABLE perfis_instituicao DROP CONSTRAINT uq_perfis_instituicao_documento;
ALTER TABLE perfis_instituicao RENAME COLUMN razao_social TO nome_instituicao;
ALTER TABLE perfis_instituicao RENAME COLUMN nome_administrador TO nome_responsavel;
ALTER TABLE perfis_instituicao RENAME COLUMN documento_fiscal TO cnpj;
ALTER TABLE perfis_instituicao
    ADD COLUMN codigo_objeto_contas UUID,
    ADD COLUMN telefone VARCHAR(30),
    ADD COLUMN endereco VARCHAR(500);
UPDATE perfis_instituicao SET codigo_objeto_contas = objeto;
ALTER TABLE perfis_instituicao
    ALTER COLUMN codigo_objeto_contas SET NOT NULL,
    ADD CONSTRAINT uq_perfis_instituicao_conta UNIQUE (codigo_objeto_contas),
    ADD CONSTRAINT ck_perfis_instituicao_conta_compartilhada CHECK (objeto = codigo_objeto_contas),
    ADD CONSTRAINT fk_perfis_instituicao_conta FOREIGN KEY (codigo_objeto_contas)
        REFERENCES contas (objeto) ON DELETE RESTRICT;

-- Normaliza documentos legados. Valores incompletos/duplicados não podem ser
-- tratados como documentos válidos; ficam nulos para atualização pelo usuário.
UPDATE perfis_instituicao
SET cnpj = CASE
    WHEN length(regexp_replace(coalesce(cnpj, ''), '[^0-9]', '', 'g')) = 14
    THEN regexp_replace(cnpj, '[^0-9]', '', 'g')
    ELSE NULL
END;
WITH duplicados AS (
    SELECT objeto, row_number() OVER (PARTITION BY cnpj ORDER BY objeto) AS ordem
    FROM perfis_instituicao
    WHERE cnpj IS NOT NULL
)
UPDATE perfis_instituicao p
SET cnpj = NULL
FROM duplicados d
WHERE p.objeto = d.objeto AND d.ordem > 1;
ALTER TABLE perfis_instituicao ALTER COLUMN cnpj TYPE VARCHAR(14);
CREATE UNIQUE INDEX uq_perfis_instituicao_cnpj
    ON perfis_instituicao (cnpj) WHERE cnpj IS NOT NULL;

ALTER TABLE habilidades RENAME COLUMN id TO objeto;
ALTER TABLE habilidades RENAME COLUMN conta_aluno_id TO codigo_objeto_perfis_aluno;

ALTER TABLE vagas_interesse RENAME COLUMN id TO objeto;
ALTER TABLE vagas_interesse RENAME COLUMN conta_aluno_id TO codigo_objeto_perfis_aluno;

ALTER TABLE requisitos_vaga RENAME COLUMN id TO objeto;
ALTER TABLE requisitos_vaga RENAME COLUMN vaga_interesse_id TO codigo_objeto_vagas_interesse;

ALTER TABLE treinamentos RENAME COLUMN id TO objeto;
ALTER TABLE treinamentos RENAME COLUMN conta_aluno_id TO codigo_objeto_perfis_aluno;
ALTER TABLE treinamentos RENAME COLUMN vaga_interesse_id TO codigo_objeto_vagas_interesse;

ALTER TABLE perguntas_entrevista RENAME COLUMN id TO objeto;
ALTER TABLE perguntas_entrevista RENAME COLUMN treinamento_id TO codigo_objeto_treinamentos;

ALTER TABLE respostas_entrevista RENAME COLUMN id TO objeto;
ALTER TABLE respostas_entrevista RENAME COLUMN pergunta_id TO codigo_objeto_perguntas_entrevista;

ALTER TABLE avaliacoes_treinamento RENAME COLUMN id TO objeto;
ALTER TABLE avaliacoes_treinamento RENAME COLUMN treinamento_id TO codigo_objeto_treinamentos;

ALTER TABLE itens_feedback_treinamento RENAME COLUMN id TO objeto;
ALTER TABLE itens_feedback_treinamento RENAME COLUMN avaliacao_id TO codigo_objeto_avaliacoes_treinamento;

ALTER TABLE tokens_recuperacao_senha RENAME COLUMN id TO objeto;
ALTER TABLE tokens_recuperacao_senha RENAME COLUMN conta_id TO codigo_objeto_contas;

ALTER TABLE sessoes_autenticacao RENAME COLUMN id TO objeto;
ALTER TABLE sessoes_autenticacao RENAME COLUMN conta_id TO codigo_objeto_contas;
ALTER TABLE sessoes_autenticacao RENAME COLUMN substituida_por_id TO codigo_objeto_sessoes_autenticacao;

ALTER TABLE planos RENAME COLUMN id TO objeto;
ALTER TABLE beneficios_plano RENAME COLUMN id TO objeto;
ALTER TABLE beneficios_plano RENAME COLUMN plano_id TO codigo_objeto_planos;

ALTER TABLE contratos RENAME COLUMN id TO objeto;
ALTER TABLE contratos RENAME COLUMN conta_titular_id TO codigo_objeto_contas;
ALTER TABLE contratos RENAME COLUMN plano_id TO codigo_objeto_planos;

ALTER TABLE vinculos_institucionais RENAME COLUMN id TO objeto;
ALTER TABLE vinculos_institucionais RENAME COLUMN contrato_id TO codigo_objeto_contratos;
ALTER TABLE vinculos_institucionais RENAME COLUMN conta_aluno_id TO codigo_objeto_perfis_aluno;
ALTER TABLE vinculos_institucionais RENAME COLUMN criado_por_conta_id TO codigo_objeto_contas;

ALTER TABLE pagamentos RENAME COLUMN id TO objeto;
ALTER TABLE pagamentos RENAME COLUMN contrato_id TO codigo_objeto_contratos;

ALTER TABLE eventos_auditoria RENAME COLUMN id TO objeto;
ALTER TABLE eventos_auditoria RENAME COLUMN ator_conta_id TO codigo_objeto_contas;
DROP INDEX ix_auditoria_recurso_data;
ALTER TABLE eventos_auditoria RENAME COLUMN recurso_id TO codigo_objeto_recurso_legado;
ALTER TABLE eventos_auditoria ADD COLUMN codigo_objeto_contratos UUID;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM eventos_auditoria
        WHERE codigo_objeto_recurso_legado IS NOT NULL
          AND tipo_recurso <> 'CONTRATO'
    ) THEN
        RAISE EXCEPTION 'A V7 requer mapear os recursos de auditoria legados que não sejam CONTRATO.';
    END IF;
END;
$$;
UPDATE eventos_auditoria
SET codigo_objeto_contratos = codigo_objeto_recurso_legado
WHERE tipo_recurso = 'CONTRATO';
ALTER TABLE eventos_auditoria DROP COLUMN codigo_objeto_recurso_legado;
ALTER TABLE eventos_auditoria
    ADD CONSTRAINT fk_auditoria_contrato FOREIGN KEY (codigo_objeto_contratos)
        REFERENCES contratos (objeto) ON DELETE RESTRICT;
CREATE INDEX ix_auditoria_recurso_data
    ON eventos_auditoria (tipo_recurso, codigo_objeto_contratos, ocorrido_em DESC);

ALTER TABLE emails_simulados RENAME COLUMN id TO objeto;
ALTER TABLE emails_simulados RENAME COLUMN conta_id TO codigo_objeto_contas;
ALTER TABLE emails_simulados RENAME COLUMN contrato_id TO codigo_objeto_contratos;
ALTER TABLE emails_simulados RENAME COLUMN pagamento_id TO codigo_objeto_pagamentos;

ALTER TABLE perfis_aluno
    ADD CONSTRAINT fk_perfis_aluno_instituicao FOREIGN KEY (codigo_objeto_perfis_instituicao)
        REFERENCES perfis_instituicao (objeto) ON DELETE RESTRICT;

WITH vinculo_atual AS (
    SELECT DISTINCT ON (v.codigo_objeto_perfis_aluno)
           v.codigo_objeto_perfis_aluno, c.codigo_objeto_contas AS objeto_instituicao
    FROM vinculos_institucionais v
    JOIN contratos c ON c.objeto = v.codigo_objeto_contratos
    WHERE v.situacao = 'ATIVO'
    ORDER BY v.codigo_objeto_perfis_aluno, v.vinculado_em DESC
)
UPDATE perfis_aluno a
SET codigo_objeto_perfis_instituicao = v.objeto_instituicao
FROM vinculo_atual v
WHERE a.objeto = v.codigo_objeto_perfis_aluno;

CREATE UNIQUE INDEX uq_perfis_aluno_cpf
    ON perfis_aluno (cpf) WHERE cpf IS NOT NULL;
ALTER TABLE perfis_aluno
    ADD CONSTRAINT ck_perfis_aluno_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$');
ALTER TABLE perfis_instituicao
    ADD CONSTRAINT ck_perfis_instituicao_cnpj CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{14}$');
