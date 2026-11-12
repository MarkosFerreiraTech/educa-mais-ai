package br.com.educamais.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao do agente e das integracoes externas. */
@ConfigurationProperties(prefix = "educamais.agent")
public class AgentProperties {

    private boolean llmEnabled;
    private String geminiApiKey;
    private String geminiModel = "gemini-2.5-flash";
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int timeoutSeconds = 20;
    private int maxSteps = 5;
    private int maxAttemptsPerStep = 3;

    public boolean isLlmEnabled() {
        return llmEnabled;
    }

    public void setLlmEnabled(boolean llmEnabled) {
        this.llmEnabled = llmEnabled;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public String getGeminiBaseUrl() {
        return geminiBaseUrl;
    }

    public void setGeminiBaseUrl(String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getMaxAttemptsPerStep() {
        return maxAttemptsPerStep;
    }

    public void setMaxAttemptsPerStep(int maxAttemptsPerStep) {
        this.maxAttemptsPerStep = maxAttemptsPerStep;
    }
}
