package br.ivis.identidade.repositorio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identidade.modelo.PerfilAluno;

public interface PerfilAlunoRepositorio extends JpaRepository<PerfilAluno, UUID> {

    Optional<PerfilAluno> findByContaId(UUID contaId);
}
