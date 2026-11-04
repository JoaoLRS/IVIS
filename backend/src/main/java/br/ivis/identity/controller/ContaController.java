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

@RestController
@RequestMapping("/v1/contas")
public class ContaController {

    private final IdentidadeService servico;

    public ContaController(IdentidadeService servico) {
        this.servico = servico;
    }

    @PostMapping("/alunos")
    public ResponseEntity<ContaResposta> cadastrarAluno(@Valid @RequestBody CadastroAlunoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.cadastrarAluno(requisicao));
    }

    @PostMapping("/instituicoes")
    public ResponseEntity<ContaResposta> cadastrarInstituicao(
            @Valid @RequestBody CadastroInstituicaoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.cadastrarInstituicao(requisicao));
    }

}
