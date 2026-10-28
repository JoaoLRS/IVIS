package br.ivis.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoChavesJwt {

    @Bean
    SecretKey chaveAssinaturaJwt(@Value("${ivis.jwt.chave-assinatura}") String valorBase64) {
        byte[] chave;
        try {
            chave = Base64.getDecoder().decode(valorBase64);
        } catch (IllegalArgumentException excecao) {
            throw new IllegalStateException("IVIS_JWT_CHAVE_ASSINATURA deve conter uma chave Base64 válida.", excecao);
        }
        if (chave.length < 32) {
            throw new IllegalStateException("IVIS_JWT_CHAVE_ASSINATURA deve conter pelo menos 32 bytes.");
        }
        return new SecretKeySpec(chave, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey chave) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer("ivis"));
        return decodificador;
    }
}
