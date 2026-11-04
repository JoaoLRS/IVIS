package br.ivis.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identity.dto.VinculoInstitucionalResumo;
import br.ivis.identity.model.VinculoInstitucionalModel;

public interface VinculoInstitucionalRepository extends JpaRepository<VinculoInstitucionalModel, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            INSERT INTO eventos_auditoria (codigo_objeto_contas,acao,resultado,tipo_recurso,codigo_objeto_contratos,detalhes)
            VALUES (:ator,:acao,'SUCESSO','CONTRATO',:contrato,
                jsonb_build_object('vinculoId',cast(:vinculo as text),'alunoId',cast(:aluno as text)))
            """, nativeQuery = true)
    void registrarAuditoria(@Param("ator") UUID ator, @Param("acao") String acao,
            @Param("contrato") UUID contrato, @Param("vinculo") UUID vinculo, @Param("aluno") UUID aluno);

    long countByContratoIdAndSituacao(UUID contratoId, String situacao);

    boolean existsByContaAlunoIdAndSituacao(UUID contaAlunoId, String situacao);

    @Query("select new br.ivis.identity.dto.VinculoInstitucionalResumo("
            + "v.id, v.contratoId, v.contaAlunoId, p.nome, c.email, v.vinculadoEm) "
            + "from VinculoInstitucional v join ContratoInstitucional ct on ct.id = v.contratoId "
            + "join Conta c on c.id = v.contaAlunoId join PerfilAluno p on p.contaId = c.id "
            + "where ct.contaTitularId = :instituicaoId and v.situacao = 'ATIVO' "
            + "order by v.vinculadoEm desc")
    List<VinculoInstitucionalResumo> listarAtivosDaInstituicao(@Param("instituicaoId") UUID instituicaoId);

    @Query("select new br.ivis.identity.dto.VinculoInstitucionalResumo("
            + "v.id, v.contratoId, v.contaAlunoId, p.nome, c.email, v.vinculadoEm) "
            + "from VinculoInstitucional v join Conta c on c.id = v.contaAlunoId "
            + "join PerfilAluno p on p.contaId = c.id "
            + "where v.id = :vinculoId and v.situacao = 'ATIVO' and exists "
            + "(select ct.id from ContratoInstitucional ct where ct.id = v.contratoId "
            + "and ct.contaTitularId = :instituicaoId)")
    Optional<VinculoInstitucionalResumo> buscarResumoAtivoDaInstituicao(
            @Param("vinculoId") UUID vinculoId, @Param("instituicaoId") UUID instituicaoId);

    @Query("select v from VinculoInstitucional v join ContratoInstitucional c on c.id = v.contratoId "
            + "where v.id = :vinculoId and c.contaTitularId = :instituicaoId and v.situacao = 'ATIVO'")
    Optional<VinculoInstitucionalModel> findAtivoByIdAndInstituicao(
            @Param("vinculoId") UUID vinculoId,
            @Param("instituicaoId") UUID instituicaoId);

    @Query("select v from VinculoInstitucional v join ContratoInstitucional c on c.id = v.contratoId "
            + "where v.id = :vinculoId and v.contratoId = :contratoId and v.situacao = 'ATIVO'")
    Optional<VinculoInstitucionalModel> findAtivoByIdAndContrato(
            @Param("vinculoId") UUID vinculoId,
            @Param("contratoId") UUID contratoId);
}
