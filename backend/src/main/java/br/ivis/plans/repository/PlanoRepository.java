package br.ivis.plans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.ivis.plans.model.PlanoModel;

public interface PlanoRepository extends Repository<PlanoModel, UUID> {
    @Query("select p from Plano p where p.ativo = true order by p.modalidade, p.precoCentavos, p.codigo")
    List<PlanoModel> listarOfertasAtivas();

    @Query("select p from Plano p where p.id = :id and p.ativo = true")
    Optional<PlanoModel> buscarOfertaAtiva(@Param("id") UUID id);

    @Query(value = "select texto_beneficio from beneficios_plano where codigo_objeto_planos = :id order by ordem", nativeQuery = true)
    List<String> listarBeneficios(@Param("id") UUID id);
}
