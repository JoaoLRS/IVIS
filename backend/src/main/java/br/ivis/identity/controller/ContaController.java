package br.ivis.identity.controller;

import static br.ivis.identity.dto.ContratoIdentidade.CadastroAlunoRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.CadastroInstituicaoRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.ContaResposta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.identity.service.IdentidadeService;
import br.ivis.identity.service.BoasVindasEmailService;

@RestController
@RequestMapping("/v1/contas")
public class ContaController {

    private final IdentidadeService servico;
    private final BoasVindasEmailService emailBoasVindas;

    public ContaController(IdentidadeService servico, BoasVindasEmailService emailBoasVindas) {
        this.servico = servico;
        this.emailBoasVindas = emailBoasVindas;
    }

    @PostMapping("/alunos")
    public ResponseEntity<ContaResposta> cadastrarAluno(@Valid @RequestBody CadastroAlunoRequisicao requisicao) {
        ContaResposta conta = servico.cadastrarAluno(requisicao);
        emailBoasVindas.enviar(conta);
        return ResponseEntity.status(HttpStatus.CREATED).body(conta);
    }

    @PostMapping("/instituicoes")
    public ResponseEntity<ContaResposta> cadastrarInstituicao(
            @Valid @RequestBody CadastroInstituicaoRequisicao requisicao) {
        ContaResposta conta = servico.cadastrarInstituicao(requisicao);
        emailBoasVindas.enviar(conta);
        return ResponseEntity.status(HttpStatus.CREATED).body(conta);
    }

}
