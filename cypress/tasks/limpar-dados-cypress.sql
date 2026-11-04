-- A lista contas_cypress contém exclusivamente os e-mails das fixtures reais.
-- A transação é aberta pelo chamador: qualquer falha reverte toda a limpeza.
SET LOCAL ivis.limpeza_dados = 'autorizada';
DELETE FROM eventos_auditoria
WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress)
   OR codigo_objeto_contratos IN (SELECT objeto FROM contratos WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress))
   OR detalhes ->> 'alunoId' IN (SELECT objeto::text FROM contas_cypress);
DELETE FROM emails_simulados
WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress)
   OR codigo_objeto_contratos IN (SELECT objeto FROM contratos WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress));
DELETE FROM pagamentos WHERE codigo_objeto_contratos IN
  (SELECT objeto FROM contratos WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress));
DELETE FROM vinculos_institucionais
WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress)
   OR codigo_objeto_contas IN (SELECT objeto FROM contas_cypress)
   OR codigo_objeto_contratos IN (SELECT objeto FROM contratos WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress));
UPDATE perfis_aluno SET codigo_objeto_perfis_instituicao = NULL
WHERE codigo_objeto_perfis_instituicao IN (SELECT objeto FROM contas_cypress);
DELETE FROM itens_feedback_treinamento WHERE codigo_objeto_avaliacoes_treinamento IN
  (SELECT a.objeto FROM avaliacoes_treinamento a JOIN treinamentos t ON t.objeto=a.codigo_objeto_treinamentos
   WHERE t.codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress));
DELETE FROM avaliacoes_treinamento WHERE codigo_objeto_treinamentos IN
  (SELECT objeto FROM treinamentos WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress));
DELETE FROM respostas_entrevista WHERE codigo_objeto_perguntas_entrevista IN
  (SELECT p.objeto FROM perguntas_entrevista p JOIN treinamentos t ON t.objeto=p.codigo_objeto_treinamentos
   WHERE t.codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress));
DELETE FROM perguntas_entrevista WHERE codigo_objeto_treinamentos IN
  (SELECT objeto FROM treinamentos WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress));
DELETE FROM treinamentos WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress);
DELETE FROM requisitos_vaga WHERE codigo_objeto_vagas_interesse IN
  (SELECT objeto FROM vagas_interesse WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress));
DELETE FROM vagas_interesse WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress);
DELETE FROM habilidades WHERE codigo_objeto_perfis_aluno IN (SELECT objeto FROM contas_cypress);
DELETE FROM tokens_recuperacao_senha WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress);
UPDATE sessoes_autenticacao SET codigo_objeto_sessoes_autenticacao = NULL
WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress);
DELETE FROM sessoes_autenticacao WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress);
DELETE FROM contratos WHERE codigo_objeto_contas IN (SELECT objeto FROM contas_cypress);
DELETE FROM perfis_aluno WHERE objeto IN (SELECT objeto FROM contas_cypress);
DELETE FROM perfis_instituicao WHERE objeto IN (SELECT objeto FROM contas_cypress);
DELETE FROM contas WHERE objeto IN (SELECT objeto FROM contas_cypress);
