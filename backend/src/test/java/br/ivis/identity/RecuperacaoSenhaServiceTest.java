package br.ivis.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.TokenRecuperacaoSenhaModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.SessaoRepository;
import br.ivis.identity.repository.TokenRecuperacaoSenhaRepository;
import br.ivis.identity.service.LimiteRecuperacaoSenha;
import br.ivis.identity.service.RecuperacaoSenhaService;

class RecuperacaoSenhaServiceTest {

    private ContaRepository contas;
    private TokenRecuperacaoSenhaRepository tokens;
    private SessaoRepository sessoes;
    private JavaMailSender remetente;
    private PasswordEncoder codificadorSenha;
    private RecuperacaoSenhaService servico;

    @BeforeEach
    void preparar() {
        contas = mock(ContaRepository.class);
        tokens = mock(TokenRecuperacaoSenhaRepository.class);
        sessoes = mock(SessaoRepository.class);
        remetente = mock(JavaMailSender.class);
        codificadorSenha = mock(PasswordEncoder.class);
        servico = new RecuperacaoSenhaService(
                contas,
                tokens,
                sessoes,
                codificadorSenha,
                new LimiteRecuperacaoSenha(5, Duration.ofMinutes(15)),
                remetente,
                Duration.ofMinutes(30),
                "no-reply@ivis.local",
                "http://localhost:8080/redefinir-senha");
    }

    @Test
    void solicitacaoRespondeNeutroSemContaEnaoEnviaEmail() {
        when(contas.findByEmailNormalizado("ausente@example.com")).thenReturn(Optional.empty());

        String resposta = servico.solicitar("Ausente@example.com", "127.0.0.1");

        assertThat(resposta).contains("Se existir uma conta");
        verify(remetente, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void tokenEnviadoEhArmazenadoComoHashEConsumoRedefineSenhaRevogandoSessoes() throws Exception {
        UUID contaId = UUID.randomUUID();
        ContaModel conta = new ContaModel(contaId, "ALUNO", "aluno@example.com",
                "aluno@example.com", "hash-antigo", Instant.now());
        when(contas.findByEmailNormalizado("aluno@example.com")).thenReturn(Optional.of(conta));
        when(tokens.save(any(TokenRecuperacaoSenhaModel.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(codificadorSenha.encode("senha-nova-segura")).thenReturn("hash-novo");

        servico.solicitar("aluno@example.com", "127.0.0.1");

        var mensagemCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remetente).send(mensagemCaptor.capture());
        String corpoMensagem = mensagemCaptor.getValue().getText();
        String token = corpoMensagem.replaceFirst("(?s).*token=([A-Za-z0-9_-]{43}).*", "$1");
        String hashToken = hash(token);
        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(TokenRecuperacaoSenhaModel.class);
        verify(tokens).save(tokenCaptor.capture());
        TokenRecuperacaoSenhaModel registro = tokenCaptor.getValue();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(registro.getHashToken()).isEqualTo(hashToken).isNotEqualTo(token);

        when(tokens.buscarPorHashParaAtualizacao(hashToken)).thenReturn(Optional.of(registro));
        when(contas.buscarPorIdParaAtualizacao(contaId)).thenReturn(Optional.of(conta));
        servico.redefinir(token, "senha-nova-segura", "127.0.0.1");

        assertThat(conta.getHashSenha()).isEqualTo("hash-novo");
        assertThat(registro.estaValidoEm(Instant.now())).isFalse();
        verify(sessoes).revogarTodasDaConta(eq(contaId), any(Instant.class));
        assertThatThrownBy(() -> servico.redefinir(token, "outra-senha-segura", "127.0.0.1"))
                .isInstanceOf(br.ivis.common.error.ExcecaoNegocio.class);
    }

    @Test
    void rejeitaTokenExpirado() {
        TokenRecuperacaoSenhaModel expirado = new TokenRecuperacaoSenhaModel(
                UUID.randomUUID(), UUID.randomUUID(), "hash", Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(60));
        when(tokens.buscarPorHashParaAtualizacao(any())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> servico.redefinir("A".repeat(43), "senha-nova-segura", "127.0.0.1"))
                .isInstanceOfSatisfying(br.ivis.common.error.ExcecaoNegocio.class,
                        excecao -> assertThat(excecao.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(sessoes, never()).revogarTodasDaConta(any(UUID.class), any(Instant.class));
    }

    private static String hash(String valor) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(valor.getBytes(StandardCharsets.UTF_8)));
    }
}
