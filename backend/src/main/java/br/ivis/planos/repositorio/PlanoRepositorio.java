package br.ivis.planos.repositorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.ivis.planos.modelo.Plano;

public interface PlanoRepositorio extends Repository<Plano, UUID> {
    @Query("select p from Plano p where p.ativo = true order by p.modalidade, p.precoCentavos, p.codigo")
    List<Plano> listarOfertasAtivas();

    @Query("select p from Plano p where p.id = :id and p.ativo = true")
    Optional<Plano> buscarOfertaAtiva(@Param("id") UUID id);

    @Query(value = "select texto_beneficio from beneficios_plano where plano_id = :id order by ordem", nativeQuery = true)
    List<String> listarBeneficios(@Param("id") UUID id);
}
