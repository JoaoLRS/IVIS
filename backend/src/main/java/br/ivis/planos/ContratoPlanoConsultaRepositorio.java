package br.ivis.planos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ContratoPlanoConsultaRepositorio extends Repository<ContratoPlanoConsulta, UUID> {
    @Query(value = """
            select c.* from contratos c join planos p on p.id = c.plano_id
            where c.situacao = 'ATIVO' and c.encerrado_em is null and c.iniciado_em <= CURRENT_TIMESTAMP
              and ((c.conta_titular_id = :contaId and p.modalidade = :modalidade)
                or (:modalidade = 'INDIVIDUAL' and p.modalidade = 'INSTITUCIONAL' and exists (
                    select 1 from vinculos_institucionais v
                    where v.contrato_id = c.id and v.conta_aluno_id = :contaId
                      and v.situacao = 'ATIVO' and v.desvinculado_em is null
                      and v.vinculado_em <= CURRENT_TIMESTAMP)))
            order by c.iniciado_em desc, c.id
            """, nativeQuery = true)
    List<ContratoPlanoConsulta> listarVigentesDaConta(
            @Param("contaId") UUID contaId, @Param("modalidade") String modalidade);

    @Query(value = "select nome from planos where id = :planoId", nativeQuery = true)
    String buscarNomePlano(@Param("planoId") UUID planoId);
}
