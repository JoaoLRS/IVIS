package br.ivis.identidade;

import static br.ivis.identidade.ContratoIdentidade.CadastroAlunoRequisicao;
import static br.ivis.identidade.ContratoIdentidade.CadastroInstituicaoRequisicao;
import static br.ivis.identidade.ContratoIdentidade.ContaResposta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/contas")
public class ContaControlador {

    private final ServicoIdentidade servico;

    public ContaControlador(ServicoIdentidade servico) {
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
