package br.ivis.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoCodificacaoSenha {

    @Bean
    PasswordEncoder codificadorSenha() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
