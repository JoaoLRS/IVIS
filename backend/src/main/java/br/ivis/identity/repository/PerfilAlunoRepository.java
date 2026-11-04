package br.ivis.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identity.model.PerfilAlunoModel;

public interface PerfilAlunoRepository extends JpaRepository<PerfilAlunoModel, UUID> {

    Optional<PerfilAlunoModel> findByContaId(UUID contaId);

    boolean existsByCpf(String cpf);
}
