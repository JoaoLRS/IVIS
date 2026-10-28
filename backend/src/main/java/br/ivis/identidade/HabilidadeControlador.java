package br.ivis.identidade;

import static br.ivis.identidade.ContratoPerfilAluno.HabilidadeRequisicao;
import static br.ivis.identidade.ContratoPerfilAluno.HabilidadeResposta;

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

@RestController
@RequestMapping("/v1/habilidades")
public class HabilidadeControlador {

    private final ServicoPerfilAluno servico;

    public HabilidadeControlador(ServicoPerfilAluno servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<HabilidadeResposta> listar(@AuthenticationPrincipal Jwt jwt) {
        return servico.listarHabilidades(contaId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabilidadeResposta criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HabilidadeRequisicao requisicao) {
        return servico.criarHabilidade(contaId(jwt), requisicao);
    }

    @PutMapping("/{id}")
    public HabilidadeResposta atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody HabilidadeRequisicao requisicao) {
        return servico.atualizarHabilidade(contaId(jwt), id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        servico.excluirHabilidade(contaId(jwt), id);
    }

    private static UUID contaId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
