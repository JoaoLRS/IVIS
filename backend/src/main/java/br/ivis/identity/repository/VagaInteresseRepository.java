package br.ivis.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identity.model.VagaInteresseModel;

public interface VagaInteresseRepository extends JpaRepository<VagaInteresseModel, UUID> {

    List<VagaInteresseModel> findAllByContaAlunoIdAndArquivadoEmIsNullOrderByCriadoEmDesc(UUID contaAlunoId);

    Optional<VagaInteresseModel> findByIdAndContaAlunoIdAndArquivadoEmIsNull(UUID id, UUID contaAlunoId);
}
