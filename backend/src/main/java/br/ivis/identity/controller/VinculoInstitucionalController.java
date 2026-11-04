package br.ivis.identity.controller;

import static br.ivis.identity.dto.ContratoInstitucionalResposta.Capacidade;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoPorEmailRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CriacaoVinculoResposta;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CadastroAlunoRequisicao;
import static br.ivis.identity.dto.ContratoInstitucionalResposta.CadastroAlunoResposta;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.identity.dto.VinculoInstitucionalResumo;
import br.ivis.identity.service.VinculoInstitucionalService;

@RestController
@RequestMapping("/v1/instituicao")
public class VinculoInstitucionalController {

    private final VinculoInstitucionalService servico;

    public VinculoInstitucionalController(VinculoInstitucionalService servico) {
        this.servico = servico;
    }

    @GetMapping("/alunos")
    public List<VinculoInstitucionalResumo> listarAlunos(@AuthenticationPrincipal Jwt jwt) {
        return servico.listarAlunos(contaId(jwt));
    }

    @PostMapping("/vinculos")
    @ResponseStatus(HttpStatus.CREATED)
    public CriacaoVinculoResposta criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriacaoVinculoRequisicao requisicao) {
        return servico.criarVinculo(contaId(jwt), requisicao);
    }

    @PostMapping("/vinculos/por-email")
    @ResponseStatus(HttpStatus.CREATED)
    public CriacaoVinculoResposta criarPorEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriacaoVinculoPorEmailRequisicao requisicao) {
        return servico.criarVinculoPorEmail(contaId(jwt), requisicao);
    }

    @PostMapping("/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroAlunoResposta cadastrarAluno(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CadastroAlunoRequisicao requisicao) {
        return servico.cadastrarEVincularAluno(contaId(jwt), requisicao);
    }

    @DeleteMapping("/vinculos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        servico.removerVinculo(contaId(jwt), id);
    }

    @GetMapping("/resumo-vagas")
    public List<Capacidade> resumirCapacidade(@AuthenticationPrincipal Jwt jwt) {
        return servico.resumirCapacidade(contaId(jwt));
    }

    private static UUID contaId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
