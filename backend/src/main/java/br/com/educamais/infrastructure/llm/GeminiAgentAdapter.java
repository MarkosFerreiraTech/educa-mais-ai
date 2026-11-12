package br.com.educamais.infrastructure.llm;

import br.com.educamais.config.AgentProperties;
import br.com.educamais.domain.enums.PlanSource;
import br.com.educamais.domain.model.MissionPlan;
import br.com.educamais.domain.port.LlmPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter do Gemini Flash (RF12, RF21 / ADR-002).
 *
 * <p>Sem credencial (ou com {@code educamais.agent.llm-enabled=false}) opera em modo demonstracao,
 * devolvendo {@code Optional.empty()} para acionar o fallback deterministico. Erros de rede,
 * timeout ou parsing tambem retornam {@code empty} -- o fluxo do aluno nunca quebra.</p>
 */
@Component
public class GeminiAgentAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiAgentAdapter.class);

    private final AgentProperties properties;
    private final GeminiRequestFactory requestFactory;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiAgentAdapter(AgentProperties properties, GeminiRequestFactory requestFactory,
                              @org.springframework.beans.factory.annotation.Qualifier(
                                      br.com.educamais.config.JsonMapperConfig.ADAPTER_OBJECT_MAPPER)
                              ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.requestFactory = requestFactory;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getGeminiBaseUrl()).build();
    }

    @Override
    public boolean isAvailable() {
        return properties.isLlmEnabled() && properties.getGeminiApiKey() != null
                && !properties.getGeminiApiKey().isBlank();
    }

    @Override
    public Optional<MissionPlan> planMission(PlanPrompt prompt) {
        if (!isAvailable()) {
            log.info("LLM_DEMO_MODE action=planMission");
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(call(requestFactory.planRequest(prompt.text(), prompt.maxSteps())));
            List<MissionPlan.PlannedStep> steps = new ArrayList<>();
            for (JsonNode node : root.path("steps")) {
                String objective = node.path("objective").asText("");
                if (objective.isBlank()) {
                    continue;
                }
                steps.add(new MissionPlan.PlannedStep(objective, node.path("explanation").asText(""),
                        node.path("example").asText("")));
            }
            return steps.isEmpty() ? Optional.empty() : Optional.of(new MissionPlan(steps, PlanSource.LLM));
        } catch (Exception exception) {
            log.warn("LLM_PLAN_FAILED reason={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ExplanationSuggestion> reExplain(ExplanationPrompt prompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(call(requestFactory.explanationRequest(prompt.text())));
            String explanation = root.path("explanation").asText("");
            return explanation.isBlank() ? Optional.empty()
                    : Optional.of(new ExplanationSuggestion(explanation, root.path("example").asText("")));
        } catch (Exception exception) {
            log.warn("LLM_EXPLANATION_FAILED reason={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<LlmQuestion>> generateQuestions(QuestionPrompt prompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(call(requestFactory.questionRequest(prompt.text())));
            List<String> options = new ArrayList<>();
            root.path("options").forEach(option -> options.add(option.asText()));
            String questionText = root.path("prompt").asText("");
            if (questionText.isBlank() || options.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(List.of(new LlmQuestion(questionText, root.path("type").asText("MULTIPLE_CHOICE"),
                    options, root.path("correctAnswer").asText(), root.path("explanation").asText(""),
                    prompt.difficulty())));
        } catch (Exception exception) {
            log.warn("LLM_QUESTION_FAILED reason={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> recommendTeacherAction(RecommendationPrompt prompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(call(requestFactory.recommendationRequest(prompt.text())));
            String recommendation = root.path("recommendation").asText("");
            return recommendation.isBlank() ? Optional.empty() : Optional.of(recommendation);
        } catch (Exception exception) {
            log.warn("LLM_RECOMMENDATION_FAILED reason={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Extrai o JSON de {@code candidates[0].content.parts[0].text}. */
    private String call(ObjectNode payload) {
        String body = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", properties.getGeminiModel(),
                        properties.getGeminiApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.toString())
                .retrieve()
                .body(String.class);
        try {
            JsonNode text = objectMapper.readTree(body)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new IllegalStateException("Resposta sem conteudo");
            }
            return text.asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Resposta invalida do provedor LLM", exception);
        }
    }
}
