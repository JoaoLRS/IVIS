package br.ivis.common.development;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.common.development.repository.LimpezaDadosRepository;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"dev & !prod", "homologacao & !prod"})
public class LimpezaDadosService {
    private final LimpezaDadosRepository dados;

    public LimpezaDadosService(LimpezaDadosRepository dados) {
        this.dados = dados;
    }

    @Transactional
    public void limpar(String documentoInformado) {
        if (!documentoInformado.matches("[0-9.\\/\\-\\s]+")) throw documentoInvalido();
        String documento = documentoInformado.replaceAll("[^0-9]", "");
        UUID contaId = switch (documento.length()) {
            case 11 -> dados.buscarAluno(documento);
            case 14 -> dados.buscarInstituicao(documento);
            default -> throw documentoInvalido();
        };
        if (contaId == null) throw new ExcecaoNegocio(HttpStatus.NOT_FOUND,
                "conta_nao_encontrada", "Nenhuma conta foi encontrada para o documento informado.");

        dados.autorizarExclusaoAuditoria();
        dados.excluirAuditoria(contaId, contaId.toString());
        dados.excluirEmails(contaId);
        dados.excluirPagamentos(contaId);
        dados.excluirVinculos(contaId);
        dados.desvincularAlunos(contaId);
        dados.excluirItensFeedback(contaId);
        dados.excluirAvaliacoes(contaId);
        dados.excluirRespostas(contaId);
        dados.excluirPerguntas(contaId);
        dados.excluirTreinos(contaId);
        dados.excluirRequisitos(contaId);
        dados.excluirVagas(contaId);
        dados.excluirHabilidades(contaId);
        dados.excluirTokensRecuperacao(contaId);
        dados.desvincularSessoes(contaId);
        dados.excluirSessoes(contaId);
        dados.excluirContratos(contaId);
        dados.excluirPerfilAluno(contaId);
        dados.excluirPerfilInstituicao(contaId);
        dados.excluirConta(contaId);
    }

    private static ExcecaoNegocio documentoInvalido() {
        return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "documento_invalido",
                "Informe um CPF com 11 dígitos ou CNPJ com 14 dígitos.");
    }
}
