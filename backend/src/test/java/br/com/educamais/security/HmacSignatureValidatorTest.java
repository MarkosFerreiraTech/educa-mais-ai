package br.com.educamais.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.educamais.config.SecurityProperties;
import br.com.educamais.domain.exception.BusinessRuleException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF07, RF08: autenticacao do webhook escolar e protecao contra replay. */
class HmacSignatureValidatorTest {

    private HmacSignatureValidator validator;
    private static final String API_KEY = "school-api-key";
    private static final String SECRET = "hmac-secret-de-teste";

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getWebhook().setApiKey(API_KEY);
        properties.getWebhook().setHmacSecret(SECRET);
        properties.getWebhook().setToleranceSeconds(300);
        validator = new HmacSignatureValidator(properties);
    }

    @Test
    @DisplayName("Assinatura valida e aceita")
    void assinaturaValida() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String body = "evt-1|student-1|lesson-1";
        String signature = validator.hmac(timestamp + "." + body);

        assertThatCode(() -> validator.validate(API_KEY, signature, timestamp, body)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("API Key invalida e rejeitada")
    void apiKeyInvalida() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = validator.hmac(timestamp + ".body");

        assertThatThrownBy(() -> validator.validate("outra-key", signature, timestamp, "body"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("API Key");
    }

    @Test
    @DisplayName("Assinatura incorreta e rejeitada com SECURITY_HMAC_MISMATCH")
    void assinaturaIncorreta() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> validator.validate(API_KEY, "deadbeef", timestamp, "body"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SECURITY_HMAC_MISMATCH");
    }

    @Test
    @DisplayName("Timestamp antigo dispara deteccao de replay")
    void replay() {
        String oldTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        String signature = validator.hmac(oldTimestamp + ".body");

        assertThatThrownBy(() -> validator.validate(API_KEY, signature, oldTimestamp, "body"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SECURITY_REPLAY_DETECTED");
    }

    @Test
    @DisplayName("Timestamp ausente e rejeitado")
    void timestampAusente() {
        assertThatThrownBy(() -> validator.validate(API_KEY, "sig", null, "body"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SECURITY_MISSING_TIMESTAMP");
    }
}
