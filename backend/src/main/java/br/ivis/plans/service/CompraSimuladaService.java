package br.ivis.plans.service;

import static br.ivis.plans.dto.ContratoCompraSimulada.*;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.identity.model.ContaModel;
import br.ivis.plans.repository.CompraSimuladaRepository;
import br.ivis.plans.repository.PlanoRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identity.repository.ContaRepository;

@Service
public class CompraSimuladaService {
    private static final String CARTAO_APROVADO = "4242424242424242";
    private static final String CARTAO_RECUSADO = "4000000000000002";

    private final ContaRepository contas;
    private final CompraSimuladaRepository compras;
    private final PlanoRepository planos;

    public CompraSimuladaService(ContaRepository contas, CompraSimuladaRepository compras,
            PlanoRepository planos) {
        this.contas = contas;
        this.compras = compras;
        this.planos = planos;
    }

    @Transactional
    public Resposta contratar(UUID contaId, String chaveIdempotencia, Requisicao requisicao) {
        ContaModel conta = contas.buscarPorIdParaAtualizacao(contaId)
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(() -> new ExcecaoNegocio(HttpStatus.FORBIDDEN,
                        "acesso_negado", "A conta não está habilitada para esta operação."));
        OfertaCompra oferta = buscarOferta(requisicao.planoId());
        String tipoContaEsperado = "INDIVIDUAL".equals(oferta.modalidade()) ? "ALUNO" : "INSTITUICAO";
        if (!tipoContaEsperado.equals(conta.getTipoConta())) {
            throw new ExcecaoNegocio(HttpStatus.FORBIDDEN, "modalidade_invalida",
                    "O tipo de conta não pode contratar esta modalidade de plano.");
        }

        String chave = validarChave(contaId, chaveIdempotencia);
        Resposta anterior = buscarCompraAnterior(chave);
        if (anterior != null) return comReprocessada(anterior);

        if ("INDIVIDUAL".equals(oferta.modalidade()) && temVinculoInstitucionalAtivo(contaId)) {
            throw new ExcecaoNegocio(HttpStatus.CONFLICT, "aluno_vinculado_a_instituicao",
                    "Alunos com vínculo institucional ativo não podem contratar um plano individual.");
        }
        if (temContratoAtivo(contaId, oferta.modalidade())) {
            throw new ExcecaoNegocio(HttpStatus.CONFLICT, "plano_ativo",
                    "Sua conta já possui um plano ativo nesta modalidade.");
        }

        boolean aprovada = validarCartao(requisicao);
        Instant agora = Instant.now();
        Instant expiraEm = aprovada ? agora.plusSeconds(oferta.duracaoDias() * 86_400L) : agora;
        UUID contratoId = UUID.randomUUID();
        compras.inserirContrato(contratoId, contaId, oferta.id(), oferta.vagasIncluidas(),
                oferta.precoCentavos(), oferta.moeda(), dataJdbc(agora), dataJdbc(expiraEm),
                aprovada ? null : dataJdbc(agora));
        UUID pagamentoId = UUID.randomUUID();
        compras.inserirPagamento(pagamentoId, contratoId, aprovada ? "APROVADO" : "RECUSADO",
                oferta.precoCentavos(), oferta.moeda(), "IVIS-DEMO-" + UUID.randomUUID(), chave, dataJdbc(agora));
        compras.atualizarContrato(contratoId, aprovada ? "ATIVO" : "ENCERRADO", dataJdbc(agora),
                aprovada ? null : dataJdbc(agora));
        compras.registrarAuditoria(contaId, contratoId, aprovada ? "SUCESSO" : "FALHA",
                "{\"modo\":\"SIMULADO\",\"situacaoPagamento\":\""
                        + (aprovada ? "APROVADO" : "RECUSADO")
                        + "\",\"valorCentavos\":" + oferta.precoCentavos() + "}");

        EmailSimulado email = aprovada
                ? criarEmailSimulado(conta, oferta, contratoId, pagamentoId, expiraEm)
                : null;
        return new Resposta(contratoId, pagamentoId, oferta.id(), oferta.nome(),
                aprovada ? "ATIVO" : "ENCERRADO", aprovada ? "APROVADO" : "RECUSADO",
                "SIMULADO", oferta.precoCentavos(), oferta.moeda(),
                aprovada ? expiraEm : null, false, email);
    }

