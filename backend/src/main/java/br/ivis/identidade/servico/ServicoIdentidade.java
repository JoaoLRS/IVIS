package br.ivis.identidade.servico;

import static br.ivis.identidade.dto.ContratoIdentidade.CadastroAlunoRequisicao;
import static br.ivis.identidade.dto.ContratoIdentidade.CadastroInstituicaoRequisicao;
import static br.ivis.identidade.dto.ContratoIdentidade.AtualizacaoContaRequisicao;
import static br.ivis.identidade.dto.ContratoIdentidade.ContaResposta;
import static br.ivis.identidade.dto.ContratoIdentidade.LoginRequisicao;
import static br.ivis.identidade.dto.ContratoIdentidade.SessaoResposta;
import static br.ivis.identidade.dto.ContratoIdentidade.TrocaSenhaRequisicao;

import br.ivis.comum.erro.ExcecaoNegocio;
import br.ivis.comum.erro.ErroCampo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ivis.identidade.modelo.Conta;
import br.ivis.identidade.modelo.PerfilAluno;
import br.ivis.identidade.modelo.PerfilInstituicao;
import br.ivis.identidade.modelo.SessaoAutenticacao;
import br.ivis.identidade.repositorio.ContaRepositorio;
import br.ivis.identidade.repositorio.PerfilAlunoRepositorio;
import br.ivis.identidade.repositorio.PerfilInstituicaoRepositorio;
import br.ivis.identidade.repositorio.SessaoRepositorio;

@Service
public class ServicoIdentidade {

    public static final long DURACAO_ACCESS_TOKEN_SEGUNDOS = 15 * 60;
    public static final long DURACAO_REFRESH_TOKEN_DIAS = 7;
    private static final Pattern FORMATO_TOKEN_REFRESH = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final ContaRepositorio contas;
    private final PerfilAlunoRepositorio perfisAlunos;
    private final PerfilInstituicaoRepositorio perfisInstituicoes;
    private final SessaoRepositorio sessoes;
    private final PasswordEncoder codificadorSenha;
    private final JwtEncoder codificadorJwt;
    private final LimiteTentativasLogin limiteTentativas;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String hashSenhaInexistente;

