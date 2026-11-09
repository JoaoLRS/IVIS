package br.ivis.plans.controller;

import static br.ivis.plans.dto.ContratoCompraSimulada.Requisicao;
import static br.ivis.plans.dto.ContratoCompraSimulada.Resposta;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.plans.service.CompraSimuladaService;
import br.ivis.plans.service.ConfirmacaoContratacaoEmailService;

@RestController
@RequestMapping("/v1/contratacoes")
public class ContratacaoController {
    private final CompraSimuladaService servico;
    private final ConfirmacaoContratacaoEmailService emailConfirmacao;

    public ContratacaoController(CompraSimuladaService servico,
            ConfirmacaoContratacaoEmailService emailConfirmacao) {
        this.servico = servico;
        this.emailConfirmacao = emailConfirmacao;
    }

    @PostMapping("/simuladas")
    public Resposta contratar(
            @AuthenticationPrincipal Jwt principal,
            @RequestHeader("Idempotency-Key") String chaveIdempotencia,
            @Valid @RequestBody Requisicao requisicao) {
        UUID contaId = UUID.fromString(principal.getSubject());
        Resposta resposta = servico.contratar(contaId, chaveIdempotencia, requisicao);
        if (!resposta.reprocessada()) emailConfirmacao.enviar(resposta.emailConfirmacao());
        return resposta;
    }
}
