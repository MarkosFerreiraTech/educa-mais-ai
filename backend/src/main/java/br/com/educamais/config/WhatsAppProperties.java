package br.com.educamais.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao do canal WhatsApp (Twilio Sandbox ou Evolution API). */
@ConfigurationProperties(prefix = "educamais.whatsapp")
public class WhatsAppProperties {

    /** Valores aceitos: DEMO, TWILIO, EVOLUTION. */
    private String provider = "DEMO";
    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String evolutionBaseUrl;
    private String evolutionApiKey;
    private String evolutionInstance;
    private String missionDeepLinkBase = "http://localhost:4200/aluno/missoes";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getEvolutionBaseUrl() {
        return evolutionBaseUrl;
    }

    public void setEvolutionBaseUrl(String evolutionBaseUrl) {
        this.evolutionBaseUrl = evolutionBaseUrl;
    }

    public String getEvolutionApiKey() {
        return evolutionApiKey;
    }

    public void setEvolutionApiKey(String evolutionApiKey) {
        this.evolutionApiKey = evolutionApiKey;
    }

    public String getEvolutionInstance() {
        return evolutionInstance;
    }

    public void setEvolutionInstance(String evolutionInstance) {
        this.evolutionInstance = evolutionInstance;
    }

    public String getMissionDeepLinkBase() {
        return missionDeepLinkBase;
    }

    public void setMissionDeepLinkBase(String missionDeepLinkBase) {
        this.missionDeepLinkBase = missionDeepLinkBase;
    }
}
