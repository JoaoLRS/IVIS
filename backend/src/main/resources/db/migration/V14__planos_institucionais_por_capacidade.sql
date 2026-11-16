-- NULL representa capacidade ilimitada; contratos existentes mantêm a capacidade original.
ALTER TABLE planos ALTER COLUMN vagas_incluidas DROP NOT NULL;
ALTER TABLE planos DROP CONSTRAINT ck_planos_vagas_incluidas;
ALTER TABLE planos ADD CONSTRAINT ck_planos_vagas_incluidas
    CHECK ((modalidade = 'INSTITUCIONAL' AND vagas_incluidas IS NULL)
        OR (vagas_incluidas BETWEEN 1 AND 10000));

ALTER TABLE contratos ALTER COLUMN quantidade_vagas DROP NOT NULL;

UPDATE planos SET ativo = false, atualizado_em = now()
WHERE codigo = 'IVIS-DEMO-ACADEMICO';

INSERT INTO planos (codigo, modalidade, nome, descricao, preco_centavos, moeda, ativo, duracao_dias, vagas_incluidas)
VALUES
    ('IVIS-DEMO-INSTITUCIONAL-50', 'INSTITUCIONAL', 'Institucional 50 alunos',
     'Acesso institucional de demonstração por 30 dias para até 50 alunos. Nenhuma cobrança real é realizada.',
     90000, 'BRL', true, 30, 50),
    ('IVIS-DEMO-INSTITUCIONAL-100', 'INSTITUCIONAL', 'Institucional 100 alunos',
     'Acesso institucional de demonstração por 30 dias para até 100 alunos. Nenhuma cobrança real é realizada.',
     175000, 'BRL', true, 30, 100),
    ('IVIS-DEMO-INSTITUCIONAL-ILIMITADO', 'INSTITUCIONAL', 'Institucional ilimitado',
     'Acesso institucional de demonstração por 30 dias sem limite de alunos. Nenhuma cobrança real é realizada.',
     500000, 'BRL', true, 30, NULL)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO beneficios_plano (codigo_objeto_planos, texto_beneficio, ordem)
SELECT p.objeto, beneficio.texto, beneficio.ordem
FROM planos p
CROSS JOIN LATERAL (VALUES
    ('Acesso institucional por 30 dias', 1),
    (CASE WHEN p.vagas_incluidas IS NULL THEN 'Alunos ilimitados'
          ELSE 'Até ' || p.vagas_incluidas || ' alunos vinculados' END, 2),
    ('Gestão de vínculos pela instituição', 3),
    ('Treinamentos e feedback de demonstração', 4)
) AS beneficio(texto, ordem)
WHERE p.codigo IN ('IVIS-DEMO-INSTITUCIONAL-50', 'IVIS-DEMO-INSTITUCIONAL-100',
                   'IVIS-DEMO-INSTITUCIONAL-ILIMITADO')
ON CONFLICT (codigo_objeto_planos, ordem) DO NOTHING;
