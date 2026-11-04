package br.ivis.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identity.model.PerfilInstituicaoModel;

public interface PerfilInstituicaoRepository extends JpaRepository<PerfilInstituicaoModel, UUID> {

    Optional<PerfilInstituicaoModel> findByContaId(UUID contaId);

    boolean existsByCnpj(String cnpj);
}
