package br.com.educamais.infrastructure.notification;

import br.com.educamais.config.WhatsAppProperties;
import br.com.educamais.domain.port.NotificationPort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Adapter de WhatsApp com dois provedores (RF24) e modo demonstracao (RF25).
 *
 * <pre>
 * provider=TWILIO     -> Twilio Sandbox (Messages API)
 * provider=EVOLUTION  -> Evolution API (self-hosted)
 * provider=DEMO       -> registra SKIPPED/NO_CREDENTIALS sem interromper o fluxo
 * </pre>
 *
 * Retries com backoff curto (RNF03) e qualquer falha vira {@code DeliveryResult.failed}.
 */
@Component("whatsAppNotificationAdapter")
public class WhatsAppNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationAdapter.class);
    private static final int MAX_ATTEMPTS = 3;

    private final WhatsAppProperties properties;
    private final RestClient restClient;

    public WhatsAppNotificationAdapter(WhatsAppProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String provider = properties.getProvider() == null ? "DEMO" : properties.getProvider().toUpperCase();
        String message = WhatsAppMessageTemplate.render(request, properties.getMissionDeepLinkBase());
        return switch (provider) {
            case "TWILIO" -> sendWithRetry(() -> sendTwilio(request.destination(), message), "TWILIO");
            case "EVOLUTION" -> sendWithRetry(() -> sendEvolution(request.destination(), message), "EVOLUTION");
            default -> {
                log.info("WHATSAPP_DEMO_MODE destination=MASKED messageChars={}", message.length());
                yield DeliveryResult.skipped("NO_CREDENTIALS");
            }
        };
    }

    private DeliveryResult sendWithRetry(java.util.function.Supplier<String> sender, String provider) {
        String lastReason = "UNKNOWN";
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return DeliveryResult.sent(sender.get());
            } catch (Exception exception) {
                lastReason = exception.getClass().getSimpleName();
                log.warn("WHATSAPP_SEND_FAILED provider={} attempt={} reason={}", provider, attempt, lastReason);
                if (attempt < MAX_ATTEMPTS) {
                    sleepQuietly(150L * attempt);
                }
            }
        }
        return DeliveryResult.failed(lastReason);
    }

    /** Twilio Messages API: application/x-www-form-urlencoded + Basic Auth. */
    private String sendTwilio(String destination, String message) {
        if (isBlank(properties.getAccountSid()) || isBlank(properties.getAuthToken())
                || isBlank(properties.getFromNumber())) {
            throw new IllegalStateException("TWILIO_CREDENTIALS_MISSING");
        }
        String credentials = Base64.getEncoder().encodeToString(
                (properties.getAccountSid() + ":" + properties.getAuthToken()).getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", "whatsapp:" + destination);
        form.add("From", "whatsapp:" + properties.getFromNumber());
        form.add("Body", message);
        String response = restClient.post()
                .uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json",
                        properties.getAccountSid())
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        return extractJsonValue(response, "sid");
    }

    /** Evolution API: /message/sendText/{instance}. */
    private String sendEvolution(String destination, String message) {
        if (isBlank(properties.getEvolutionBaseUrl()) || isBlank(properties.getEvolutionApiKey())) {
            throw new IllegalStateException("EVOLUTION_CREDENTIALS_MISSING");
        }
        String payload = """
                {"number":"%s","text":"%s"}""".formatted(destination, message.replace("\"", "'").replace("\n", "\\n"));
        String response = restClient.post()
                .uri(properties.getEvolutionBaseUrl() + "/message/sendText/" + properties.getEvolutionInstance())
                .header("apikey", properties.getEvolutionApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
        return extractJsonValue(response, "key");
    }

    private static String extractJsonValue(String json, String field) {
        if (json == null) {
            return null;
        }
        int index = json.indexOf("\"" + field + "\"");
        if (index < 0) {
            return null;
        }
        int colon = json.indexOf(':', index);
        int start = json.indexOf('"', colon + 1);
        if (colon < 0 || start < 0) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        return end > start ? json.substring(start + 1, end) : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
