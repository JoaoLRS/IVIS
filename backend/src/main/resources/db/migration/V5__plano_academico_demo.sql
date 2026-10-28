ALTER TABLE planos
    ADD COLUMN vagas_incluidas INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT ck_planos_vagas_incluidas CHECK (vagas_incluidas BETWEEN 1 AND 10000);

INSERT INTO planos (
    codigo,
    modalidade,
    nome,
    descricao,
    preco_centavos,
    moeda,
    ativo,
    duracao_dias,
    vagas_incluidas
)
VALUES (
    'IVIS-DEMO-ACADEMICO',
    'INSTITUCIONAL',
    'Plano Acadêmico IVIS',
    'Acesso institucional demonstrativo por 30 dias para até 10 alunos. Preço ilustrativo; nenhuma cobrança real é realizada.',
    9990,
    'BRL',
    true,
    30,
    10
)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO beneficios_plano (plano_id, texto_beneficio, ordem)
SELECT p.id, beneficio.texto, beneficio.ordem
FROM planos p
CROSS JOIN (VALUES
    ('Acesso institucional por 30 dias', 1),
    ('Até 10 vagas para alunos vinculados', 2),
    ('Gestão de vínculos pela instituição', 3),
    ('Treinamentos e feedback de demonstração', 4)
) AS beneficio(texto, ordem)
WHERE p.codigo = 'IVIS-DEMO-ACADEMICO'
ON CONFLICT (plano_id, ordem) DO NOTHING;