    private EmailSimulado criarEmailSimulado(
            ContaModel conta, OfertaCompra oferta, UUID contratoId, UUID pagamentoId, Instant expiraEm) {
        UUID emailId = UUID.randomUUID();
        Instant agora = Instant.now();
        String assunto = "Confirmação de contratação simulada — IVIS";
        String corpo = "Olá!\n\n"
                + "A contratação do " + oferta.nome() + " foi aprovada pelo simulador acadêmico IVIS.\n"
                + "Valor ilustrativo: " + formatarValor(oferta.precoCentavos(), oferta.moeda()) + "\n"
                + "Acesso válido até: " + expiraEm + "\n"
                + "Contrato: " + contratoId + "\n"
                + "Referência de pagamento simulado: " + pagamentoId + "\n\n"
                + "Nenhum valor foi cobrado. Esta confirmação foi enviada ao serviço de e-mail de demonstração.";
        compras.inserirEmail(emailId, conta.getId(), contratoId, pagamentoId,
                conta.getEmail(), assunto, corpo, dataJdbc(agora));
        return new EmailSimulado(conta.getEmail(), assunto, corpo, agora);
    }

    private Resposta buscarCompraAnterior(String chave) {
        var anterior = compras.buscarCompraAnterior(chave);
        if (anterior == null) return null;
        return new Resposta(anterior.getContratoId(), anterior.getPagamentoId(), anterior.getPlanoId(),
                anterior.getNomePlano(), anterior.getSituacaoContrato(), anterior.getSituacaoPagamento(),
                anterior.getModoPagamento(), anterior.getValorCentavos(), anterior.getMoeda().trim(),
                anterior.getExpiraEmMillis() == null ? null : Instant.ofEpochMilli(anterior.getExpiraEmMillis()), false,
                anterior.getDestinatario() == null ? null : new EmailSimulado(
                        anterior.getDestinatario(), anterior.getAssunto(), anterior.getCorpo(),
                        Instant.ofEpochMilli(anterior.getEmailCriadoEmMillis())));
    }

    private OfertaCompra buscarOferta(UUID planoId) {
        var plano = planos.buscarOfertaAtiva(planoId)
                .filter(item -> "INDIVIDUAL".equals(item.getModalidade())
                        || "INSTITUCIONAL".equals(item.getModalidade()))
                .orElseThrow(() -> new ExcecaoNegocio(HttpStatus.NOT_FOUND, "plano_nao_encontrado",
                        "Plano disponível não encontrado."));
        return new OfertaCompra(plano.getId(), plano.getCodigo(), plano.getNome(),
                plano.getModalidade(), plano.getPrecoCentavos(), plano.getMoeda().trim(),
                plano.getDuracaoDias(), plano.getVagasIncluidas());
    }

    private boolean temVinculoInstitucionalAtivo(UUID contaId) {
        return compras.temVinculoInstitucionalAtivo(contaId);
    }

    private boolean temContratoAtivo(UUID contaId, String modalidade) {
        return compras.temContratoAtivo(contaId, modalidade);
    }

    private boolean validarCartao(Requisicao requisicao) {
        String numero = requisicao.numeroCartao().replaceAll("\\s", "");
        if (!numero.matches("^\\d{16}$") || !validadeFutura(requisicao.validade())) {
            throw cartaoInvalido();
        }
        if (!"123".equals(requisicao.cvv())) throw cartaoInvalido();
        if (CARTAO_APROVADO.equals(numero)) return true;
        if (CARTAO_RECUSADO.equals(numero)) return false;
        throw cartaoInvalido();
    }

    private boolean validadeFutura(String validade) {
        try {
            String[] partes = validade.split("/");
            int mes = Integer.parseInt(partes[0]);
            int ano = Integer.parseInt(partes[1]);
            if (partes[1].length() == 2) ano += 2000;
            return !YearMonth.of(ano, mes).isBefore(YearMonth.now(ZoneOffset.UTC));
        } catch (RuntimeException excecao) {
            return false;
        }
    }

    private String validarChave(UUID contaId, String chave) {
        if (chave == null || !chave.matches("^[A-Za-z0-9_-]{16,80}$")) {
            throw new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "chave_idempotencia_invalida",
                    "A chave de idempotência está ausente ou inválida.");
        }
        return contaId + ":" + chave;
    }

    private Resposta comReprocessada(Resposta resposta) {
        return new Resposta(resposta.contratoId(), resposta.pagamentoId(), resposta.planoId(),
                resposta.nomePlano(), resposta.situacaoContrato(), resposta.situacaoPagamento(),
                resposta.modoPagamento(), resposta.valorCentavos(), resposta.moeda(), resposta.expiraEm(),
                true, resposta.emailConfirmacao());
    }

    private String formatarValor(long centavos, String moeda) {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.of("pt", "BR"))
                .format(centavos / 100.0) + " " + moeda;
    }

    private OffsetDateTime dataJdbc(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    private ExcecaoNegocio cartaoInvalido() {
        return new ExcecaoNegocio(HttpStatus.UNPROCESSABLE_CONTENT, "cartao_teste_invalido",
                "Use um dos cartões fictícios aceitos pelo simulador e uma validade futura.");
    }

    private record OfertaCompra(UUID id, String codigo, String nome, String modalidade, long precoCentavos,
                                String moeda, int duracaoDias, int vagasIncluidas) { }
}
