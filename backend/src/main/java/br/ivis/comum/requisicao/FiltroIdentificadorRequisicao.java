package br.ivis.comum.requisicao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Atribui um identificador a cada requisição: aproveita o {@code X-Id-Requisicao} enviado
 * pelo cliente se tiver formato seguro, senão gera um. O valor vai para o MDC (logs),
 * para o cabeçalho da resposta e para o corpo de erro.
 *
 * <p>Registra apenas método, caminho, status e duração. Não registra consulta, cabeçalhos
 * nem corpo, para não vazar senhas, tokens ou dados pessoais.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroIdentificadorRequisicao extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Id-Requisicao";
    public static final String CHAVE_MDC = "idRequisicao";

    private static final Pattern FORMATO_SEGURO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final Logger registro = LoggerFactory.getLogger(FiltroIdentificadorRequisicao.class);

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String recebido = requisicao.getHeader(CABECALHO);
        String id = recebido != null && FORMATO_SEGURO.matcher(recebido).matches()
                ? recebido
                : UUID.randomUUID().toString();

        MDC.put(CHAVE_MDC, id);
        resposta.setHeader(CABECALHO, id);
        long inicio = System.nanoTime();
        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
            registro.info("{} {} -> {} ({} ms)", requisicao.getMethod(), requisicao.getRequestURI(),
                    resposta.getStatus(), duracaoMs);
            MDC.remove(CHAVE_MDC);
        }
    }
}
