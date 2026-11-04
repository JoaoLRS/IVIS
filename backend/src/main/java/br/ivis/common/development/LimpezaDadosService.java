package br.ivis.common.development;

import br.ivis.common.error.ExcecaoNegocio;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"dev & !prod", "homolog & !prod", "homologacao & !prod"})
public class LimpezaDadosService {
    private final JdbcTemplate banco;

    public LimpezaDadosService(JdbcTemplate banco) {
        this.banco = banco;
    }

    @Transactional
    public void limpar(String documentoInformado) {
        if (!documentoInformado.matches("[0-9.\\/\\-\\s]+")) throw documentoInvalido();
        String documento = documentoInformado.replaceAll("[^0-9]", "");
        String tabela = switch (documento.length()) {
            case 11 -> "perfis_aluno";
            case 14 -> "perfis_instituicao";
            default -> throw documentoInvalido();
        };
        String coluna = documento.length() == 11 ? "cpf" : "cnpj";
        List<UUID> contas = banco.query("""
                select c.objeto from contas c join %s p on p.objeto=c.objeto
                where p.%s=? for update of c
                """.formatted(tabela, coluna), (rs, linha) -> rs.getObject(1, UUID.class), documento);
        if (contas.isEmpty()) throw new ExcecaoNegocio(HttpStatus.NOT_FOUND,
                "conta_nao_encontrada", "Nenhuma conta foi encontrada para o documento informado.");
        UUID contaId = contas.getFirst();
        String id = contaId.toString();

        banco.execute("set local ivis.limpeza_dados = 'autorizada'");
        banco.update("""
                delete from eventos_auditoria where codigo_objeto_contas=?
                   or codigo_objeto_contratos in
                     (select objeto from contratos where codigo_objeto_contas=?)
                   or detalhes ->> 'alunoId'=?
                """, contaId, contaId, id);
        banco.update("""
                delete from emails_simulados where codigo_objeto_contas=?
                   or codigo_objeto_contratos in
                     (select objeto from contratos where codigo_objeto_contas=?)
                """, contaId, contaId);
        banco.update("""
                delete from pagamentos where codigo_objeto_contratos in
                  (select objeto from contratos where codigo_objeto_contas=?)
                """, contaId);
        banco.update("""
                delete from vinculos_institucionais where codigo_objeto_perfis_aluno=?
                   or codigo_objeto_contas=?
                   or codigo_objeto_contratos in
                     (select objeto from contratos where codigo_objeto_contas=?)
                """, contaId, contaId, contaId);
        banco.update("""
                update perfis_aluno set codigo_objeto_perfis_instituicao=null
                where codigo_objeto_perfis_instituicao=?
                """, contaId);

        banco.update("""
                delete from itens_feedback_treinamento where codigo_objeto_avaliacoes_treinamento in
                  (select a.objeto from avaliacoes_treinamento a join treinamentos t
                   on t.objeto=a.codigo_objeto_treinamentos where t.codigo_objeto_perfis_aluno=?)
                """, contaId);
        banco.update("""
                delete from avaliacoes_treinamento where codigo_objeto_treinamentos in
                  (select objeto from treinamentos where codigo_objeto_perfis_aluno=?)
                """, contaId);
        banco.update("""
                delete from respostas_entrevista where codigo_objeto_perguntas_entrevista in
                  (select p.objeto from perguntas_entrevista p join treinamentos t
                   on t.objeto=p.codigo_objeto_treinamentos where t.codigo_objeto_perfis_aluno=?)
                """, contaId);
        banco.update("""
                delete from perguntas_entrevista where codigo_objeto_treinamentos in
                  (select objeto from treinamentos where codigo_objeto_perfis_aluno=?)
                """, contaId);
        banco.update("delete from treinamentos where codigo_objeto_perfis_aluno=?", contaId);
        banco.update("""
                delete from requisitos_vaga where codigo_objeto_vagas_interesse in
                  (select objeto from vagas_interesse where codigo_objeto_perfis_aluno=?)
                """, contaId);
        banco.update("delete from vagas_interesse where codigo_objeto_perfis_aluno=?", contaId);
        banco.update("delete from habilidades where codigo_objeto_perfis_aluno=?", contaId);
        banco.update("delete from tokens_recuperacao_senha where codigo_objeto_contas=?", contaId);
        banco.update("""
                update sessoes_autenticacao set codigo_objeto_sessoes_autenticacao=null
                where codigo_objeto_contas=?
                """, contaId);
        banco.update("delete from sessoes_autenticacao where codigo_objeto_contas=?", contaId);
        banco.update("delete from contratos where codigo_objeto_contas=?", contaId);
        banco.update("delete from perfis_aluno where objeto=?", contaId);
        banco.update("delete from perfis_instituicao where objeto=?", contaId);
        banco.update("delete from contas where objeto=?", contaId);
    }

    private static ExcecaoNegocio documentoInvalido() {
        return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "documento_invalido",
                "Informe um CPF com 11 dígitos ou CNPJ com 14 dígitos.");
    }
}
