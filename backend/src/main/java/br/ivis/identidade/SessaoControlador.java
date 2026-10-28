package br.ivis.identidade;

import static br.ivis.identidade.ContratoIdentidade.LoginRequisicao;
import static br.ivis.identidade.ContratoIdentidade.SessaoResposta;
import static br.ivis.identidade.ContratoIdentidade.TokenCsrfResposta;

import br.ivis.comum.erro.ExcecaoNegocio;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessoes")
public class SessaoControlador {

    private static final String COOKIE_REFRESH = "ivis_refresh";
    private static final String CAMINHO_COOKIE_REFRESH = "/api/v1/sessoes";

    private final ServicoIdentidade servico;
    private final boolean cookieSeguro;

    public SessaoControlador(
            ServicoIdentidade servico,
            @Value("${ivis.sessao.cookie-seguro:true}") boolean cookieSeguro) {
        this.servico = servico;
        this.cookieSeguro = cookieSeguro;
    }

    @GetMapping("/csrf")
    public TokenCsrfResposta tokenCsrf(CsrfToken token) {
        return new TokenCsrfResposta(token.getToken());
    }

    @PostMapping
    public SessaoResposta entrar(
            @Valid @RequestBody LoginRequisicao requisicao,
            HttpServletRequest pedido,
            HttpServletResponse resposta) {
        ServicoIdentidade.EmissaoSessao emissao = servico.autenticar(requisicao, pedido.getRemoteAddr());
        definirCookieRefresh(resposta, emissao);
        return emissao.resposta();
    }

    @PostMapping("/renovacao")
    public SessaoResposta renovar(
            @CookieValue(name = COOKIE_REFRESH, required = false) String token,
            HttpServletResponse resposta) {
        ServicoIdentidade.EmissaoSessao emissao = servico.renovar(token);
        definirCookieRefresh(resposta, emissao);
        return emissao.resposta();
    }

    @DeleteMapping("/atual")
    public void sair(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = COOKIE_REFRESH, required = false) String token,
            HttpServletResponse resposta) {
        UUID contaId = UUID.fromString(jwt.getSubject());
        servico.encerrar(contaId, token);
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookieRefresh("", Duration.ZERO).toString());
        resposta.setStatus(HttpStatus.NO_CONTENT.value());
    }

    private void definirCookieRefresh(HttpServletResponse resposta, ServicoIdentidade.EmissaoSessao emissao) {
        Duration validade = Duration.between(java.time.Instant.now(), emissao.expiraRefreshEm());
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookieRefresh(emissao.tokenRenovacao(), validade).toString());
    }

    private ResponseCookie cookieRefresh(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)
                .secure(cookieSeguro)
                .sameSite("Strict")
                .path(CAMINHO_COOKIE_REFRESH)
                .maxAge(validade)
                .build();
    }
}
