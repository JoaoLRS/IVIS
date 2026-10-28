package br.ivis.identidade.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ivis.identidade.modelo.VagaInteresse;

public interface VagaInteresseRepositorio extends JpaRepository<VagaInteresse, UUID> {

    List<VagaInteresse> findAllByContaAlunoIdAndArquivadoEmIsNullOrderByCriadoEmDesc(UUID contaAlunoId);

    Optional<VagaInteresse> findByIdAndContaAlunoIdAndArquivadoEmIsNull(UUID id, UUID contaAlunoId);
}
