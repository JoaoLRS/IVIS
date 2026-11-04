package br.ivis.identity.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identity.model.ContaModel;

public interface ContaRepository extends JpaRepository<ContaModel, UUID> {

    Optional<ContaModel> findByEmailNormalizado(String emailNormalizado);

    boolean existsByEmailNormalizado(String emailNormalizado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conta c where c.id = :id")
    Optional<ContaModel> buscarPorIdParaAtualizacao(@Param("id") UUID id);
}
