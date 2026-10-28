package br.ivis.identidade.controlador;

import static br.ivis.identidade.dto.ContratoPerfilAluno.VagaRequisicao;
import static br.ivis.identidade.dto.ContratoPerfilAluno.VagaResposta;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.identidade.servico.ServicoPerfilAluno;

@RestController
@RequestMapping("/v1/vagas")
public class VagaControlador {

    private final ServicoPerfilAluno servico;

    public VagaControlador(ServicoPerfilAluno servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<VagaResposta> listar(@AuthenticationPrincipal Jwt jwt) {
        return servico.listarVagas(contaId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VagaResposta criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VagaRequisicao requisicao) {
        return servico.criarVaga(contaId(jwt), requisicao);
    }

    @PutMapping("/{id}")
    public VagaResposta atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody VagaRequisicao requisicao) {
        return servico.atualizarVaga(contaId(jwt), id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        servico.excluirVaga(contaId(jwt), id);
    }

    private static UUID contaId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