    public ServicoIdentidade(
            ContaRepositorio contas,
            PerfilAlunoRepositorio perfisAlunos,
            PerfilInstituicaoRepositorio perfisInstituicoes,
            SessaoRepositorio sessoes,
            PasswordEncoder codificadorSenha,
            JwtEncoder codificadorJwt,
            LimiteTentativasLogin limiteTentativas) {
        this.contas = contas;
        this.perfisAlunos = perfisAlunos;
        this.perfisInstituicoes = perfisInstituicoes;
        this.sessoes = sessoes;
        this.codificadorSenha = codificadorSenha;
        this.codificadorJwt = codificadorJwt;
        this.limiteTentativas = limiteTentativas;
        this.hashSenhaInexistente = codificadorSenha.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public ContaResposta cadastrarAluno(CadastroAlunoRequisicao requisicao) {
        String email = normalizarEmail(requisicao.email());
        validarEmailDisponivel(email);
        Instant agora = Instant.now();
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(id, "ALUNO", requisicao.email().trim(), email,
                codificadorSenha.encode(requisicao.senha()), agora);
        try {
            contas.saveAndFlush(conta);
            PerfilAluno perfil = perfisAlunos.save(new PerfilAluno(id, requisicao.nome().trim(), agora));
            return new ContaResposta(id, conta.getTipoConta(), conta.getEmail(), perfil.getNomeExibicao());
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
    }

    @Transactional
    public ContaResposta cadastrarInstituicao(CadastroInstituicaoRequisicao requisicao) {
        String email = normalizarEmail(requisicao.email());
        validarEmailDisponivel(email);
        Instant agora = Instant.now();
        UUID id = UUID.randomUUID();
        Conta conta = new Conta(id, "INSTITUICAO", requisicao.email().trim(), email,
                codificadorSenha.encode(requisicao.senha()), agora);
        try {
            contas.saveAndFlush(conta);
            PerfilInstituicao perfil =
                    perfisInstituicoes.save(new PerfilInstituicao(id, requisicao.razaoSocial().trim(),
                            requisicao.nomeAdministrador() == null || requisicao.nomeAdministrador().isBlank()
                                    ? requisicao.razaoSocial().trim() : requisicao.nomeAdministrador().trim(), agora));
            return new ContaResposta(id, conta.getTipoConta(), conta.getEmail(), perfil.getRazaoSocial(),
                    perfil.getNomeAdministrador());
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
    }

    @Transactional
    public EmissaoSessao autenticar(LoginRequisicao requisicao, String enderecoOrigem) {
        String email = normalizarEmail(requisicao.email());
        limiteTentativas.verificar(email, enderecoOrigem);
        Conta conta = contas.findByEmailNormalizado(email).orElse(null);
        String hash = conta == null ? hashSenhaInexistente : conta.getHashSenha();
        boolean senhaCorreta = codificadorSenha.matches(requisicao.senha(), hash);
        if (conta == null || !conta.estaAtiva() || !senhaCorreta) {
            limiteTentativas.registrarFalha(email, enderecoOrigem);
            throw credenciaisInvalidas();
        }
        limiteTentativas.registrarSucesso(email, enderecoOrigem);
        return criarSessao(conta);
    }

    @Transactional(readOnly = true)
    public ContaResposta buscarMinhaConta(UUID contaId) {
        Conta conta = contas.findById(contaId).filter(Conta::estaAtiva)
                .orElseThrow(ServicoIdentidade::sessaoInvalida);
        return resumoConta(conta);
    }

    @Transactional
    public ContaResposta atualizarMinhaConta(UUID contaId, AtualizacaoContaRequisicao requisicao) {
        Conta conta = contas.buscarPorIdParaAtualizacao(contaId).filter(Conta::estaAtiva)
                .orElseThrow(ServicoIdentidade::sessaoInvalida);
        Instant agora = Instant.now();
        if (requisicao.email() != null) {
            String email = requisicao.email().trim();
            if (email.isBlank()) {
                throw dadosContaInvalidos("email", "O e-mail não pode ficar vazio.");
            }
            String normalizado = normalizarEmail(email);
            if (!normalizado.equals(normalizarEmail(conta.getEmail()))
                    && contas.existsByEmailNormalizado(normalizado)) {
                throw emailJaCadastrado();
            }
            conta.atualizarEmail(email, normalizado, agora);
        }

        String nome;
        if ("ALUNO".equals(conta.getTipoConta())) {
            if (requisicao.razaoSocial() != null) {
                throw dadosContaInvalidos("razaoSocial", "Este campo não se aplica a contas de aluno.");
            }
            PerfilAluno perfil = perfisAlunos.findByContaId(contaId)
                    .orElseThrow(ServicoIdentidade::sessaoInvalida);
            if (requisicao.nome() != null) {
                String valor = requisicao.nome().trim();
                if (valor.isBlank()) {
                    throw dadosContaInvalidos("nome", "O nome não pode ficar vazio.");
                }
                perfil.atualizarNome(valor, agora);
            }
            nome = perfil.getNomeExibicao();
        } else {
            if (requisicao.nome() != null) {
                throw dadosContaInvalidos("nome", "Este campo não se aplica a contas de instituição.");
            }
            PerfilInstituicao perfil = perfisInstituicoes.findByContaId(contaId)
                    .orElseThrow(ServicoIdentidade::sessaoInvalida);
            if (requisicao.razaoSocial() != null) {
                String valor = requisicao.razaoSocial().trim();
                if (valor.isBlank()) {
                    throw dadosContaInvalidos("razaoSocial", "A razão social não pode ficar vazia.");
                }
                perfil.atualizarRazaoSocial(valor, agora);
            }
            if (requisicao.nomeAdministrador() != null) {
                String valor = requisicao.nomeAdministrador().trim();
                if (valor.isBlank()) {
                    throw dadosContaInvalidos("nomeAdministrador", "O nome do administrador não pode ficar vazio.");
                }
                perfil.atualizarNomeAdministrador(valor, agora);
            }
            nome = perfil.getRazaoSocial();
        }
        try {
            contas.saveAndFlush(conta);
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
        String nomeAdministrador = "INSTITUICAO".equals(conta.getTipoConta())
                ? perfisInstituicoes.findByContaId(contaId).orElseThrow(ServicoIdentidade::sessaoInvalida)
                        .getNomeAdministrador()
                : null;
        return new ContaResposta(conta.getId(), conta.getTipoConta(), conta.getEmail(), nome, nomeAdministrador);
    }

    @Transactional
    public void trocarSenha(UUID contaId, TrocaSenhaRequisicao requisicao) {
        Conta conta = contas.buscarPorIdParaAtualizacao(contaId).filter(Conta::estaAtiva)
                .orElseThrow(ServicoIdentidade::sessaoInvalida);
        if (!codificadorSenha.matches(requisicao.senhaAtual(), conta.getHashSenha())) {
            throw senhaAtualInvalida();
        }
        conta.atualizarSenha(codificadorSenha.encode(requisicao.novaSenha()), Instant.now());
        sessoes.revogarTodasDaConta(contaId, Instant.now());
    }

    @Transactional
    public EmissaoSessao renovar(String tokenRenovacao) {
        if (!tokenRefreshValido(tokenRenovacao)) {
            throw sessaoInvalida();
        }
        Instant agora = Instant.now();
        SessaoAutenticacao anterior = sessoes.buscarPorHashParaAtualizacao(hashToken(tokenRenovacao))
                .filter(sessao -> sessao.estaAtivaEm(agora))
                .orElseThrow(ServicoIdentidade::sessaoInvalida);
        Conta conta = contas.findById(anterior.getContaId())
                .filter(Conta::estaAtiva)
                .orElseThrow(ServicoIdentidade::sessaoInvalida);

        EmissaoSessao nova = criarSessao(conta);
        anterior.revogar(agora, nova.sessao().getId());
        sessoes.save(anterior);
        return nova;
    }

    @Transactional
    public void encerrar(UUID contaId, String tokenRenovacao) {
        if (!tokenRefreshValido(tokenRenovacao)) {
            return;
        }
        Instant agora = Instant.now();
        sessoes.buscarPorHashParaAtualizacao(hashToken(tokenRenovacao))
                .filter(sessao -> sessao.getContaId().equals(contaId) && sessao.estaAtivaEm(agora))
                .ifPresent(sessao -> {
                    sessao.revogar(agora, null);
                    sessoes.save(sessao);
                });
    }

    public record EmissaoSessao(SessaoResposta resposta, String tokenRenovacao, Instant expiraRefreshEm,
            SessaoAutenticacao sessao) {
    }

    private EmissaoSessao criarSessao(Conta conta) {
        Instant agora = Instant.now();
        String tokenRenovacao = gerarTokenAleatorio();
        Instant expiraRefresh = agora.plus(DURACAO_REFRESH_TOKEN_DIAS, ChronoUnit.DAYS);
        SessaoAutenticacao sessao = sessoes.save(new SessaoAutenticacao(
                UUID.randomUUID(), conta.getId(), hashToken(tokenRenovacao), expiraRefresh, agora));
        ContaResposta resumo = resumoConta(conta);
        Instant expiraAccess = agora.plusSeconds(DURACAO_ACCESS_TOKEN_SEGUNDOS);
        String accessToken = codificadorJwt.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("ivis")
                        .subject(conta.getId().toString())
                        .issuedAt(agora)
                        .expiresAt(expiraAccess)
                        .id(UUID.randomUUID().toString())
                        .claim("tipo_conta", conta.getTipoConta())
                        .build()))
                .getTokenValue();
        return new EmissaoSessao(
                new SessaoResposta(accessToken, "Bearer", DURACAO_ACCESS_TOKEN_SEGUNDOS, resumo),
                tokenRenovacao,
                expiraRefresh,
                sessao);
    }

    private ContaResposta resumoConta(Conta conta) {
        if ("ALUNO".equals(conta.getTipoConta())) {
            String nome = perfisAlunos.findByContaId(conta.getId())
                    .orElseThrow(ServicoIdentidade::sessaoInvalida)
                    .getNomeExibicao();
            return new ContaResposta(conta.getId(), conta.getTipoConta(), conta.getEmail(), nome);
        }
        String nome = perfisInstituicoes.findByContaId(conta.getId())
                .orElseThrow(ServicoIdentidade::sessaoInvalida)
                .getRazaoSocial();
        String nomeAdministrador = perfisInstituicoes.findByContaId(conta.getId())
                .orElseThrow(ServicoIdentidade::sessaoInvalida).getNomeAdministrador();
        return new ContaResposta(conta.getId(), conta.getTipoConta(), conta.getEmail(), nome, nomeAdministrador);
    }

    private void validarEmailDisponivel(String email) {
        if (contas.existsByEmailNormalizado(email)) {
            throw emailJaCadastrado();
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String gerarTokenAleatorio() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não está disponível.", excecao);
        }
    }

    private static boolean tokenRefreshValido(String token) {
        return token != null && FORMATO_TOKEN_REFRESH.matcher(token).matches();
    }

    private static ExcecaoNegocio emailJaCadastrado() {
        return new ExcecaoNegocio(HttpStatus.CONFLICT, "email_ja_cadastrado",
                "Já existe uma conta cadastrada com este e-mail.");
    }

    private static RuntimeException traduzirIntegridade(DataIntegrityViolationException excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "uq_contas_email_normalizado".equals(violacao.getConstraintName())) {
                return emailJaCadastrado();
            }
        }
        return excecao;
    }

    private static ExcecaoNegocio credenciaisInvalidas() {
        return new ExcecaoNegocio(HttpStatus.UNAUTHORIZED, "credenciais_invalidas",
                "E-mail ou senha inválidos.");
    }

    private static ExcecaoNegocio sessaoInvalida() {
        return new ExcecaoNegocio(HttpStatus.UNAUTHORIZED, "sessao_invalida",
                "Sessão inválida ou expirada.");
    }

    private static ExcecaoNegocio senhaAtualInvalida() {
        return new ExcecaoNegocio(HttpStatus.UNAUTHORIZED, "senha_atual_invalida",
                "A senha atual não confere.");
    }

    private static ExcecaoNegocio dadosContaInvalidos(String campo, String mensagem) {
        return new ExcecaoNegocio(HttpStatus.BAD_REQUEST, "dados_invalidos", mensagem,
                List.of(new ErroCampo(campo, mensagem)));
    }
}
