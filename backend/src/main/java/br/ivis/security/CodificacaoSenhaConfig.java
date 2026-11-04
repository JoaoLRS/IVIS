package br.ivis.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class CodificacaoSenhaConfig {

    @Bean
    PasswordEncoder codificadorSenha() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
