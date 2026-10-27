package br.ivis.identidade;

import static br.ivis.identidade.ContratoIdentidade.AtualizacaoContaRequisicao;
import static br.ivis.identidade.ContratoIdentidade.ContaResposta;
import static br.ivis.identidade.ContratoIdentidade.TrocaSenhaRequisicao;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conta")
public class ContaPropriaControlador {

    private final ServicoIdentidade servico;

    public ContaPropriaControlador(ServicoIdentidade servico) {
        this.servico = servico;
    }

    @GetMapping
    public ContaResposta minhaConta(@AuthenticationPrincipal Jwt jwt) {
        return servico.buscarMinhaConta(contaId(jwt));
    }

    @PatchMapping
    public ContaResposta atualizarMinhaConta(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AtualizacaoContaRequisicao requisicao) {
        return servico.atualizarMinhaConta(contaId(jwt), requisicao);
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TrocaSenhaRequisicao requisicao) {
        servico.trocarSenha(contaId(jwt), requisicao);
    }

    private static UUID contaId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
