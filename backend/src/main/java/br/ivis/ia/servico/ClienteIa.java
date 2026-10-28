package br.ivis.ia.servico;

import static br.ivis.ia.dto.ContratoIa.GerarAvaliacaoEntrada;
import static br.ivis.ia.dto.ContratoIa.GerarAvaliacaoResposta;
import static br.ivis.ia.dto.ContratoIa.GerarAvaliacaoRequisicao;
import static br.ivis.ia.dto.ContratoIa.GerarPerguntaEntrada;
import static br.ivis.ia.dto.ContratoIa.GerarPerguntaResposta;
import static br.ivis.ia.dto.ContratoIa.GerarPerguntaRequisicao;

import br.ivis.comum.erro.ExcecaoNegocio;
import jakarta.validation.Validator;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import br.ivis.ia.configuracao.ConfiguracaoClienteIa;

@Service
@Slf4j
public class ClienteIa {

    private final RestClient clientePerguntas;
    private final RestClient clienteAvaliacoes;
    private final ConfiguracaoClienteIa configuracao;
    private final Validator validador;

    public ClienteIa(
            @Qualifier("clienteIaPerguntas") RestClient clientePerguntas,
            @Qualifier("clienteIaAvaliacoes") RestClient clienteAvaliacoes,
            ConfiguracaoClienteIa configuracao,
            Validator validador) {
        this.clientePerguntas = clientePerguntas;
        this.clienteAvaliacoes = clienteAvaliacoes;
        this.configuracao = configuracao;
        this.validador = validador;
    }

    public GerarPerguntaResposta gerarPergunta(GerarPerguntaEntrada entrada) {
        GerarPerguntaRequisicao requisicao = new GerarPerguntaRequisicao(
                UUID.randomUUID(),
                entrada.tipoContexto(),
                entrada.habilidades(),
                entrada.vaga(),
                entrada.interacoes(),
                entrada.numeroPergunta(),
                entrada.totalPerguntas());
        return executar(
                clientePerguntas, "/v1/perguntas/gerar", requisicao, GerarPerguntaResposta.class);
    }

    public GerarAvaliacaoResposta gerarAvaliacao(GerarAvaliacaoEntrada entrada) {
        GerarAvaliacaoRequisicao requisicao = new GerarAvaliacaoRequisicao(
                UUID.randomUUID(),
                entrada.tipoContexto(),
                entrada.habilidades(),
                entrada.vaga(),
                entrada.interacoes());
        return executar(
                clienteAvaliacoes, "/v1/avaliacoes/gerar", requisicao, GerarAvaliacaoResposta.class);
    }

    private <T> T executar(RestClient cliente, String caminho, Object entrada, Class<T> tipoResposta) {
        T resposta;
        try {
            resposta = cliente.post()
                    .uri(caminho)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Chave-Servico", configuracao.chaveServico())
                    .body(entrada)
                    .retrieve()
                    .body(tipoResposta);
        } catch (RestClientResponseException excecao) {
            throw traduzirRespostaHttp(excecao);
        } catch (ResourceAccessException excecao) {
            if (tempoEsgotado(excecao)) {
                throw new ExcecaoNegocio(
                        HttpStatus.GATEWAY_TIMEOUT, "tempo_esgotado",
                        "A geração de conteúdo demorou mais que o limite permitido.");
            }
            log.warn("API de IA indisponível durante chamada ao endpoint {}", caminho);
            throw new ExcecaoNegocio(
                    HttpStatus.SERVICE_UNAVAILABLE, "provedor_indisponivel",
                    "O serviço de IA está temporariamente indisponível.");
        } catch (RestClientException excecao) {
            log.warn("API de IA devolveu uma resposta que não pôde ser interpretada.");
            throw saidaInvalida();
        }

        if (resposta == null || !validador.validate(resposta).isEmpty()) {
            log.warn("API de IA devolveu uma resposta fora do contrato.");
            throw saidaInvalida();
        }
        return resposta;
    }

    private RuntimeException traduzirRespostaHttp(RestClientResponseException excecao) {
        return switch (excecao.getStatusCode().value()) {
            case 401, 422 -> {
                log.error("A API de IA rejeitou uma chamada de serviço; verifique a configuração.");
                yield new ExcecaoNegocio(
                        HttpStatus.BAD_GATEWAY, "falha_integracao",
                        "Não foi possível concluir a solicitação ao serviço de IA.");
            }
            case 502 -> {
                log.warn("A API de IA informou que a saída do provedor é inválida.");
                yield saidaInvalida();
            }
            case 503 -> new ExcecaoNegocio(
                    HttpStatus.SERVICE_UNAVAILABLE, "provedor_indisponivel",
                    "O serviço de IA está temporariamente indisponível.");
            case 504 -> new ExcecaoNegocio(
                    HttpStatus.GATEWAY_TIMEOUT, "tempo_esgotado",
                    "A geração de conteúdo demorou mais que o limite permitido.");
            default -> {
                log.warn("API de IA respondeu com status HTTP {}.", excecao.getStatusCode().value());
                yield new ExcecaoNegocio(
                        HttpStatus.BAD_GATEWAY, "falha_integracao",
                        "Não foi possível concluir a solicitação ao serviço de IA.");
            }
        };
    }

    private static ExcecaoNegocio saidaInvalida() {
        return new ExcecaoNegocio(
                HttpStatus.BAD_GATEWAY, "saida_invalida",
                "O serviço de IA devolveu uma resposta inválida.");
    }

    private static boolean tempoEsgotado(Throwable excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            if (causa instanceof SocketTimeoutException || causa instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
