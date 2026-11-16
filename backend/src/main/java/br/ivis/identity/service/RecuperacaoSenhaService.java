package br.ivis.identity.service;

import br.ivis.common.error.ExcecaoNegocio;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.TokenRecuperacaoSenhaModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.SessaoRepository;
import br.ivis.identity.repository.TokenRecuperacaoSenhaRepository;

@Service
public class RecuperacaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    private static final Duration DURACAO_TOKEN_MINIMA = Duration.ofMinutes(5);
    private static final Duration DURACAO_TOKEN_MAXIMA = Duration.ofHours(2);
    private static final Duration DURACAO_AUTORIZACAO = Duration.ofMinutes(10);
    private static final String MENSAGEM_NEUTRA =
            "Se existir uma conta para este e-mail, enviaremos um código de recuperação.";

    private final ContaRepository contas;
    private final TokenRecuperacaoSenhaRepository tokens;
    private final SessaoRepository sessoes;
    private final PasswordEncoder codificadorSenha;
    private final LimiteRecuperacaoSenha limite;
    private final JavaMailSender remetente;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Duration validadeToken;
    private final String emailRemetente;
    private final String urlRedefinicao;

    public RecuperacaoSenhaService(
            ContaRepository contas,
            TokenRecuperacaoSenhaRepository tokens,
            SessaoRepository sessoes,
            PasswordEncoder codificadorSenha,
            LimiteRecuperacaoSenha limite,
            JavaMailSender remetente,
            @Value("${ivis.recuperacao.validade-token:30m}") Duration validadeToken,
            @Value("${ivis.email.remetente:no-reply@ivis.local}") String emailRemetente,
            @Value("${ivis.recuperacao.url-redefinicao:http://localhost:8080/redefinir-senha}") String urlRedefinicao) {
        if (validadeToken.compareTo(DURACAO_TOKEN_MINIMA) < 0
                || validadeToken.compareTo(DURACAO_TOKEN_MAXIMA) > 0) {
            throw new IllegalArgumentException("A validade do token de recuperação deve estar entre 5m e 2h.");
        }
        validarEndereco(emailRemetente);
        this.contas = contas;
        this.tokens = tokens;
        this.sessoes = sessoes;
        this.codificadorSenha = codificadorSenha;
        this.limite = limite;
        this.remetente = remetente;
        this.validadeToken = validadeToken;
        this.emailRemetente = emailRemetente;
        this.urlRedefinicao = urlRedefinicao;
    }

    @Transactional
    public String solicitar(String email, String enderecoOrigem) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        limite.registrarSolicitacao(normalizado, enderecoOrigem);
        ContaModel conta = contas.findByEmailNormalizado(normalizado).filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElse(null);
        if (conta != null) {
            Instant agora = Instant.now();
            tokens.consumirTodosAtivosDaConta(conta.getId());
            String codigo = gerarCodigo();
            TokenRecuperacaoSenhaModel registro = new TokenRecuperacaoSenhaModel(
                    UUID.randomUUID(), conta.getId(), codificadorSenha.encode(codigo), agora.plus(validadeToken), agora);
            registro = tokens.save(registro);
            try {
                enviar(conta.getEmail(), codigo);
            } catch (MailException falha) {
                registro.consumir(Instant.now());
                log.error("Falha ao enviar mensagem de recuperação pelo SMTP. tipo={}",
                        falha.getClass().getSimpleName());
            }
        }
        return MENSAGEM_NEUTRA;
    }

    @Transactional
    public String validarCodigo(String email, String codigo, String enderecoOrigem) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        limite.verificarRedefinicao(normalizado, enderecoOrigem);
        if (!codigoFormatoValido(codigo)) {
            throw tokenInvalido();
        }
        Instant agora = Instant.now();
        ContaModel conta = contas.findByEmailNormalizado(normalizado)
                .filter(ContaModel::estaAtiva)
                .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
        TokenRecuperacaoSenhaModel registro = tokens.buscarAtivosDaContaParaAtualizacao(conta.getId()).stream()
                .filter(item -> item.estaValidoEm(agora) && codificadorSenha.matches(codigo, item.getHashToken()))
                .findFirst()
                .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
        registro.consumir(agora);
        String autorizacao = gerarAutorizacao();
        tokens.save(new TokenRecuperacaoSenhaModel(
                UUID.randomUUID(), conta.getId(), hashToken(autorizacao), agora.plus(DURACAO_AUTORIZACAO), agora));
        return autorizacao;
    }

    @Transactional
    public void redefinir(String autorizacao, String novaSenha, String enderecoOrigem) {
        limite.verificarRedefinicao("autorizacao:" + hashToken(autorizacao == null ? "" : autorizacao), enderecoOrigem);
        if (autorizacao == null || !autorizacao.matches("[A-Za-z0-9_-]{43}")) {
            throw tokenInvalido();
        }
        Instant agora = Instant.now();
        TokenRecuperacaoSenhaModel registro = tokens.buscarPorHashParaAtualizacao(hashToken(autorizacao))
                .filter(item -> item.estaValidoEm(agora))
                .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
        UUID contaId = registro.getContaId();
        ContaModel conta = contas.buscarPorIdParaAtualizacao(contaId)
                .filter(ContaModel::estaAtiva)
                .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
        conta.atualizarSenha(codificadorSenha.encode(novaSenha), agora);
        registro.consumir(agora);
        sessoes.revogarTodasDaConta(conta.getId(), agora);
    }

    @Transactional(readOnly = true)
    public void verificarAutorizacao(String autorizacao) {
        if (autorizacao == null || !autorizacao.matches("[A-Za-z0-9_-]{43}")) {
            throw tokenInvalido();
        }
        tokens.findByHashToken(hashToken(autorizacao))
                .filter(item -> item.estaValidoEm(Instant.now()))
                .orElseThrow(RecuperacaoSenhaService::tokenInvalido);
    }

    private void enviar(String destino, String codigo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailRemetente);
        mensagem.setTo(destino);
        mensagem.setSubject("Redefinição de senha — IVIS");
        mensagem.setText("""
                Recebemos uma solicitação para redefinir a senha da sua conta IVIS.

                Use este código de 8 dígitos em até %d minutos:
                %s

                Acesse a página abaixo e informe seu e-mail e o código:
                %s

                Se você não solicitou a redefinição, ignore esta mensagem.""".formatted(
                validadeToken.toMinutes(), codigo, urlRedefinicao).stripTrailing());
        remetente.send(mensagem);
    }

    private String gerarCodigo() {
        return "%08d".formatted(aleatorio.nextInt(100_000_000));
    }

    private String gerarAutorizacao() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não está disponível.", excecao);
        }
    }

    private static boolean codigoFormatoValido(String codigo) {
        return codigo != null && codigo.matches("[0-9]{8}");
    }

    private static void validarEndereco(String endereco) {
        try {
            InternetAddress enderecoValidado = new InternetAddress(endereco, true);
            enderecoValidado.validate();
        } catch (AddressException excecao) {
            throw new IllegalArgumentException("O endereço de e-mail remetente é inválido.", excecao);
        }
    }

    private static ExcecaoNegocio tokenInvalido() {
        return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "codigo_recuperacao_invalido",
                "O código de recuperação é inválido ou expirou.");
    }
}
