package br.ivis.comum.erro;

/** Problema de validação associado a um campo da requisição. */
public record ErroCampo(String campo, String mensagem) {
}
