package br.ivis.identidade;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilInstituicaoRepositorio extends JpaRepository<PerfilInstituicao, UUID> {

    Optional<PerfilInstituicao> findByContaId(UUID contaId);
}
