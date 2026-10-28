package br.ivis.ia.controlador;

import static br.ivis.ia.dto.ContratoIa.GerarAvaliacaoEntrada;
import static br.ivis.ia.dto.ContratoIa.GerarAvaliacaoResposta;
import static br.ivis.ia.dto.ContratoIa.GerarPerguntaEntrada;
import static br.ivis.ia.dto.ContratoIa.GerarPerguntaResposta;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ivis.ia.servico.ClienteIa;

@RestController
@RequestMapping("/v1/ia")
public class IaControlador {

    private final ClienteIa clienteIa;

    public IaControlador(ClienteIa clienteIa) {
        this.clienteIa = clienteIa;
    }

    @PostMapping("/perguntas/gerar")
    public GerarPerguntaResposta gerarPergunta(@Valid @RequestBody GerarPerguntaEntrada entrada) {
        return clienteIa.gerarPergunta(entrada);
    }

    @PostMapping("/avaliacoes/gerar")
    public GerarAvaliacaoResposta gerarAvaliacao(@Valid @RequestBody GerarAvaliacaoEntrada entrada) {
        return clienteIa.gerarAvaliacao(entrada);
    }
}
