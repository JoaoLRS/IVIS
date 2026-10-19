package br.ivis.comum.erro;

import java.util.List;

/**
 * Formato uniforme de erro da API: {@code codigo}, {@code mensagem}, {@code campos}
 * e o identificador da requisição (o mesmo do cabeçalho {@code X-Id-Requisicao}).
 */
public record RespostaErro(String codigo, String mensagem, List<ErroCampo> campos, String idRequisicao) {
}
