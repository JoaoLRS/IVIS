package br.ivis.identity.controller;

import static br.ivis.identity.dto.ContratoRecuperacaoSenha.RedefinicaoRequisicao;
import static br.ivis.identity.dto.ContratoRecuperacaoSenha.SolicitacaoRequisicao;
import static br.ivis.identity.dto.ContratoRecuperacaoSenha.SolicitacaoResposta;
import static br.ivis.identity.dto.ContratoRecuperacaoSenha.ValidacaoCodigoRequisicao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.identity.service.RecuperacaoSenhaService;

@RestController
@RequestMapping("/v1/senhas")
public class RecuperacaoSenhaController {

    private static final String COOKIE_REDEFINICAO = "ivis_redefinicao";
    private static final String CAMINHO_COOKIE_REDEFINICAO = "/api/v1/senhas/redefinicao";
    private final RecuperacaoSenhaService servico;
    private final boolean cookieSeguro;

    public RecuperacaoSenhaController(
            RecuperacaoSenhaService servico,
            @Value("${ivis.sessao.cookie-seguro:true}") boolean cookieSeguro) {
        this.servico = servico;
        this.cookieSeguro = cookieSeguro;
    }

    @PostMapping("/recuperacao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SolicitacaoResposta solicitar(
            @Valid @RequestBody SolicitacaoRequisicao requisicao, HttpServletRequest pedido) {
        return new SolicitacaoResposta(servico.solicitar(requisicao.email(), pedido.getRemoteAddr()));
    }

    @PostMapping("/validacao-codigo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validarCodigo(
            @Valid @RequestBody ValidacaoCodigoRequisicao requisicao,
            HttpServletRequest pedido,
            HttpServletResponse resposta) {
        String autorizacao = servico.validarCodigo(requisicao.email(), requisicao.codigo(), pedido.getRemoteAddr());
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookieRedefinicao(autorizacao, Duration.ofMinutes(10)).toString());
    }

    @PostMapping("/redefinicao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(
            @Valid @RequestBody RedefinicaoRequisicao requisicao,
            @CookieValue(name = COOKIE_REDEFINICAO, required = false) String autorizacao,
            HttpServletRequest pedido,
            HttpServletResponse resposta) {
        servico.redefinir(autorizacao, requisicao.novaSenha(), pedido.getRemoteAddr());
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookieRedefinicao("", Duration.ZERO).toString());
    }

    @GetMapping("/redefinicao/autorizacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarAutorizacao(@CookieValue(name = COOKIE_REDEFINICAO, required = false) String autorizacao) {
        servico.verificarAutorizacao(autorizacao);
    }

    private ResponseCookie cookieRedefinicao(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE_REDEFINICAO, valor)
                .httpOnly(true)
                .secure(cookieSeguro)
                .sameSite("Strict")
                .path(CAMINHO_COOKIE_REDEFINICAO)
                .maxAge(validade)
                .build();
    }
}
