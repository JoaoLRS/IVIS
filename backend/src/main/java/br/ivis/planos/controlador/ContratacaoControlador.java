package br.ivis.planos.controlador;

import static br.ivis.planos.dto.ContratoCompraSimulada.Requisicao;
import static br.ivis.planos.dto.ContratoCompraSimulada.Resposta;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.planos.servico.ServicoCompraSimulada;

@RestController
@RequestMapping("/v1/contratacoes")
public class ContratacaoControlador {
    private final ServicoCompraSimulada servico;

    public ContratacaoControlador(ServicoCompraSimulada servico) {
        this.servico = servico;
    }

    @PostMapping("/simuladas")
    public Resposta contratar(
            @AuthenticationPrincipal Jwt principal,
            @RequestHeader("Idempotency-Key") String chaveIdempotencia,
            @Valid @RequestBody Requisicao requisicao) {
        UUID contaId = UUID.fromString(principal.getSubject());
        return servico.contratar(contaId, chaveIdempotencia, requisicao);
    }
}
