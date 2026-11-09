package br.ivis.identity.service;

import br.ivis.identity.dto.ContratoIdentidade.ContaResposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class BoasVindasEmailService {

    private static final Logger log = LoggerFactory.getLogger(BoasVindasEmailService.class);

    private final JavaMailSender remetente;
    private final String emailRemetente;
    private final boolean habilitado;

    public BoasVindasEmailService(
            JavaMailSender remetente,
            @Value("${ivis.email.remetente:no-reply@ivis.local}") String emailRemetente,
            @Value("${ivis.email.boas-vindas-habilitado:false}") boolean habilitado) {
        this.remetente = remetente;
        this.emailRemetente = emailRemetente;
        this.habilitado = habilitado;
    }

    public void enviar(ContaResposta conta) {
        if (!habilitado) return;

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailRemetente);
        mensagem.setTo(conta.email());
        mensagem.setSubject("Bem-vindo ao IVIS");
        mensagem.setText("Olá, " + conta.nome() + "!\n\n"
                + "Sua conta IVIS foi criada com sucesso. A partir de agora, você pode escolher um plano "
                + "individual ou, se sua instituição tiver um plano acadêmico ativo, acessar pelo vínculo institucional.\n\n"
                + "Esta mensagem foi capturada pelo ambiente de demonstração do IVIS.");
        try {
            remetente.send(mensagem);
        } catch (MailException falha) {
            log.warn("Não foi possível entregar o e-mail de boas-vindas ao SMTP. tipo={}",
                    falha.getClass().getSimpleName());
        }
    }
}
