package br.ivis.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identity.model.HabilidadePerfilModel;

public interface HabilidadePerfilRepository extends JpaRepository<HabilidadePerfilModel, UUID> {

    List<HabilidadePerfilModel> findAllByContaAlunoIdOrderByNomeNormalizado(UUID contaAlunoId);

    Optional<HabilidadePerfilModel> findByIdAndContaAlunoId(UUID id, UUID contaAlunoId);

    boolean existsByContaAlunoIdAndNomeNormalizadoAndNivel(UUID contaAlunoId, String nomeNormalizado, short nivel);

    boolean existsByContaAlunoIdAndNomeNormalizadoAndNivelAndIdNot(
            UUID contaAlunoId, String nomeNormalizado, short nivel, UUID id);
}
