package br.ivis.common.development.repository;

import br.ivis.identity.model.ContaModel;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@Profile({"dev & !prod", "homologacao & !prod"})
public interface LimpezaDadosRepository extends Repository<ContaModel, UUID> {
    @Query(value = """
            select c.objeto from contas c join perfis_aluno p on p.objeto=c.objeto
            where p.cpf=:documento for update of c
            """, nativeQuery = true)
    UUID buscarAluno(@Param("documento") String documento);

    @Query(value = """
            select c.objeto from contas c join perfis_instituicao p on p.objeto=c.objeto
            where p.cnpj=:documento for update of c
            """, nativeQuery = true)
    UUID buscarInstituicao(@Param("documento") String documento);

    @Query(value = "select set_config('ivis.limpeza_dados', 'autorizada', true)", nativeQuery = true)
    String autorizarExclusaoAuditoria();

    @Modifying
    @Query(value = """
            delete from eventos_auditoria where codigo_objeto_contas=:id
              or codigo_objeto_contratos in (select objeto from contratos where codigo_objeto_contas=:id)
              or detalhes ->> 'alunoId'=:textoId
            """, nativeQuery = true)
    void excluirAuditoria(@Param("id") UUID id, @Param("textoId") String textoId);

    @Modifying
    @Query(value = """
            delete from emails_simulados where codigo_objeto_contas=:id
              or codigo_objeto_contratos in (select objeto from contratos where codigo_objeto_contas=:id)
            """, nativeQuery = true)
    void excluirEmails(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from pagamentos where codigo_objeto_contratos in
              (select objeto from contratos where codigo_objeto_contas=:id)
            """, nativeQuery = true)
    void excluirPagamentos(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from vinculos_institucionais where codigo_objeto_perfis_aluno=:id
              or codigo_objeto_contas=:id or codigo_objeto_contratos in
                (select objeto from contratos where codigo_objeto_contas=:id)
            """, nativeQuery = true)
    void excluirVinculos(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            update perfis_aluno set codigo_objeto_perfis_instituicao=null
            where codigo_objeto_perfis_instituicao=:id
            """, nativeQuery = true)
    void desvincularAlunos(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from itens_feedback_treinamento where codigo_objeto_avaliacoes_treinamento in
              (select a.objeto from avaliacoes_treinamento a join treinamentos t
               on t.objeto=a.codigo_objeto_treinamentos where t.codigo_objeto_perfis_aluno=:id)
            """, nativeQuery = true)
    void excluirItensFeedback(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from avaliacoes_treinamento where codigo_objeto_treinamentos in
              (select objeto from treinamentos where codigo_objeto_perfis_aluno=:id)
            """, nativeQuery = true)
    void excluirAvaliacoes(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from respostas_entrevista where codigo_objeto_perguntas_entrevista in
              (select p.objeto from perguntas_entrevista p join treinamentos t
               on t.objeto=p.codigo_objeto_treinamentos where t.codigo_objeto_perfis_aluno=:id)
            """, nativeQuery = true)
    void excluirRespostas(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from perguntas_entrevista where codigo_objeto_treinamentos in
              (select objeto from treinamentos where codigo_objeto_perfis_aluno=:id)
            """, nativeQuery = true)
    void excluirPerguntas(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from treinamentos where codigo_objeto_perfis_aluno=:id", nativeQuery = true)
    void excluirTreinos(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            delete from requisitos_vaga where codigo_objeto_vagas_interesse in
              (select objeto from vagas_interesse where codigo_objeto_perfis_aluno=:id)
            """, nativeQuery = true)
    void excluirRequisitos(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from vagas_interesse where codigo_objeto_perfis_aluno=:id", nativeQuery = true)
    void excluirVagas(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from habilidades where codigo_objeto_perfis_aluno=:id", nativeQuery = true)
    void excluirHabilidades(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from tokens_recuperacao_senha where codigo_objeto_contas=:id", nativeQuery = true)
    void excluirTokensRecuperacao(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            update sessoes_autenticacao set codigo_objeto_sessoes_autenticacao=null
            where codigo_objeto_contas=:id
            """, nativeQuery = true)
    void desvincularSessoes(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from sessoes_autenticacao where codigo_objeto_contas=:id", nativeQuery = true)
    void excluirSessoes(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from contratos where codigo_objeto_contas=:id", nativeQuery = true)
    void excluirContratos(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from perfis_aluno where objeto=:id", nativeQuery = true)
    void excluirPerfilAluno(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from perfis_instituicao where objeto=:id", nativeQuery = true)
    void excluirPerfilInstituicao(@Param("id") UUID id);

    @Modifying
    @Query(value = "delete from contas where objeto=:id", nativeQuery = true)
    void excluirConta(@Param("id") UUID id);
}
