package br.ivis.identidade.repositorio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ivis.identidade.modelo.RequisitoVaga;

public interface RequisitoVagaRepositorio extends JpaRepository<RequisitoVaga, UUID> {

    List<RequisitoVaga> findAllByVagaInteresseIdOrderByOrdem(UUID vagaInteresseId);

    @Modifying
    @Query("delete from RequisitoVaga r where r.vagaInteresseId = :vagaId")
    int excluirTodosDaVaga(@Param("vagaId") UUID vagaId);
}
