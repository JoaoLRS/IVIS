package br.ivis.plans.service;

import static br.ivis.plans.dto.ContratoConsultaPlanos.*;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.identity.model.ContaModel;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identity.repository.ContaRepository;
import br.ivis.plans.model.ContratoPlanoConsultaModel;
import br.ivis.plans.model.PlanoModel;
import br.ivis.plans.repository.ContratoPlanoConsultaRepository;
import br.ivis.plans.repository.PlanoRepository;

@Service
@Transactional(readOnly = true)
public class ConsultaPlanosService {
    private final PlanoRepository planos;
    private final ContratoPlanoConsultaRepository contratos;
    private final ContaRepository contas;

    public ConsultaPlanosService(PlanoRepository planos, ContratoPlanoConsultaRepository contratos,
                                ContaRepository contas) {
        this.planos = planos;
        this.contratos = contratos;
        this.contas = contas;
    }

    public List<Oferta> listarOfertas() {
        return planos.listarOfertasAtivas().stream().map(this::oferta).toList();
    }

    public Oferta consultarOferta(UUID id) {
        return oferta(planos.buscarOfertaAtiva(id).orElseThrow(() -> new ExcecaoNegocio(
                HttpStatus.NOT_FOUND, "plano_nao_encontrado", "Plano disponível não encontrado.")));
    }

    public PlanoDaConta consultarPlanoDaConta(UUID contaId) {
        ContaModel conta = contas.findById(contaId).filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(() -> new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado",
                        "A conta não está habilitada para esta operação."));
        String modalidade = switch (conta.getTipoConta()) {
            case "ALUNO" -> "INDIVIDUAL";
            case "INSTITUICAO" -> "INSTITUCIONAL";
            default -> throw new ExcecaoNegocio(HttpStatus.FORBIDDEN, "acesso_negado", "Acesso negado.");
        };
        List<PlanoVigente> vigentes = contratos.listarVigentesDaConta(contaId, modalidade).stream()
                .map(contrato -> resumo(contrato, contaId)).toList();
        return new PlanoDaConta(vigentes.isEmpty() ? "SEM_PLANO_VIGENTE" : "COM_PLANO_VIGENTE", vigentes);
    }

    private Oferta oferta(PlanoModel plano) {
        return new Oferta(plano.getId(), plano.getCodigo(), plano.getModalidade(), plano.getNome(),
                plano.getDescricao(), plano.getPrecoCentavos(), plano.getMoeda(), plano.getDuracaoDias(),
                plano.getVagasIncluidas(),
                planos.listarBeneficios(plano.getId()));
    }

    private PlanoVigente resumo(ContratoPlanoConsultaModel contrato, UUID contaId) {
        boolean titular = contaId.equals(contrato.getContaTitularId());
        return new PlanoVigente(contrato.getId(), contrato.getPlanoId(),
                contratos.buscarNomePlano(contrato.getPlanoId()), titular ? "PROPRIO" : "VINCULO_INSTITUCIONAL",
                contrato.getSituacao(), contrato.getIniciadoEm(), contrato.getExpiraEm(), contrato.getEncerradoEm(),
                titular ? new CondicoesTitular(contrato.getQuantidadeVagas(),
                        contrato.getPrecoCentavosContratado(), contrato.getMoeda()) : null,
                planos.listarBeneficios(contrato.getPlanoId()));
    }
}
