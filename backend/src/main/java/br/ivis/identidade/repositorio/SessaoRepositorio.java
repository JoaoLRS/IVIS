package br.ivis.identidade.repositorio;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identidade.modelo.SessaoAutenticacao;

public interface SessaoRepositorio extends JpaRepository<SessaoAutenticacao, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoAutenticacao s where s.hashTokenRenovacao = :hash")
    Optional<SessaoAutenticacao> buscarPorHashParaAtualizacao(@Param("hash") String hash);

    @Modifying
    @Transactional
    @Query("update SessaoAutenticacao s set s.revogadaEm = :agora "
            + "where s.contaId = :contaId and s.revogadaEm is null")
    int revogarTodasDaConta(@Param("contaId") UUID contaId, @Param("agora") java.time.Instant agora);
}
