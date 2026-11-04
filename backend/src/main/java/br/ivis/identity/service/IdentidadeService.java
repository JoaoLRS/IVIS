package br.ivis.identity.service;

import static br.ivis.identity.dto.ContratoIdentidade.CadastroAlunoRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.CadastroInstituicaoRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.AtualizacaoContaRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.ContaResposta;
import static br.ivis.identity.dto.ContratoIdentidade.LoginRequisicao;
import static br.ivis.identity.dto.ContratoIdentidade.SessaoResposta;
import static br.ivis.identity.dto.ContratoIdentidade.TrocaSenhaRequisicao;

import br.ivis.common.error.ExcecaoNegocio;
import br.ivis.common.error.ErroCampo;
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

import br.ivis.identity.model.ContaModel;
import br.ivis.identity.model.PerfilAlunoModel;
import br.ivis.identity.model.PerfilInstituicaoModel;
import br.ivis.identity.model.SessaoAutenticacaoModel;
import br.ivis.identity.repository.ContaRepository;
import br.ivis.identity.repository.PerfilAlunoRepository;
import br.ivis.identity.repository.PerfilInstituicaoRepository;
import br.ivis.identity.repository.SessaoRepository;

@Service
public class IdentidadeService {

    public static final long DURACAO_ACCESS_TOKEN_SEGUNDOS = 15 * 60;
    public static final long DURACAO_REFRESH_TOKEN_DIAS = 7;
    private static final Pattern FORMATO_TOKEN_REFRESH = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final ContaRepository contas;
    private final PerfilAlunoRepository perfisAlunos;
    private final PerfilInstituicaoRepository perfisInstituicoes;
    private final SessaoRepository sessoes;
    private final PasswordEncoder codificadorSenha;
    private final JwtEncoder codificadorJwt;
    private final LimiteTentativasLogin limiteTentativas;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String hashSenhaInexistente;

