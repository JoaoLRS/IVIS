package br.ivis.identidade.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identidade.modelo.HabilidadePerfil;

public interface HabilidadePerfilRepositorio extends JpaRepository<HabilidadePerfil, UUID> {

    List<HabilidadePerfil> findAllByContaAlunoIdOrderByNomeNormalizado(UUID contaAlunoId);

    Optional<HabilidadePerfil> findByIdAndContaAlunoId(UUID id, UUID contaAlunoId);

    boolean existsByContaAlunoIdAndNomeNormalizado(UUID contaAlunoId, String nomeNormalizado);

    boolean existsByContaAlunoIdAndNomeNormalizadoAndIdNot(UUID contaAlunoId, String nomeNormalizado, UUID id);
}
