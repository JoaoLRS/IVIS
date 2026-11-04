package br.ivis.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identity.model.TokenRecuperacaoSenhaModel;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenhaModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRecuperacaoSenha t where t.hashToken = :hash")
    Optional<TokenRecuperacaoSenhaModel> buscarPorHashParaAtualizacao(@Param("hash") String hash);

    @Modifying
    @Query("update TokenRecuperacaoSenha t set t.consumidoEm = CURRENT_TIMESTAMP "
            + "where t.contaId = :contaId and t.consumidoEm is null")
    int consumirTodosAtivosDaConta(@Param("contaId") UUID contaId);
}
