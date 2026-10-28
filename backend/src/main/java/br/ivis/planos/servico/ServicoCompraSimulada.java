package br.ivis.planos.servico;

import static br.ivis.planos.dto.ContratoCompraSimulada.*;

import br.ivis.comum.erro.ExcecaoNegocio;
import br.ivis.identidade.modelo.Conta;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identidade.repositorio.ContaRepositorio;
import br.ivis.planos.modelo.Plano;

@Service
public class ServicoCompraSimulada {
    private static final String CARTAO_APROVADO = "4242424242424242";
    private static final String CARTAO_RECUSADO = "4000000000000002";

    private final ContaRepositorio contas;
    private final JdbcTemplate banco;

    public ServicoCompraSimulada(ContaRepositorio contas, JdbcTemplate banco) {
        this.contas = contas;
        this.banco = banco;
    }

    @Transactional
    public Resposta contratar(UUID contaId, String chaveIdempotencia, Requisicao requisicao) {
        Conta conta = contas.buscarPorIdParaAtualizacao(contaId)
                .filter(Conta::estaAtiva)
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
        UUID contratoId = banco.queryForObject("""
                insert into contratos (
                    conta_titular_id, plano_id, situacao, quantidade_vagas,
                    preco_centavos_contratado, moeda, iniciado_em, expira_em, encerrado_em
                ) values (?, ?, 'PENDENTE', ?, ?, ?, ?, ?, ?)
                returning id
                """, UUID.class, contaId, oferta.id(), oferta.vagasIncluidas(), oferta.precoCentavos(), oferta.moeda(),
                dataJdbc(agora), dataJdbc(expiraEm), aprovada ? null : dataJdbc(agora));
        UUID pagamentoId = banco.queryForObject("""
                insert into pagamentos (
                    contrato_id, modo, situacao, valor_centavos, moeda, metodo,
                    referencia_provedor, chave_idempotencia, criado_em, atualizado_em, confirmado_em
                ) values (?, 'SIMULADO', ?, ?, ?, 'CARTAO_TESTE', ?, ?, ?, ?, ?)
                returning id
                """, UUID.class, contratoId, aprovada ? "APROVADO" : "RECUSADO",
                oferta.precoCentavos(), oferta.moeda(), "IVIS-DEMO-" + UUID.randomUUID(), chave,
                dataJdbc(agora), dataJdbc(agora), dataJdbc(agora));

        banco.update("""
                update contratos set situacao = ?, atualizado_em = ?, encerrado_em = ?
                where id = ?
                """, aprovada ? "ATIVO" : "ENCERRADO", dataJdbc(agora),
                aprovada ? null : dataJdbc(agora), contratoId);
        banco.update("""
                insert into eventos_auditoria (
                    ator_conta_id, acao, resultado, tipo_recurso, recurso_id, detalhes
                ) values (?, 'CONTRATACAO_PAGAMENTO_SIMULADO', ?, 'CONTRATO', ?, ?::jsonb)
                """, contaId, aprovada ? "SUCESSO" : "FALHA", contratoId,
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
            Conta conta, OfertaCompra oferta, UUID contratoId, UUID pagamentoId, Instant expiraEm) {
        UUID emailId = UUID.randomUUID();
        Instant agora = Instant.now();
        String assunto = "Confirmação de contratação simulada — IVIS";
        String corpo = "Olá!\n\n"
                + "A contratação do " + oferta.nome() + " foi aprovada pelo simulador acadêmico IVIS.\n"
                + "Valor ilustrativo: " + formatarValor(oferta.precoCentavos(), oferta.moeda()) + "\n"
                + "Acesso válido até: " + expiraEm + "\n"
                + "Contrato: " + contratoId + "\n"
                + "Referência de pagamento simulado: " + pagamentoId + "\n\n"
                + "Nenhum valor foi cobrado. Este comprovante é uma mensagem fictícia armazenada no IVIS.";
        banco.update("""
                insert into emails_simulados (
                    id, conta_id, contrato_id, pagamento_id, destinatario, assunto, corpo, criado_em
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """, emailId, conta.getId(), contratoId, pagamentoId,
                conta.getEmail(), assunto, corpo, dataJdbc(agora));
        return new EmailSimulado(conta.getEmail(), assunto, corpo, agora);
    }

    private Resposta buscarCompraAnterior(String chave) {
        List<Resposta> resultados = banco.query("""
                select c.id as contrato_id, pg.id as pagamento_id, p.id as plano_id,
                       p.nome as nome_plano, c.situacao as situacao_contrato,
                       pg.situacao as situacao_pagamento, pg.modo as modo_pagamento,
                       pg.valor_centavos, pg.moeda, c.expira_em,
                       e.destinatario, e.assunto, e.corpo, e.criado_em as email_criado_em
                from pagamentos pg
                join contratos c on c.id = pg.contrato_id
                join planos p on p.id = c.plano_id
                left join emails_simulados e on e.pagamento_id = pg.id
                where pg.chave_idempotencia = ?
                """, (rs, linha) -> new Resposta(
                rs.getObject("contrato_id", UUID.class), rs.getObject("pagamento_id", UUID.class),
                rs.getObject("plano_id", UUID.class), rs.getString("nome_plano"),
                rs.getString("situacao_contrato"), rs.getString("situacao_pagamento"),
                rs.getString("modo_pagamento"), rs.getLong("valor_centavos"), rs.getString("moeda"),
                rs.getTimestamp("expira_em") == null ? null : rs.getTimestamp("expira_em").toInstant(),
                false,
                rs.getString("destinatario") == null ? null : new EmailSimulado(
                        rs.getString("destinatario"), rs.getString("assunto"), rs.getString("corpo"),
                        rs.getTimestamp("email_criado_em").toInstant())), chave);
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    private OfertaCompra buscarOferta(UUID planoId) {
        try {
            return banco.queryForObject("""
                    select id, codigo, nome, modalidade, preco_centavos, moeda, duracao_dias, vagas_incluidas
                    from planos where id = ? and modalidade in ('INDIVIDUAL', 'INSTITUCIONAL') and ativo = true
                    """, (rs, linha) -> new OfertaCompra(
                    rs.getObject("id", UUID.class), rs.getString("codigo"), rs.getString("nome"),
                    rs.getString("modalidade"), rs.getLong("preco_centavos"), rs.getString("moeda").trim(),
                    rs.getInt("duracao_dias"), rs.getInt("vagas_incluidas")),
                    planoId);
        } catch (EmptyResultDataAccessException excecao) {
            throw new ExcecaoNegocio(HttpStatus.NOT_FOUND, "plano_nao_encontrado",
                    "Plano disponível não encontrado.");
        }
    }

    private boolean temVinculoInstitucionalAtivo(UUID contaId) {
        Boolean ativo = banco.queryForObject("""
                select exists (
                    select 1 from vinculos_institucionais v
                    join contratos c on c.id = v.contrato_id
                    where v.conta_aluno_id = ? and v.situacao = 'ATIVO'
                      and v.desvinculado_em is null and v.vinculado_em <= CURRENT_TIMESTAMP
                      and c.situacao = 'ATIVO' and c.encerrado_em is null
                      and (c.expira_em is null or c.expira_em > CURRENT_TIMESTAMP)
                )
                """, Boolean.class, contaId);
        return Boolean.TRUE.equals(ativo);
    }

    private boolean temContratoAtivo(UUID contaId, String modalidade) {
        Boolean ativo = banco.queryForObject("""
                select exists (
                    select 1 from contratos c join planos p on p.id = c.plano_id
                    where c.conta_titular_id = ? and p.modalidade = ?
                      and c.situacao = 'ATIVO' and c.encerrado_em is null
                      and (c.expira_em is null or c.expira_em > CURRENT_TIMESTAMP)
                )
                """, Boolean.class, contaId, modalidade);
        return Boolean.TRUE.equals(ativo);
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
        return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"))
                .format(centavos / 100.0) + " " + moeda;
    }

    private OffsetDateTime dataJdbc(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    private ExcecaoNegocio cartaoInvalido() {
        return new ExcecaoNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "cartao_teste_invalido",
                "Use um dos cartões fictícios aceitos pelo simulador e uma validade futura.");
    }

    private record OfertaCompra(UUID id, String codigo, String nome, String modalidade, long precoCentavos,
                                String moeda, int duracaoDias, int vagasIncluidas) { }
}
