package br.com.educamais.security;

import br.com.educamais.config.SecurityProperties;
import br.com.educamais.domain.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Validacao do webhook escolar (RF07, RF08 / RNF08):
 * API Key dedicada + assinatura HMAC-SHA256 com comparacao em tempo constante
 * e janela anti-replay de 5 minutos.
 */
@Component
public class HmacSignatureValidator {

    public static final String HEADER_API_KEY = "X-Api-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    private final SecurityProperties.Webhook config;

    public HmacSignatureValidator(SecurityProperties securityProperties) {
        this.config = securityProperties.getWebhook();
    }

    public void validate(String apiKey, String signature, String timestamp, String rawBody) {
        validateApiKey(apiKey);
        validateTimestamp(timestamp);
        validateSignature(signature, timestamp, rawBody);
    }

    public void validateApiKey(String apiKey) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessRuleException("SECURITY_WEBHOOK_DISABLED", "Webhook sem chave configurada");
        }
        if (!constantTimeEquals(config.getApiKey(), apiKey)) {
            throw new BusinessRuleException("SECURITY_INVALID_API_KEY", "API Key invalida");
        }
    }

    public void validateTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new BusinessRuleException("SECURITY_MISSING_TIMESTAMP", "Header X-Timestamp obrigatorio");
        }
        long eventEpoch;
        try {
            eventEpoch = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("SECURITY_INVALID_TIMESTAMP", "Timestamp invalido");
        }
        long nowEpoch = Instant.now().getEpochSecond();
        if (Math.abs(nowEpoch - eventEpoch) > config.getToleranceSeconds()) {
            throw new BusinessRuleException("SECURITY_REPLAY_DETECTED", "Evento expirado (possivel replay)");
        }
    }

    public void validateSignature(String signature, String timestamp, String rawBody) {
        if (config.getHmacSecret() == null || config.getHmacSecret().isBlank()) {
            throw new BusinessRuleException("SECURITY_WEBHOOK_DISABLED", "Webhook sem segredo HMAC configurado");
        }
        String expected = hmac(timestamp + "." + rawBody);
        if (!constantTimeEquals(expected, signature)) {
            throw new BusinessRuleException("SECURITY_HMAC_MISMATCH", "Assinatura HMAC invalida");
        }
    }

    public String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new BusinessRuleException("SECURITY_HMAC_ERROR", "Falha ao calcular assinatura");
        }
    }

    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
