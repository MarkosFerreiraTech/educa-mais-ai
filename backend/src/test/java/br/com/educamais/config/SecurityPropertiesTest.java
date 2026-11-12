package br.com.educamais.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RNF06/RNF09: defaults seguros da configuracao de seguranca (CORS, webhook, OIDC). */
class SecurityPropertiesTest {

    @Test
    @DisplayName("CORS comeca restrito ao frontend local")
    void corsRestritoPorPadrao() {
        SecurityProperties properties = new SecurityProperties();
        assertThat(properties.getCorsAllowedOrigins()).containsExactly("http://localhost:4200");
    }

    @Test
    @DisplayName("webhook exige API key e HMAC, com tolerancia de 5 minutos")
    void defaultsDoWebhook() {
        SecurityProperties.Webhook webhook = new SecurityProperties().getWebhook();
        assertThat(webhook.getApiKey()).isNull();
        assertThat(webhook.getHmacSecret()).isNull();
        assertThat(webhook.getToleranceSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("OIDC desligado por padrao e com grupos de professor/admin configurados")
    void defaultsDoAuthelia() {
        SecurityProperties.Authelia authelia = new SecurityProperties().getAuthelia();
        assertThat(authelia.isEnabled()).isFalse();
        assertThat(authelia.getClientId()).isEqualTo("educa-mais");
        assertThat(authelia.getProfessorGroup()).isEqualTo("professores");
        assertThat(authelia.getAdminGroup()).isEqualTo("administradores");
    }

    @Test
    @DisplayName("propriedades sao sobrescritas por ambiente")
    void settersSobrescrevem() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEncryptionKey("chave-base64");
        properties.setCorsAllowedOrigins(List.of("https://app.educa-mais.com"));
        properties.getAuthelia().setEnabled(true);

        assertThat(properties.getEncryptionKey()).isEqualTo("chave-base64");
        assertThat(properties.getCorsAllowedOrigins()).containsExactly("https://app.educa-mais.com");
        assertThat(properties.getAuthelia().isEnabled()).isTrue();
    }
}
