package br.ivis.identidade.repositorio;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identidade.modelo.PerfilInstituicao;

public interface PerfilInstituicaoRepositorio extends JpaRepository<PerfilInstituicao, UUID> {

    Optional<PerfilInstituicao> findByContaId(UUID contaId);
}
