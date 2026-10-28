package br.ivis.identidade.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identidade.dto.VinculoInstitucionalResumo;
import br.ivis.identidade.modelo.Conta;
import br.ivis.identidade.modelo.ContratoInstitucional;
import br.ivis.identidade.modelo.PerfilAluno;
import br.ivis.identidade.modelo.VinculoInstitucional;

public interface VinculoInstitucionalRepositorio extends JpaRepository<VinculoInstitucional, UUID> {

    long countByContratoIdAndSituacao(UUID contratoId, String situacao);

    boolean existsByContaAlunoIdAndSituacao(UUID contaAlunoId, String situacao);

    @Query("select new br.ivis.identidade.dto.VinculoInstitucionalResumo("
            + "v.id, v.contratoId, v.contaAlunoId, p.nomeExibicao, c.email, v.vinculadoEm) "
            + "from VinculoInstitucional v join ContratoInstitucional ct on ct.id = v.contratoId "
            + "join Conta c on c.id = v.contaAlunoId join PerfilAluno p on p.contaId = c.id "
            + "where ct.contaTitularId = :instituicaoId and v.situacao = 'ATIVO' "
            + "order by v.vinculadoEm desc")
    List<VinculoInstitucionalResumo> listarAtivosDaInstituicao(@Param("instituicaoId") UUID instituicaoId);

    @Query("select new br.ivis.identidade.dto.VinculoInstitucionalResumo("
            + "v.id, v.contratoId, v.contaAlunoId, p.nomeExibicao, c.email, v.vinculadoEm) "
            + "from VinculoInstitucional v join Conta c on c.id = v.contaAlunoId "
            + "join PerfilAluno p on p.contaId = c.id "
            + "where v.id = :vinculoId and v.situacao = 'ATIVO' and exists "
            + "(select ct.id from ContratoInstitucional ct where ct.id = v.contratoId "
            + "and ct.contaTitularId = :instituicaoId)")
    Optional<VinculoInstitucionalResumo> buscarResumoAtivoDaInstituicao(
            @Param("vinculoId") UUID vinculoId, @Param("instituicaoId") UUID instituicaoId);

    @Query("select v from VinculoInstitucional v join ContratoInstitucional c on c.id = v.contratoId "
            + "where v.id = :vinculoId and c.contaTitularId = :instituicaoId and v.situacao = 'ATIVO'")
    Optional<VinculoInstitucional> findAtivoByIdAndInstituicao(
            @Param("vinculoId") UUID vinculoId,
            @Param("instituicaoId") UUID instituicaoId);

    @Query("select v from VinculoInstitucional v join ContratoInstitucional c on c.id = v.contratoId "
            + "where v.id = :vinculoId and v.contratoId = :contratoId and v.situacao = 'ATIVO'")
    Optional<VinculoInstitucional> findAtivoByIdAndContrato(
            @Param("vinculoId") UUID vinculoId,
            @Param("contratoId") UUID contratoId);
}
