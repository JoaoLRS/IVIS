package br.ivis.identity.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identity.model.RequisitoVagaModel;

public interface RequisitoVagaRepository extends JpaRepository<RequisitoVagaModel, UUID> {

    List<RequisitoVagaModel> findAllByVagaInteresseIdOrderByOrdem(UUID vagaInteresseId);

    @Modifying
    @Query("delete from RequisitoVaga r where r.vagaInteresseId = :vagaId")
    int excluirTodosDaVaga(@Param("vagaId") UUID vagaId);
}
