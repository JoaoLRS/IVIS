package br.ivis.identidade;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilAlunoRepositorio extends JpaRepository<PerfilAluno, UUID> {

    Optional<PerfilAluno> findByContaId(UUID contaId);
}
