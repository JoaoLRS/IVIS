package br.ivis.planos.servico;

import static br.ivis.planos.dto.ContratoConsultaPlanos.*;

import br.ivis.comum.erro.ExcecaoNegocio;
import br.ivis.identidade.modelo.Conta;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identidade.repositorio.ContaRepositorio;
import br.ivis.planos.modelo.ContratoPlanoConsulta;
import br.ivis.planos.modelo.Plano;
import br.ivis.planos.repositorio.ContratoPlanoConsultaRepositorio;
import br.ivis.planos.repositorio.PlanoRepositorio;

@Service
@Transactional(readOnly = true)
public class ServicoConsultaPlanos {
    private final PlanoRepositorio planos;
    private final ContratoPlanoConsultaRepositorio contratos;
    private final ContaRepositorio contas;

    public ServicoConsultaPlanos(PlanoRepositorio planos, ContratoPlanoConsultaRepositorio contratos,
                                ContaRepositorio contas) {
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
        Conta conta = contas.findById(contaId).filter(contaEncontrada -> contaEncontrada.estaAtiva())
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

    private Oferta oferta(Plano plano) {
        return new Oferta(plano.getId(), plano.getCodigo(), plano.getModalidade(), plano.getNome(),
                plano.getDescricao(), plano.getPrecoCentavos(), plano.getMoeda(), plano.getDuracaoDias(),
                plano.getVagasIncluidas(),
                planos.listarBeneficios(plano.getId()));
    }

    private PlanoVigente resumo(ContratoPlanoConsulta contrato, UUID contaId) {
        boolean titular = contaId.equals(contrato.getContaTitularId());
        return new PlanoVigente(contrato.getId(), contrato.getPlanoId(),
                contratos.buscarNomePlano(contrato.getPlanoId()), titular ? "PROPRIO" : "VINCULO_INSTITUCIONAL",
                contrato.getSituacao(), contrato.getIniciadoEm(), contrato.getExpiraEm(), contrato.getEncerradoEm(),
                titular ? new CondicoesTitular(contrato.getQuantidadeVagas(),
                        contrato.getPrecoCentavosContratado(), contrato.getMoeda()) : null,
                planos.listarBeneficios(contrato.getPlanoId()));
    }
}
