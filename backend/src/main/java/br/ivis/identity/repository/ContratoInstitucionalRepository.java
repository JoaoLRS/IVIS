package br.ivis.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identity.model.ContratoInstitucionalModel;

public interface ContratoInstitucionalRepository extends JpaRepository<ContratoInstitucionalModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContratoInstitucional c join PlanoContrato p on p.id = c.planoId "
            + "where c.id = :contratoId and c.contaTitularId = :instituicaoId "
            + "and c.situacao = 'ATIVO' and c.encerradoEm is null "
            + "and (c.expiraEm is null or c.expiraEm > CURRENT_TIMESTAMP) "
            + "and p.modalidade = 'INSTITUCIONAL'")
    Optional<ContratoInstitucionalModel> buscarAtivoParaVinculacao(
            @Param("contratoId") UUID contratoId, @Param("instituicaoId") UUID instituicaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContratoInstitucional c where c.id = :contratoId "
            + "and c.contaTitularId = :instituicaoId")
    Optional<ContratoInstitucionalModel> buscarDaInstituicaoParaAtualizacao(
            @Param("contratoId") UUID contratoId, @Param("instituicaoId") UUID instituicaoId);

    @Query("select c from ContratoInstitucional c join PlanoContrato p on p.id = c.planoId "
            + "where c.contaTitularId = :instituicaoId and c.situacao = 'ATIVO' "
            + "and c.encerradoEm is null and (c.expiraEm is null or c.expiraEm > CURRENT_TIMESTAMP) "
            + "and p.modalidade = 'INSTITUCIONAL' order by c.iniciadoEm")
    List<ContratoInstitucionalModel> listarAtivosDaInstituicao(@Param("instituicaoId") UUID instituicaoId);
}
