package br.ivis.planos.repositorio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import br.ivis.planos.modelo.ContratoPlanoConsulta;

public interface ContratoPlanoConsultaRepositorio extends Repository<ContratoPlanoConsulta, UUID> {
    @Query(value = """
            select c.* from contratos c join planos p on p.objeto = c.codigo_objeto_planos
            where c.situacao = 'ATIVO' and c.encerrado_em is null and c.iniciado_em <= CURRENT_TIMESTAMP
              and (c.expira_em is null or c.expira_em > CURRENT_TIMESTAMP)
              and ((c.codigo_objeto_contas = :contaId and p.modalidade = :modalidade)
                or (:modalidade = 'INDIVIDUAL' and p.modalidade = 'INSTITUCIONAL' and exists (
                    select 1 from vinculos_institucionais v
                    where v.codigo_objeto_contratos = c.objeto and v.codigo_objeto_perfis_aluno = :contaId
                      and v.situacao = 'ATIVO' and v.desvinculado_em is null
                      and v.vinculado_em <= CURRENT_TIMESTAMP)))
            order by c.iniciado_em desc, c.objeto
            """, nativeQuery = true)
    List<ContratoPlanoConsulta> listarVigentesDaConta(
            @Param("contaId") UUID contaId, @Param("modalidade") String modalidade);

    @Query(value = "select nome from planos where objeto = :planoId", nativeQuery = true)
    String buscarNomePlano(@Param("planoId") UUID planoId);
}
