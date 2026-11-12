package br.com.educamais.domain.port;

import br.com.educamais.domain.enums.PedagogicalStrategyType;
import br.com.educamais.domain.model.MissionPlan;
import java.util.List;
import java.util.Optional;

/**
 * Porta do LLM (Adapter: GeminiAgentAdapter).
 *
 * <p>O dominio fornece o prompt ja montado (PromptFactory) e recebe estruturas tipadas.
 * Toda falha do provedor deve resultar em {@link Optional#empty()} -- nunca em excecao --
 * para que o fallback deterministico assuma (RF21).</p>
 */
public interface LlmPort {

    boolean isAvailable();

    Optional<MissionPlan> planMission(PlanPrompt prompt);

    Optional<ExplanationSuggestion> reExplain(ExplanationPrompt prompt);

    Optional<List<LlmQuestion>> generateQuestions(QuestionPrompt prompt);

    Optional<String> recommendTeacherAction(RecommendationPrompt prompt);

    record PlanPrompt(String text, int maxSteps) {
    }

    record ExplanationPrompt(String text, PedagogicalStrategyType strategy) {
    }

    record QuestionPrompt(String text, int difficulty) {
    }

    record RecommendationPrompt(String text) {
    }

    record ExplanationSuggestion(String explanation, String example) {
    }

    record LlmQuestion(String prompt, String type, List<String> options, String correctAnswer,
                       String explanation, int difficulty) {
    }
}
