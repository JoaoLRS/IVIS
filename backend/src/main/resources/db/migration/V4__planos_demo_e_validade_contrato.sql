ALTER TABLE planos
    ADD COLUMN duracao_dias INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT ck_planos_duracao_dias CHECK (duracao_dias BETWEEN 1 AND 3650);

ALTER TABLE contratos
    ADD COLUMN expira_em TIMESTAMPTZ,
    ADD CONSTRAINT ck_contratos_expiracao CHECK (expira_em IS NULL OR expira_em >= iniciado_em);

CREATE TABLE emails_simulados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_id UUID NOT NULL,
    contrato_id UUID NOT NULL,
    pagamento_id UUID NOT NULL,
    destinatario VARCHAR(254) NOT NULL,
    assunto VARCHAR(200) NOT NULL,
    corpo TEXT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_emails_simulados_conta FOREIGN KEY (conta_id)
        REFERENCES contas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_emails_simulados_contrato FOREIGN KEY (contrato_id)
        REFERENCES contratos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_emails_simulados_pagamento FOREIGN KEY (pagamento_id)
        REFERENCES pagamentos (id) ON DELETE RESTRICT,
    CONSTRAINT uq_emails_simulados_pagamento UNIQUE (pagamento_id)
);

CREATE INDEX ix_emails_simulados_conta_data
    ON emails_simulados (conta_id, criado_em DESC);

INSERT INTO planos (
    codigo,
    modalidade,
    nome,
    descricao,
    preco_centavos,
    moeda,
    ativo,
    duracao_dias
)
VALUES (
    'IVIS-DEMO-INDIVIDUAL',
    'INDIVIDUAL',
    'Plano Individual IVIS',
    'Acesso individual por 30 dias. Valor ilustrativo para a demonstração acadêmica; nenhuma cobrança real é realizada.',
    1990,
    'BRL',
    true,
    30
)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO beneficios_plano (plano_id, texto_beneficio, ordem)
SELECT p.id, beneficio.texto, beneficio.ordem
FROM planos p
CROSS JOIN (VALUES
    ('Treinamentos técnicos por texto', 1),
    ('Contexto por habilidades ou vaga de interesse', 2),
    ('Feedback de demonstração ao final do treinamento', 3),
    ('Acesso individual durante 30 dias', 4)
) AS beneficio(texto, ordem)
WHERE p.codigo = 'IVIS-DEMO-INDIVIDUAL'
ON CONFLICT (plano_id, ordem) DO NOTHING;
