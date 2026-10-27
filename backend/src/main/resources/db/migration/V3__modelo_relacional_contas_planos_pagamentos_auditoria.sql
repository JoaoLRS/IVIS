-- Complementa V2 com as relações gerais do produto: autenticação,
-- catálogo/contratos, vínculos institucionais, pagamentos e auditoria.

CREATE TABLE tokens_recuperacao_senha (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_id UUID NOT NULL,
    hash_token VARCHAR(255) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    consumido_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tokens_recuperacao_conta FOREIGN KEY (conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT uq_tokens_recuperacao_hash UNIQUE (hash_token),
    CONSTRAINT ck_tokens_recuperacao_expiracao CHECK (expira_em > criado_em),
    CONSTRAINT ck_tokens_recuperacao_consumo CHECK (consumido_em IS NULL OR consumido_em >= criado_em)
);

CREATE INDEX ix_tokens_recuperacao_conta_expiracao
    ON tokens_recuperacao_senha (conta_id, expira_em DESC);

CREATE TABLE sessoes_autenticacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_id UUID NOT NULL,
    hash_token_renovacao VARCHAR(255) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    revogada_em TIMESTAMPTZ,
    substituida_por_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_sessoes_conta FOREIGN KEY (conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sessoes_substituta FOREIGN KEY (substituida_por_id)
        REFERENCES sessoes_autenticacao (id) ON DELETE RESTRICT,
    CONSTRAINT uq_sessoes_hash_renovacao UNIQUE (hash_token_renovacao),
    CONSTRAINT ck_sessoes_expiracao CHECK (expira_em > criado_em),
    CONSTRAINT ck_sessoes_revogacao CHECK (revogada_em IS NULL OR revogada_em >= criado_em)
);

CREATE INDEX ix_sessoes_conta_ativas
    ON sessoes_autenticacao (conta_id, expira_em DESC)
    WHERE revogada_em IS NULL;

CREATE TABLE planos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(60) NOT NULL,
    modalidade VARCHAR(20) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    preco_centavos BIGINT NOT NULL,
    moeda CHAR(3) NOT NULL DEFAULT 'BRL',
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_planos_codigo UNIQUE (codigo),
    CONSTRAINT ck_planos_modalidade CHECK (modalidade IN ('INDIVIDUAL', 'INSTITUCIONAL')),
    CONSTRAINT ck_planos_preco CHECK (preco_centavos >= 0),
    CONSTRAINT ck_planos_moeda CHECK (moeda ~ '^[A-Z]{3}$')
);

CREATE TABLE beneficios_plano (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plano_id UUID NOT NULL,
    texto_beneficio VARCHAR(500) NOT NULL,
    ordem SMALLINT NOT NULL,
    CONSTRAINT fk_beneficios_plano FOREIGN KEY (plano_id)
        REFERENCES planos (id) ON DELETE RESTRICT,
    CONSTRAINT ck_beneficios_ordem CHECK (ordem >= 1),
    CONSTRAINT uq_beneficios_plano_ordem UNIQUE (plano_id, ordem)
);

CREATE TABLE contratos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_titular_id UUID NOT NULL,
    plano_id UUID NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    quantidade_vagas INTEGER NOT NULL DEFAULT 1,
    preco_centavos_contratado BIGINT NOT NULL,
    moeda CHAR(3) NOT NULL DEFAULT 'BRL',
    iniciado_em TIMESTAMPTZ NOT NULL,
    encerrado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_contratos_titular FOREIGN KEY (conta_titular_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contratos_plano FOREIGN KEY (plano_id)
        REFERENCES planos (id) ON DELETE RESTRICT,
    CONSTRAINT ck_contratos_situacao CHECK (situacao IN ('PENDENTE', 'ATIVO', 'SUSPENSO', 'ENCERRADO')),
    CONSTRAINT ck_contratos_vagas CHECK (quantidade_vagas >= 1),
    CONSTRAINT ck_contratos_preco CHECK (preco_centavos_contratado >= 0),
    CONSTRAINT ck_contratos_moeda CHECK (moeda ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_contratos_datas CHECK (encerrado_em IS NULL OR encerrado_em >= iniciado_em)
);

CREATE INDEX ix_contratos_titular_situacao
    ON contratos (conta_titular_id, situacao);

CREATE TABLE vinculos_institucionais (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contrato_id UUID NOT NULL,
    conta_aluno_id UUID NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    vinculado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    desvinculado_em TIMESTAMPTZ,
    criado_por_conta_id UUID NOT NULL,
    CONSTRAINT fk_vinculos_contrato FOREIGN KEY (contrato_id)
        REFERENCES contratos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_vinculos_aluno FOREIGN KEY (conta_aluno_id)
        REFERENCES perfis_aluno (conta_id) ON DELETE RESTRICT,
    CONSTRAINT fk_vinculos_criador FOREIGN KEY (criado_por_conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT ck_vinculos_situacao CHECK (situacao IN ('ATIVO', 'ENCERRADO')),
    CONSTRAINT ck_vinculos_datas CHECK (desvinculado_em IS NULL OR desvinculado_em >= vinculado_em)
);

CREATE INDEX ix_vinculos_contrato_situacao
    ON vinculos_institucionais (contrato_id, situacao);

CREATE INDEX ix_vinculos_aluno_situacao
    ON vinculos_institucionais (conta_aluno_id, situacao);

CREATE UNIQUE INDEX uq_vinculos_aluno_ativo
    ON vinculos_institucionais (conta_aluno_id)
    WHERE situacao = 'ATIVO';

CREATE TABLE pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contrato_id UUID NOT NULL,
    modo VARCHAR(20) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    valor_centavos BIGINT NOT NULL,
    moeda CHAR(3) NOT NULL DEFAULT 'BRL',
    metodo VARCHAR(40),
    referencia_provedor VARCHAR(200),
    chave_idempotencia VARCHAR(120) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmado_em TIMESTAMPTZ,
    CONSTRAINT fk_pagamentos_contrato FOREIGN KEY (contrato_id)
        REFERENCES contratos (id) ON DELETE RESTRICT,
    CONSTRAINT uq_pagamentos_idempotencia UNIQUE (chave_idempotencia),
    CONSTRAINT ck_pagamentos_modo CHECK (modo IN ('SIMULADO', 'REAL')),
    CONSTRAINT ck_pagamentos_situacao CHECK (situacao IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'CANCELADO', 'ESTORNADO')),
    CONSTRAINT ck_pagamentos_valor CHECK (valor_centavos >= 0),
    CONSTRAINT ck_pagamentos_moeda CHECK (moeda ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_pagamentos_confirmacao CHECK (confirmado_em IS NULL OR confirmado_em >= criado_em)
);

CREATE INDEX ix_pagamentos_contrato_criacao
    ON pagamentos (contrato_id, criado_em DESC);

CREATE TABLE eventos_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ator_conta_id UUID,
    acao VARCHAR(100) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    tipo_recurso VARCHAR(80) NOT NULL,
    recurso_id UUID,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    detalhes JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_auditoria_ator FOREIGN KEY (ator_conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('SUCESSO', 'FALHA')),
    CONSTRAINT ck_auditoria_detalhes_objeto CHECK (jsonb_typeof(detalhes) = 'object')
);

CREATE INDEX ix_auditoria_recurso_data
    ON eventos_auditoria (tipo_recurso, recurso_id, ocorrido_em DESC);

CREATE INDEX ix_auditoria_ator_data
    ON eventos_auditoria (ator_conta_id, ocorrido_em DESC);
