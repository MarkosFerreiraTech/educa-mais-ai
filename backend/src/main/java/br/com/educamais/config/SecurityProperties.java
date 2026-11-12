package br.com.educamais.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de seguranca carregada de variaveis de ambiente (12-factor).
 */
@ConfigurationProperties(prefix = "educamais.security")
public class SecurityProperties {

    private String encryptionKey;
    private List<String> corsAllowedOrigins = List.of("http://localhost:4200");
    private Webhook webhook = new Webhook();
    private Authelia authelia = new Authelia();

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    public Authelia getAuthelia() {
        return authelia;
    }

    public void setAuthelia(Authelia authelia) {
        this.authelia = authelia;
    }

    /** Credenciais do sistema escolar (RF07). */
    public static class Webhook {
        private String apiKey;
        private String hmacSecret;
        private long toleranceSeconds = 300;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public long getToleranceSeconds() {
            return toleranceSeconds;
        }

        public void setToleranceSeconds(long toleranceSeconds) {
            this.toleranceSeconds = toleranceSeconds;
        }
    }

    /** Integracao OIDC/SSO (RF09 / RNF09). */
    public static class Authelia {
        private boolean enabled;
        private String issuerUri = "https://auth.educa-mais.local";
        private String clientId = "educa-mais";
        private String clientSecret;
        private String professorGroup = "professores";
        private String adminGroup = "administradores";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getProfessorGroup() {
            return professorGroup;
        }

        public void setProfessorGroup(String professorGroup) {
            this.professorGroup = professorGroup;
        }

        public String getAdminGroup() {
            return adminGroup;
        }

        public void setAdminGroup(String adminGroup) {
            this.adminGroup = adminGroup;
        }
    }
}
