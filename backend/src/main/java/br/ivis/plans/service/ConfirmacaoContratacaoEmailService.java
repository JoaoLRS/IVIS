package br.ivis.plans.service;

import br.ivis.plans.dto.ContratoCompraSimulada.EmailSimulado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ConfirmacaoContratacaoEmailService {
    private static final Logger log = LoggerFactory.getLogger(ConfirmacaoContratacaoEmailService.class);

    private final JavaMailSender remetente;
    private final String emailRemetente;
    private final boolean habilitado;

    public ConfirmacaoContratacaoEmailService(
            JavaMailSender remetente,
            @Value("${ivis.email.confirmacao-habilitada:false}") boolean habilitado,
            @Value("${ivis.email.remetente:no-reply@ivis.local}") String emailRemetente) {
        this.remetente = remetente;
        this.habilitado = habilitado;
        this.emailRemetente = emailRemetente;
    }

    public void enviar(EmailSimulado confirmacao) {
        if (!habilitado || confirmacao == null) return;

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailRemetente);
        mensagem.setTo(confirmacao.destinatario());
        mensagem.setSubject(confirmacao.assunto());
        mensagem.setText(confirmacao.corpo());
        try {
            remetente.send(mensagem);
        } catch (MailException falha) {
            log.warn("Não foi possível entregar a confirmação de contratação ao SMTP. tipo={}",
                    falha.getClass().getSimpleName());
        }
    }
}