    public IdentidadeService(
            ContaRepository contas,
            PerfilAlunoRepository perfisAlunos,
            PerfilInstituicaoRepository perfisInstituicoes,
            SessaoRepository sessoes,
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
        String cpf = normalizarDocumento(requisicao.cpf(), "cpf", 11);
        validarCpfDisponivel(cpf, null);
        Instant agora = Instant.now();
        UUID id = UUID.randomUUID();
        ContaModel conta = new ContaModel(id, "ALUNO", requisicao.email().trim(), email,
                codificadorSenha.encode(requisicao.senha()), agora);
        try {
            contas.saveAndFlush(conta);
            perfisAlunos.saveAndFlush(new PerfilAlunoModel(id, requisicao.nome().trim(), cpf,
                    requisicao.telefone().trim(), requisicao.endereco().trim(), null, agora));
            return resumoConta(conta);
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
    }

    @Transactional
    public ContaResposta cadastrarInstituicao(CadastroInstituicaoRequisicao requisicao) {
        String email = normalizarEmail(requisicao.email());
        validarEmailDisponivel(email);
        String cnpj = normalizarDocumento(requisicao.cnpj(), "cnpj", 14);
        validarCnpjDisponivel(cnpj, null);
        Instant agora = Instant.now();
        UUID id = UUID.randomUUID();
        ContaModel conta = new ContaModel(id, "INSTITUICAO", requisicao.email().trim(), email,
                codificadorSenha.encode(requisicao.senha()), agora);
        try {
            contas.saveAndFlush(conta);
            perfisInstituicoes.saveAndFlush(new PerfilInstituicaoModel(id, requisicao.nomeInstituicao().trim(),
                    requisicao.nomeResponsavel().trim(), cnpj, requisicao.telefone().trim(),
                    requisicao.endereco().trim(), agora));
            return resumoConta(conta);
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
    }

    @Transactional
    public EmissaoSessao autenticar(LoginRequisicao requisicao, String enderecoOrigem) {
        String email = normalizarEmail(requisicao.email());
        limiteTentativas.verificar(email, enderecoOrigem);
        ContaModel conta = contas.findByEmailNormalizado(email).orElse(null);
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
        ContaModel conta = contas.findById(contaId).filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(IdentidadeService::sessaoInvalida);
        return resumoConta(conta);
    }

    @Transactional
    public ContaResposta atualizarMinhaConta(UUID contaId, AtualizacaoContaRequisicao requisicao) {
        ContaModel conta = contas.buscarPorIdParaAtualizacao(contaId).filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(IdentidadeService::sessaoInvalida);
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
        if ("ALUNO".equals(conta.getTipoConta())) {
            if (requisicao.nomeInstituicao() != null || requisicao.nomeResponsavel() != null
                    || requisicao.cnpj() != null) {
                throw dadosContaInvalidos("nomeInstituicao", "Os campos de instituição não se aplicam a contas de aluno.");
            }
            PerfilAlunoModel perfil = perfisAlunos.findByContaId(contaId)
                    .orElseThrow(IdentidadeService::sessaoInvalida);
            String nome = perfil.getNome();
            if (requisicao.nome() != null) {
                nome = validarTextoObrigatorio(requisicao.nome(), "nome", "O nome não pode ficar vazio.");
            }
            String cpf = perfil.getCpf();
            if (requisicao.cpf() != null) {
                cpf = normalizarDocumento(requisicao.cpf(), "cpf", 11);
                validarCpfDisponivel(cpf, perfil.getCpf());
            }
            String telefone = requisicao.telefone() == null ? perfil.getTelefone()
                    : validarTextoObrigatorio(requisicao.telefone(), "telefone", "O telefone não pode ficar vazio.");
            String endereco = requisicao.endereco() == null ? perfil.getEndereco()
                    : validarTextoObrigatorio(requisicao.endereco(), "endereco", "O endereço não pode ficar vazio.");
            perfil.atualizarDados(nome, cpf, telefone, endereco, agora);
            try {
                perfisAlunos.saveAndFlush(perfil);
            } catch (DataIntegrityViolationException excecao) {
                throw traduzirIntegridade(excecao);
            }
        } else {
            if (requisicao.nome() != null || requisicao.cpf() != null) {
                throw dadosContaInvalidos("nome", "Os campos de aluno não se aplicam a contas de instituição.");
            }
            PerfilInstituicaoModel perfil = perfisInstituicoes.findByContaId(contaId)
                    .orElseThrow(IdentidadeService::sessaoInvalida);
            String nomeInstituicao = requisicao.nomeInstituicao() == null ? perfil.getNomeInstituicao()
                    : validarTextoObrigatorio(requisicao.nomeInstituicao(), "nomeInstituicao",
                            "O nome da instituição não pode ficar vazio.");
            String nomeResponsavel = requisicao.nomeResponsavel() == null ? perfil.getNomeResponsavel()
                    : validarTextoObrigatorio(requisicao.nomeResponsavel(), "nomeResponsavel",
                            "O nome do responsável não pode ficar vazio.");
            String cnpj = perfil.getCnpj();
            if (requisicao.cnpj() != null) {
                cnpj = normalizarDocumento(requisicao.cnpj(), "cnpj", 14);
                validarCnpjDisponivel(cnpj, perfil.getCnpj());
            }
            String telefone = requisicao.telefone() == null ? perfil.getTelefone()
                    : validarTextoObrigatorio(requisicao.telefone(), "telefone", "O telefone não pode ficar vazio.");
            String endereco = requisicao.endereco() == null ? perfil.getEndereco()
                    : validarTextoObrigatorio(requisicao.endereco(), "endereco", "O endereço não pode ficar vazio.");
            perfil.atualizarDados(nomeInstituicao, nomeResponsavel, cnpj, telefone, endereco, agora);
            try {
                perfisInstituicoes.saveAndFlush(perfil);
            } catch (DataIntegrityViolationException excecao) {
                throw traduzirIntegridade(excecao);
            }
        }
        try {
            contas.saveAndFlush(conta);
        } catch (DataIntegrityViolationException excecao) {
            throw traduzirIntegridade(excecao);
        }
        return resumoConta(conta);
    }

    @Transactional
    public void trocarSenha(UUID contaId, TrocaSenhaRequisicao requisicao) {
        ContaModel conta = contas.buscarPorIdParaAtualizacao(contaId).filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(IdentidadeService::sessaoInvalida);
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
        SessaoAutenticacaoModel anterior = sessoes.buscarPorHashParaAtualizacao(hashToken(tokenRenovacao))
                .filter(sessao -> sessao.estaAtivaEm(agora))
                .orElseThrow(IdentidadeService::sessaoInvalida);
        ContaModel conta = contas.findById(anterior.getContaId())
                .filter(contaEncontrada -> contaEncontrada.estaAtiva())
                .orElseThrow(IdentidadeService::sessaoInvalida);

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
            SessaoAutenticacaoModel sessao) {
    }

    private EmissaoSessao criarSessao(ContaModel conta) {
        Instant agora = Instant.now();
        String tokenRenovacao = gerarTokenAleatorio();
        Instant expiraRefresh = agora.plus(DURACAO_REFRESH_TOKEN_DIAS, ChronoUnit.DAYS);
        SessaoAutenticacaoModel sessao = sessoes.save(new SessaoAutenticacaoModel(
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

    private ContaResposta resumoConta(ContaModel conta) {
        if ("ALUNO".equals(conta.getTipoConta())) {
            PerfilAlunoModel perfil = perfisAlunos.findByContaId(conta.getId())
                    .orElseThrow(IdentidadeService::sessaoInvalida);
            return new ContaResposta(conta.getId(), conta.getTipoConta(), conta.getEmail(),
                    perfil.getNome(), null, perfil.getCpf(), perfil.getTelefone(), perfil.getEndereco(),
                    perfil.getObjetoInstituicao(), null);
        }
        PerfilInstituicaoModel perfil = perfisInstituicoes.findByContaId(conta.getId())
                .orElseThrow(IdentidadeService::sessaoInvalida);
        return new ContaResposta(conta.getId(), conta.getTipoConta(), conta.getEmail(),
                perfil.getNomeInstituicao(), perfil.getNomeResponsavel(), null, perfil.getTelefone(),
                perfil.getEndereco(), null, perfil.getCnpj());
    }

    private void validarEmailDisponivel(String email) {
        if (contas.existsByEmailNormalizado(email)) {
            throw emailJaCadastrado();
        }
    }

    private void validarCpfDisponivel(String cpf, String cpfAtual) {
        if (!cpf.equals(cpfAtual) && perfisAlunos.existsByCpf(cpf)) {
            throw documentoJaCadastrado("cpf", "Já existe um aluno cadastrado com este CPF.");
        }
    }

    private void validarCnpjDisponivel(String cnpj, String cnpjAtual) {
        if (!cnpj.equals(cnpjAtual) && perfisInstituicoes.existsByCnpj(cnpj)) {
            throw documentoJaCadastrado("cnpj", "Já existe uma instituição cadastrada com este CNPJ.");
        }
    }

    private static String normalizarDocumento(String valor, String campo, int tamanhoEsperado) {
        String documento = valor == null ? "" : valor.replaceAll("[^0-9]", "");
        if (documento.length() != tamanhoEsperado) {
            throw dadosContaInvalidos(campo,
                    "Informe um documento válido com " + tamanhoEsperado + " dígitos.");
        }
        return documento;
    }

    private static String validarTextoObrigatorio(String valor, String campo, String mensagem) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isBlank()) {
            throw dadosContaInvalidos(campo, mensagem);
        }
        return texto;
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
            if (causa instanceof ConstraintViolationException violacao
                    && "uq_perfis_aluno_cpf".equals(violacao.getConstraintName())) {
                return documentoJaCadastrado("cpf", "Já existe um aluno cadastrado com este CPF.");
            }
            if (causa instanceof ConstraintViolationException violacao
                    && "uq_perfis_instituicao_cnpj".equals(violacao.getConstraintName())) {
                return documentoJaCadastrado("cnpj", "Já existe uma instituição cadastrada com este CNPJ.");
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

    private static ExcecaoNegocio documentoJaCadastrado(String campo, String mensagem) {
        return new ExcecaoNegocio(HttpStatus.CONFLICT, "documento_ja_cadastrado", mensagem,
                List.of(new ErroCampo(campo, mensagem)));
    }
}
