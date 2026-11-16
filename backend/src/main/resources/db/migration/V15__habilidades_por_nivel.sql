ALTER TABLE habilidades DROP CONSTRAINT uq_habilidades_aluno_nome;
ALTER TABLE habilidades
    ADD CONSTRAINT uq_habilidades_aluno_nome_nivel
    UNIQUE (codigo_objeto_perfis_aluno, nome_normalizado, nivel);
