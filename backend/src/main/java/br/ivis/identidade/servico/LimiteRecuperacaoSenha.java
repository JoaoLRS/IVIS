package br.ivis.identidade.servico;

import br.ivis.comum.erro.ExcecaoNegocio;
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

/** Limites locais ao processo; implantação horizontal exige armazenamento compartilhado. */
@Component
public class LimiteRecuperacaoSenha {

    private final int tentativasMaximas;
    private final Duration janela;
    private final Cache<String, Contador> contadores;

    public LimiteRecuperacaoSenha(
            @Value("${ivis.recuperacao.tentativas-maximas:5}") int tentativasMaximas,
            @Value("${ivis.recuperacao.janela:15m}") Duration janela) {
        if (tentativasMaximas < 1 || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("A configuração de limite de recuperação deve ser positiva.");
        }
        this.tentativasMaximas = tentativasMaximas;
        this.janela = janela;
        this.contadores = Caffeine.newBuilder().maximumSize(50_000).expireAfterAccess(janela).build();
    }

    public void registrarSolicitacao(String emailNormalizado, String enderecoOrigem) {
        boolean emailPermitido = registrarTentativa("email", emailNormalizado);
        boolean ipPermitido = registrarTentativa("ip", enderecoOrigem);
        if (!emailPermitido || !ipPermitido) {
            throw limiteExcedido();
        }
    }

    public void verificarRedefinicao(String enderecoOrigem) {
        if (!registrarTentativa("ip", enderecoOrigem)) {
            throw limiteExcedido();
        }
    }

    private boolean registrarTentativa(String tipo, String valor) {
        String chave = chave(tipo, valor);
        boolean[] permitida = {true};
        contadores.asMap().compute(chave, (ignorada, contador) -> {
            if (contador == null) {
                return new Contador(Instant.now());
            }
            synchronized (contador) {
                reiniciarSeExpirado(contador);
                if (contador.tentativas >= tentativasMaximas) {
                    permitida[0] = false;
                } else {
                    contador.tentativas++;
                }
            }
            return contador;
        });
        return permitida[0];
    }

    private void reiniciarSeExpirado(Contador contador) {
        if (!Instant.now().isBefore(contador.inicioJanela.plus(janela))) {
            contador.inicioJanela = Instant.now();
            contador.tentativas = 0;
        }
    }

    private static String chave(String tipo, String valor) {
        return tipo + ":" + hash(valor);
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 não está disponível.", excecao);
        }
    }

    private static ExcecaoNegocio limiteExcedido() {
        return new ExcecaoNegocio(HttpStatus.TOO_MANY_REQUESTS, "limite_excedido",
                "Muitas solicitações. Tente novamente mais tarde.");
    }

    private static final class Contador {
        private Instant inicioJanela;
        private int tentativas = 1;

        private Contador(Instant inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }
}
