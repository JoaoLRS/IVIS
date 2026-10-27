package br.ivis.identidade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import br.ivis.comum.erro.ExcecaoNegocio;

/** Limitação local ao processo; implantação horizontal exige um armazenamento compartilhado. */
@Component
public class LimiteTentativasLogin {

    private final int tentativasMaximas;
    private final Duration janela;
    private final Cache<String, Contador> contadores;

    public LimiteTentativasLogin(
            @Value("${ivis.autenticacao.tentativas-login-maximas:10}") int tentativasMaximas,
            @Value("${ivis.autenticacao.janela-login:15m}") Duration janela) {
        if (tentativasMaximas < 1 || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("A configuração de limite de login deve ser positiva.");
        }
        this.tentativasMaximas = tentativasMaximas;
        this.janela = janela;
        this.contadores = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterAccess(janela)
                .build();
    }

    public void verificar(String emailNormalizado, String enderecoOrigem) {
        Instant agora = Instant.now();
        if (bloqueado(chave("conta", emailNormalizado), agora)
                || bloqueado(chave("origem", enderecoOrigem), agora)) {
            throw limiteExcedido();
        }
    }

    public void registrarFalha(String emailNormalizado, String enderecoOrigem) {
        Instant agora = Instant.now();
        incrementar(chave("conta", emailNormalizado), agora);
        incrementar(chave("origem", enderecoOrigem), agora);
    }

    public void registrarSucesso(String emailNormalizado, String enderecoOrigem) {
        contadores.invalidate(chave("conta", emailNormalizado));
        contadores.invalidate(chave("origem", enderecoOrigem));
    }

    private boolean bloqueado(String chave, Instant agora) {
        Contador contador = contadores.getIfPresent(chave);
        return contador != null && contador.bloqueado(agora, janela, tentativasMaximas);
    }

    private void incrementar(String chave, Instant agora) {
        contadores.asMap().compute(chave, (ignorada, contador) -> {
            if (contador == null) {
                return new Contador(agora);
            }
            contador.incrementar(agora, janela);
            return contador;
        });
    }

    private static String chave(String tipo, String valor) {
        return tipo + ":" + hash(valor);
    }

    private static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não está disponível.", excecao);
        }
    }

    private static ExcecaoNegocio limiteExcedido() {
        return new ExcecaoNegocio(HttpStatus.TOO_MANY_REQUESTS, "limite_excedido",
                "Muitas tentativas de login. Tente novamente mais tarde.");
    }

    private static final class Contador {

        private Instant inicioJanela;
        private int tentativas;

        private Contador(Instant inicioJanela) {
            this.inicioJanela = inicioJanela;
            this.tentativas = 1;
        }

        private synchronized boolean bloqueado(Instant agora, Duration janela, int maximo) {
            reiniciarSeExpirado(agora, janela);
            return tentativas >= maximo;
        }

        private synchronized void incrementar(Instant agora, Duration janela) {
            reiniciarSeExpirado(agora, janela);
            tentativas++;
        }

        private void reiniciarSeExpirado(Instant agora, Duration janela) {
            if (!agora.isBefore(inicioJanela.plus(janela))) {
                inicioJanela = agora;
                tentativas = 0;
            }
        }
    }
}
