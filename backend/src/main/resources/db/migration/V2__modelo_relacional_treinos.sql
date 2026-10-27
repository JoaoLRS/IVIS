-- Primeira fatia persistente do IVIS: identidade mínima, contexto e ciclo Q&A.
-- O backend é o único módulo que deve ler/gravar estas tabelas.

CREATE TABLE contas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_conta VARCHAR(20) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_normalizado VARCHAR(320) NOT NULL,
    hash_senha VARCHAR(255) NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_contas_tipo CHECK (tipo_conta IN ('ALUNO', 'INSTITUICAO')),
    CONSTRAINT ck_contas_situacao CHECK (situacao IN ('ATIVA', 'INATIVA')),
    CONSTRAINT ck_contas_email_normalizado CHECK (email_normalizado = lower(trim(email_normalizado))),
    CONSTRAINT uq_contas_email_normalizado UNIQUE (email_normalizado)
);

CREATE TABLE perfis_aluno (
    conta_id UUID PRIMARY KEY,
    nome_exibicao VARCHAR(160) NOT NULL,
    url_foto VARCHAR(2048),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_perfis_aluno_conta FOREIGN KEY (conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT
);

CREATE TABLE perfis_instituicao (
    conta_id UUID PRIMARY KEY,
    razao_social VARCHAR(200) NOT NULL,
    documento_fiscal VARCHAR(32),
    url_logo VARCHAR(2048),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_perfis_instituicao_conta FOREIGN KEY (conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT uq_perfis_instituicao_documento UNIQUE (documento_fiscal)
);

CREATE TABLE habilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_aluno_id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    nome_normalizado VARCHAR(120) NOT NULL,
    categoria VARCHAR(80),
    nivel SMALLINT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_habilidades_perfil FOREIGN KEY (conta_aluno_id)
        REFERENCES perfis_aluno (conta_id) ON DELETE RESTRICT,
    CONSTRAINT ck_habilidades_nivel CHECK (nivel BETWEEN 1 AND 5),
    CONSTRAINT ck_habilidades_nome_normalizado CHECK (nome_normalizado = lower(trim(nome_normalizado))),
    CONSTRAINT uq_habilidades_aluno_nome UNIQUE (conta_aluno_id, nome_normalizado)
);

CREATE TABLE vagas_interesse (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_aluno_id UUID NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    empresa VARCHAR(200),
    senioridade VARCHAR(80) NOT NULL,
    descricao TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    arquivado_em TIMESTAMPTZ,
    CONSTRAINT fk_vagas_perfil FOREIGN KEY (conta_aluno_id)
        REFERENCES perfis_aluno (conta_id) ON DELETE RESTRICT
);

CREATE INDEX ix_vagas_aluno_ativas
    ON vagas_interesse (conta_aluno_id, criado_em DESC)
    WHERE arquivado_em IS NULL;

CREATE TABLE requisitos_vaga (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vaga_interesse_id UUID NOT NULL,
    texto_requisito VARCHAR(500) NOT NULL,
    ordem SMALLINT NOT NULL,
    CONSTRAINT fk_requisitos_vaga FOREIGN KEY (vaga_interesse_id)
        REFERENCES vagas_interesse (id) ON DELETE RESTRICT,
    CONSTRAINT ck_requisitos_vaga_ordem CHECK (ordem >= 1),
    CONSTRAINT uq_requisitos_vaga_ordem UNIQUE (vaga_interesse_id, ordem)
);

CREATE TABLE treinamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_aluno_id UUID NOT NULL,
    vaga_interesse_id UUID,
    tipo_contexto VARCHAR(20) NOT NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'EM_ANDAMENTO',
    contexto_registrado JSONB NOT NULL,
    iniciado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_treinamentos_perfil FOREIGN KEY (conta_aluno_id)
        REFERENCES perfis_aluno (conta_id) ON DELETE RESTRICT,
    CONSTRAINT fk_treinamentos_vaga FOREIGN KEY (vaga_interesse_id)
        REFERENCES vagas_interesse (id) ON DELETE RESTRICT,
    CONSTRAINT ck_treinamentos_tipo_contexto CHECK (tipo_contexto IN ('HABILIDADES', 'VAGA')),
    CONSTRAINT ck_treinamentos_situacao CHECK (situacao IN ('EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_treinamentos_contexto_objeto CHECK (jsonb_typeof(contexto_registrado) = 'object'),
    CONSTRAINT ck_treinamentos_vaga_contexto CHECK (
        (tipo_contexto = 'VAGA' AND vaga_interesse_id IS NOT NULL)
        OR (tipo_contexto = 'HABILIDADES' AND vaga_interesse_id IS NULL)
    ),
    CONSTRAINT ck_treinamentos_datas CHECK (finalizado_em IS NULL OR finalizado_em >= iniciado_em)
);

CREATE INDEX ix_treinamentos_aluno_inicio
    ON treinamentos (conta_aluno_id, iniciado_em DESC);

CREATE TABLE perguntas_entrevista (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    treinamento_id UUID NOT NULL,
    numero_sequencial SMALLINT NOT NULL,
    texto_pergunta VARCHAR(1000) NOT NULL,
    versao_modelo VARCHAR(120) NOT NULL,
    versao_prompt VARCHAR(80) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_perguntas_treinamento FOREIGN KEY (treinamento_id)
        REFERENCES treinamentos (id) ON DELETE RESTRICT,
    CONSTRAINT ck_perguntas_numero CHECK (numero_sequencial BETWEEN 1 AND 10),
    CONSTRAINT uq_perguntas_treinamento_numero UNIQUE (treinamento_id, numero_sequencial)
);

CREATE TABLE respostas_entrevista (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pergunta_id UUID NOT NULL,
    texto_resposta VARCHAR(4000) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_respostas_pergunta FOREIGN KEY (pergunta_id)
        REFERENCES perguntas_entrevista (id) ON DELETE RESTRICT,
    CONSTRAINT uq_respostas_pergunta UNIQUE (pergunta_id),
    CONSTRAINT ck_respostas_texto CHECK (length(trim(texto_resposta)) > 0)
);

CREATE TABLE avaliacoes_treinamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    treinamento_id UUID NOT NULL,
    nota SMALLINT NOT NULL,
    versao_modelo VARCHAR(120) NOT NULL,
    versao_prompt VARCHAR(80) NOT NULL,
    gerado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_avaliacoes_treinamento FOREIGN KEY (treinamento_id)
        REFERENCES treinamentos (id) ON DELETE RESTRICT,
    CONSTRAINT uq_avaliacoes_treinamento UNIQUE (treinamento_id),
    CONSTRAINT ck_avaliacoes_nota CHECK (nota BETWEEN 0 AND 100)
);

CREATE TABLE itens_feedback_treinamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    avaliacao_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    ordem SMALLINT NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_itens_feedback_avaliacao FOREIGN KEY (avaliacao_id)
        REFERENCES avaliacoes_treinamento (id) ON DELETE RESTRICT,
    CONSTRAINT ck_itens_feedback_tipo CHECK (tipo IN ('PONTO_FORTE', 'PONTO_MELHORIA')),
    CONSTRAINT ck_itens_feedback_ordem CHECK (ordem BETWEEN 1 AND 5),
    CONSTRAINT uq_itens_feedback_ordem UNIQUE (avaliacao_id, tipo, ordem)
);
