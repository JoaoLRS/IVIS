package br.ivis.identity.controller;

import static br.ivis.identity.dto.ContratoRecuperacaoSenha.RedefinicaoRequisicao;
import static br.ivis.identity.dto.ContratoRecuperacaoSenha.SolicitacaoRequisicao;
import static br.ivis.identity.dto.ContratoRecuperacaoSenha.SolicitacaoResposta;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.identity.service.RecuperacaoSenhaService;

@RestController
@RequestMapping("/v1/senhas")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService servico;

    public RecuperacaoSenhaController(RecuperacaoSenhaService servico) {
        this.servico = servico;
    }

    @PostMapping("/recuperacao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SolicitacaoResposta solicitar(
            @Valid @RequestBody SolicitacaoRequisicao requisicao, HttpServletRequest pedido) {
        return new SolicitacaoResposta(servico.solicitar(requisicao.email(), pedido.getRemoteAddr()));
    }

    @PostMapping("/redefinicao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(
            @Valid @RequestBody RedefinicaoRequisicao requisicao, HttpServletRequest pedido) {
        servico.redefinir(requisicao.token(), requisicao.novaSenha(), pedido.getRemoteAddr());
    }
}
