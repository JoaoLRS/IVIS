package br.ivis.plans.controller;

import static br.ivis.plans.dto.ContratoConsultaPlanos.*;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.plans.service.ConsultaPlanosService;

@RestController
@RequestMapping("/v1")
public class PlanoController {
    private final ConsultaPlanosService servico;

    public PlanoController(ConsultaPlanosService servico) { this.servico = servico; }

    @Operation(summary = "Listar ofertas ativas do catálogo público")
    @GetMapping("/planos")
    public List<Oferta> listar() { return servico.listarOfertas(); }

    @Operation(summary = "Consultar uma oferta ativa e seus benefícios")
    @GetMapping("/planos/{id}")
    public Oferta consultar(@PathVariable UUID id) { return servico.consultarOferta(id); }

    @Operation(summary = "Consultar contratos vigentes próprios ou por vínculo da conta autenticada")
    @GetMapping("/conta/plano")
    public PlanoDaConta consultarDaConta(@AuthenticationPrincipal Jwt principal) {
        return servico.consultarPlanoDaConta(UUID.fromString(principal.getSubject()));
    }
}
