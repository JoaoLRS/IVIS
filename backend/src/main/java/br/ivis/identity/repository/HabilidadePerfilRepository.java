package br.ivis.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identity.model.HabilidadePerfilModel;

public interface HabilidadePerfilRepository extends JpaRepository<HabilidadePerfilModel, UUID> {

    List<HabilidadePerfilModel> findAllByContaAlunoIdOrderByNomeNormalizado(UUID contaAlunoId);

    Optional<HabilidadePerfilModel> findByIdAndContaAlunoId(UUID id, UUID contaAlunoId);

    boolean existsByContaAlunoIdAndNomeNormalizado(UUID contaAlunoId, String nomeNormalizado);

    boolean existsByContaAlunoIdAndNomeNormalizadoAndIdNot(UUID contaAlunoId, String nomeNormalizado, UUID id);
}
